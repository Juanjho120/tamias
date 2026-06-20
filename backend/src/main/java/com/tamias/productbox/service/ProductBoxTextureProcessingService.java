package com.tamias.productbox.service;

import com.tamias.common.exception.BadRequestException;
import com.tamias.productbox.dto.ProductBoxTexturePointRequest;
import com.tamias.productbox.dto.ProductBoxTextureProcessRequest;
import com.tamias.productbox.entity.ProductBoxModel;
import com.tamias.productbox.enums.ProductBoxFaceName;
import com.tamias.productbox.enums.ProductBoxTextureEnhancementMode;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class ProductBoxTextureProcessingService {

    private static final int MAX_TARGET_SIDE_PX = 1600;
    private static final String PROCESSED_CONTENT_TYPE = "image/png";
    private static final double MIN_CONTOUR_AREA_RATIO = 0.0125;
    private static final double MIN_FALLBACK_CONFIDENCE = 0.1000;
    private static volatile boolean openCvLoaded = false;

    public ProcessedProductBoxTexture process(
            Resource originalResource,
            ProductBoxModel productBoxModel,
            ProductBoxFaceName faceName,
            ProductBoxTextureProcessRequest request,
            Integer coordinateWidthPx,
            Integer coordinateHeightPx
    ) {
        ensureOpenCvLoaded();

        Mat source = null;
        Mat perspectiveTransform = null;
        Mat warped = null;
        Mat enhanced = null;
        MatOfPoint2f sourcePoints = null;
        MatOfPoint2f targetPoints = null;
        MatOfByte encoded = null;

        try {
            byte[] originalBytes = originalResource.getInputStream().readAllBytes();
            source = Imgcodecs.imdecode(new MatOfByte(originalBytes), Imgcodecs.IMREAD_COLOR);

            if (source.empty()) {
                throw new BadRequestException("Original product box texture image could not be decoded");
            }

            ProductBoxTextureProcessRequest sourceCoordinateRequest = toSourceCoordinateRequest(
                    request,
                    source.width(),
                    source.height(),
                    coordinateWidthPx,
                    coordinateHeightPx
            );

            validatePoints(sourceCoordinateRequest, source.width(), source.height());

            TargetTextureSize targetSize = calculateTargetSize(productBoxModel, faceName);

            sourcePoints = new MatOfPoint2f(
                    toPoint(sourceCoordinateRequest.topLeft()),
                    toPoint(sourceCoordinateRequest.topRight()),
                    toPoint(sourceCoordinateRequest.bottomRight()),
                    toPoint(sourceCoordinateRequest.bottomLeft())
            );

            targetPoints = new MatOfPoint2f(
                    new Point(0, 0),
                    new Point(targetSize.width() - 1.0, 0),
                    new Point(targetSize.width() - 1.0, targetSize.height() - 1.0),
                    new Point(0, targetSize.height() - 1.0)
            );

            perspectiveTransform = Imgproc.getPerspectiveTransform(sourcePoints, targetPoints);
            warped = new Mat();

            Imgproc.warpPerspective(
                    source,
                    warped,
                    perspectiveTransform,
                    new Size(targetSize.width(), targetSize.height()),
                    Imgproc.INTER_CUBIC
            );

            ProductBoxTextureEnhancementMode enhancementMode = normalizeEnhancementMode(request.enhancementMode());
            enhanced = applyEnhancement(warped, enhancementMode);

            encoded = new MatOfByte();
            boolean encodedSuccessfully = Imgcodecs.imencode(".png", enhanced, encoded);

            if (!encodedSuccessfully) {
                throw new BadRequestException("Processed product box texture image could not be encoded");
            }

            String filename = "processed-" + faceName.getValue() + ".png";

            return new ProcessedProductBoxTexture(
                    encoded.toArray(),
                    filename,
                    PROCESSED_CONTENT_TYPE,
                    targetSize.width(),
                    targetSize.height(),
                    targetSize.aspectRatio(),
                    sourceCoordinateRequest,
                    enhancementMode
            );
        } catch (IOException ex) {
            throw new BadRequestException("Original product box texture image could not be read");
        } finally {
            release(source);
            release(perspectiveTransform);
            release(warped);
            release(enhanced);
            release(sourcePoints);
            release(targetPoints);
            release(encoded);
        }
    }

    public DetectedProductBoxContour detectContour(
            Resource originalResource,
            Integer coordinateWidthPx,
            Integer coordinateHeightPx
    ) {
        ensureOpenCvLoaded();

        Mat source = null;
        Mat gray = null;

        try {
            byte[] originalBytes = originalResource.getInputStream().readAllBytes();
            source = Imgcodecs.imdecode(new MatOfByte(originalBytes), Imgcodecs.IMREAD_COLOR);

            if (source.empty()) {
                throw new BadRequestException("Original product box texture image could not be decoded");
            }

            gray = new Mat();
            Imgproc.cvtColor(source, gray, Imgproc.COLOR_BGR2GRAY);

            List<ContourCandidate> candidates = new ArrayList<>();
            ContourCandidate dominantEdgeCandidate = findCandidateWithDominantEdges(gray, source.width(), source.height());
            if (dominantEdgeCandidate != null) {
                candidates.add(dominantEdgeCandidate);
            }
            candidates.addAll(findCandidatesWithCanny(gray, source.width(), source.height()));
            candidates.addAll(findCandidatesWithAdaptiveThreshold(gray, source.width(), source.height()));

            ContourCandidate bestCandidate = candidates.stream()
                    .max(Comparator.comparing(ContourCandidate::confidence))
                    .orElse(null);

            if (bestCandidate == null) {
                return defaultAdjustableContour(source.width(), source.height(), coordinateWidthPx, coordinateHeightPx);
            }

            ProductBoxTextureProcessRequest sourcePoints = toRequest(bestCandidate.orderedPoints());
            ProductBoxTextureProcessRequest coordinatePoints = fromSourceCoordinateRequest(
                    sourcePoints,
                    source.width(),
                    source.height(),
                    coordinateWidthPx,
                    coordinateHeightPx
            );

            return new DetectedProductBoxContour(
                    true,
                    bestCandidate.confidence(),
                    coordinatePoints,
                    "Contour detected successfully"
            );
        } catch (IOException ex) {
            throw new BadRequestException("Original product box texture image could not be read");
        } finally {
            release(source);
            release(gray);
        }
    }


    private ContourCandidate findCandidateWithDominantEdges(Mat gray, int width, int height) {
        Mat blurred = new Mat();
        Mat edges = new Mat();
        Mat lines = new Mat();

        try {
            Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);
            Imgproc.Canny(blurred, edges, 45, 140);
            Imgproc.HoughLinesP(
                    edges,
                    lines,
                    1,
                    Math.PI / 180,
                    Math.max(90, (int) Math.round(Math.min(width, height) * 0.09)),
                    Math.max(180, Math.min(width, height) * 0.16),
                    Math.max(18, Math.min(width, height) * 0.015)
            );

            if (lines.empty()) {
                return null;
            }

            List<LineSegment> horizontalLines = new ArrayList<>();
            List<LineSegment> verticalLines = new ArrayList<>();

            for (int i = 0; i < lines.rows(); i++) {
                double[] row = lines.get(i, 0);
                if (row == null || row.length < 4) {
                    continue;
                }

                LineSegment segment = new LineSegment(row[0], row[1], row[2], row[3]);
                double absoluteAngle = Math.abs(segment.angleDegrees());
                double normalizedVerticalAngle = Math.abs(90.0 - absoluteAngle);

                if (absoluteAngle <= 8.0 && segment.length() >= width * 0.18) {
                    horizontalLines.add(segment);
                } else if (normalizedVerticalAngle <= 10.0 && segment.length() >= height * 0.12) {
                    verticalLines.add(segment);
                }
            }

            LineSegment topEdge = horizontalLines.stream()
                    .filter(line -> line.midY() <= height * 0.30)
                    .filter(line -> line.maxX() >= width * 0.45)
                    .max(Comparator.comparingDouble(line -> line.length() - (line.midY() * 0.10)))
                    .orElse(null);

            LineSegment bottomEdge = horizontalLines.stream()
                    .filter(line -> line.midY() >= height * 0.78)
                    .max(Comparator.comparingDouble(line -> (line.midY() * 0.55) + line.length()))
                    .orElse(null);

            if (topEdge == null || bottomEdge == null) {
                return null;
            }

            LineSegment leftEdge = verticalLines.stream()
                    .filter(line -> line.midX() >= width * 0.24 && line.midX() <= width * 0.58)
                    .min(Comparator.comparingDouble(line -> Math.abs(line.midX() - topEdge.minX()) - (line.length() * 0.015)))
                    .orElse(null);

            LineSegment rightEdge = verticalLines.stream()
                    .filter(line -> line.midX() >= width * 0.58 && line.midX() <= width * 0.96)
                    .min(Comparator.comparingDouble(line -> Math.abs(line.midX() - topEdge.maxX()) - (line.length() * 0.015)))
                    .orElse(null);

            double topY = clamp(topEdge.midY(), 0, height - 1.0);
            double bottomY = clamp(bottomEdge.midY(), topY + 10.0, height - 1.0);
            double leftX = leftEdge != null
                    ? leftEdge.midX()
                    : Math.max(width * 0.25, Math.min(topEdge.minX(), bottomEdge.minX()));
            double rightX = rightEdge != null
                    ? Math.max(rightEdge.midX(), topEdge.maxX())
                    : Math.min(width * 0.95, Math.max(topEdge.maxX(), bottomEdge.maxX()));

            leftX = clamp(leftX, 0, width - 2.0);
            rightX = clamp(rightX, leftX + 10.0, width - 1.0);

            Point[] points = orderPoints(new Point[] {
                    new Point(leftX, topY),
                    new Point(rightX, topY),
                    new Point(rightX, bottomY),
                    new Point(leftX, bottomY)
            });

            double areaRatio = Math.abs(Imgproc.contourArea(new MatOfPoint(points))) / Math.max(1.0, width * (double) height);
            if (areaRatio < 0.18) {
                return null;
            }

            double confidence = Math.min(0.96, 0.55 + areaRatio + (leftEdge != null ? 0.08 : 0.0) + (rightEdge != null ? 0.08 : 0.0));
            return new ContourCandidate(
                    points,
                    BigDecimal.valueOf(confidence).setScale(4, RoundingMode.HALF_UP)
            );
        } finally {
            release(blurred);
            release(edges);
            release(lines);
        }
    }


    private List<ContourCandidate> findCandidatesWithGrabCut(Mat source, int width, int height) {
        Mat resized = null;
        Mat mask = null;
        Mat backgroundModel = null;
        Mat foregroundModel = null;
        Mat sureForeground = new Mat();
        Mat probableForeground = new Mat();
        Mat foregroundMask = new Mat();
        Mat cleanedMask = new Mat();
        Mat kernel = null;
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        List<ContourCandidate> candidates = new ArrayList<>();

        try {
            double scale = Math.min(1.0, 900.0 / Math.max(width, height));
            if (scale < 1.0) {
                resized = new Mat();
                Imgproc.resize(source, resized, new Size(Math.round(width * scale), Math.round(height * scale)), 0, 0, Imgproc.INTER_AREA);
            } else {
                resized = source.clone();
            }

            int resizedWidth = resized.width();
            int resizedHeight = resized.height();
            int insetX = Math.max(2, (int) Math.round(resizedWidth * 0.08));
            int insetY = Math.max(2, (int) Math.round(resizedHeight * 0.025));
            Rect initialRectangle = new Rect(
                    insetX,
                    insetY,
                    Math.max(2, resizedWidth - (insetX * 2)),
                    Math.max(2, resizedHeight - (insetY * 2))
            );

            mask = new Mat(resizedHeight, resizedWidth, CvType.CV_8UC1, new Scalar(Imgproc.GC_BGD));
            backgroundModel = new Mat(1, 65, CvType.CV_64FC1);
            foregroundModel = new Mat(1, 65, CvType.CV_64FC1);

            Imgproc.grabCut(
                    resized,
                    mask,
                    initialRectangle,
                    backgroundModel,
                    foregroundModel,
                    5,
                    Imgproc.GC_INIT_WITH_RECT
            );

            Core.inRange(mask, new Scalar(Imgproc.GC_FGD), new Scalar(Imgproc.GC_FGD), sureForeground);
            Core.inRange(mask, new Scalar(Imgproc.GC_PR_FGD), new Scalar(Imgproc.GC_PR_FGD), probableForeground);
            Core.bitwise_or(sureForeground, probableForeground, foregroundMask);

            kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9, 9));
            Imgproc.morphologyEx(foregroundMask, cleanedMask, Imgproc.MORPH_CLOSE, kernel, new Point(-1, -1), 2);
            Imgproc.morphologyEx(cleanedMask, cleanedMask, Imgproc.MORPH_OPEN, kernel, new Point(-1, -1), 1);

            Imgproc.findContours(cleanedMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            double imageArea = Math.max(1.0, width * (double) height);
            double inverseScale = scale > 0 ? 1.0 / scale : 1.0;

            for (MatOfPoint contour : contours) {
                MatOfPoint scaledContour = scaleContour(contour, inverseScale);
                try {
                    ContourCandidate candidate = toCandidate(scaledContour, imageArea, 1.45);
                    if (candidate != null) {
                        candidates.add(candidate);
                    }
                } finally {
                    scaledContour.release();
                }
            }

            return candidates;
        } catch (RuntimeException ex) {
            return candidates;
        } finally {
            contours.forEach(Mat::release);
            release(resized);
            release(mask);
            release(backgroundModel);
            release(foregroundModel);
            release(sureForeground);
            release(probableForeground);
            release(foregroundMask);
            release(cleanedMask);
            release(kernel);
            release(hierarchy);
        }
    }

    private MatOfPoint scaleContour(MatOfPoint contour, double scale) {
        Point[] points = contour.toArray();
        Point[] scaledPoints = new Point[points.length];
        for (int i = 0; i < points.length; i++) {
            scaledPoints[i] = new Point(points[i].x * scale, points[i].y * scale);
        }
        return new MatOfPoint(scaledPoints);
    }

    private List<ContourCandidate> findCandidatesWithCanny(Mat gray, int width, int height) {
        List<ContourCandidate> candidates = new ArrayList<>();
        int[][] thresholds = {
                {20, 80},
                {35, 120},
                {50, 160},
                {70, 210}
        };

        for (int[] threshold : thresholds) {
            Mat blurred = new Mat();
            Mat edges = new Mat();
            Mat closed = new Mat();
            Mat kernel = null;

            try {
                Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);
                Imgproc.Canny(blurred, edges, threshold[0], threshold[1]);
                kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
                Imgproc.morphologyEx(edges, closed, Imgproc.MORPH_CLOSE, kernel);
                Imgproc.dilate(closed, closed, kernel, new Point(-1, -1), 1);
                candidates.addAll(findCandidatesFromBinary(closed, width, height, 1.0));
            } finally {
                release(blurred);
                release(edges);
                release(closed);
                release(kernel);
            }
        }

        return candidates;
    }

    private List<ContourCandidate> findCandidatesWithAdaptiveThreshold(Mat gray, int width, int height) {
        Mat blurred = new Mat();
        Mat threshold = new Mat();
        Mat inverted = new Mat();
        Mat closed = new Mat();
        Mat kernel = null;

        try {
            Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);
            Imgproc.adaptiveThreshold(
                    blurred,
                    threshold,
                    255,
                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY,
                    31,
                    7
            );
            Core.bitwise_not(threshold, inverted);
            kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
            Imgproc.morphologyEx(inverted, closed, Imgproc.MORPH_CLOSE, kernel);
            return findCandidatesFromBinary(closed, width, height, 0.85);
        } finally {
            release(blurred);
            release(threshold);
            release(inverted);
            release(closed);
            release(kernel);
        }
    }

    private List<ContourCandidate> findCandidatesFromBinary(Mat binary, int width, int height, double scoreMultiplier) {
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        List<ContourCandidate> candidates = new ArrayList<>();

        try {
            Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
            double imageArea = Math.max(1.0, width * (double) height);

            for (MatOfPoint contour : contours) {
                ContourCandidate candidate = toCandidate(contour, imageArea, scoreMultiplier);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            }

            return candidates;
        } finally {
            contours.forEach(Mat::release);
            release(hierarchy);
        }
    }

    private ContourCandidate toCandidate(MatOfPoint contour, double imageArea, double scoreMultiplier) {
        MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
        MatOfPoint2f approx = new MatOfPoint2f();

        try {
            double contourArea = Math.abs(Imgproc.contourArea(contour));
            if (contourArea / imageArea < MIN_CONTOUR_AREA_RATIO) {
                return null;
            }

            double perimeter = Imgproc.arcLength(contour2f, true);
            if (perimeter <= 0) {
                return null;
            }

            double[] approximationFactors = {0.015, 0.02, 0.03, 0.04, 0.055, 0.07};

            for (double factor : approximationFactors) {
                Imgproc.approxPolyDP(contour2f, approx, factor * perimeter, true);

                if (approx.total() == 4) {
                    Point[] points = approx.toArray();
                    double approxArea = Math.abs(Imgproc.contourArea(new MatOfPoint(points)));
                    double confidence = calculateContourConfidence(approxArea, imageArea, scoreMultiplier);

                    if (confidence >= MIN_CONTOUR_AREA_RATIO) {
                        return new ContourCandidate(
                                orderPoints(points),
                                BigDecimal.valueOf(Math.min(1.0, confidence)).setScale(4, RoundingMode.HALF_UP)
                        );
                    }
                }
            }

            return toMinAreaRectCandidate(contour2f, contourArea, imageArea, scoreMultiplier);
        } finally {
            release(contour2f);
            release(approx);
        }
    }

    private ContourCandidate toMinAreaRectCandidate(
            MatOfPoint2f contour2f,
            double contourArea,
            double imageArea,
            double scoreMultiplier
    ) {
        RotatedRect rectangle = Imgproc.minAreaRect(contour2f);

        if (rectangle.size.width < 20 || rectangle.size.height < 20) {
            return null;
        }

        double rectangleArea = Math.max(1.0, rectangle.size.width * rectangle.size.height);
        double rectangularity = Math.max(0.0, Math.min(1.0, contourArea / rectangleArea));
        double confidence = (rectangleArea / imageArea) * rectangularity * scoreMultiplier * 0.80;

        if (confidence < MIN_CONTOUR_AREA_RATIO) {
            return null;
        }

        Point[] rectanglePoints = new Point[4];
        rectangle.points(rectanglePoints);

        return new ContourCandidate(
                orderPoints(rectanglePoints),
                BigDecimal.valueOf(Math.min(1.0, confidence)).setScale(4, RoundingMode.HALF_UP)
        );
    }

    private double calculateContourConfidence(double contourArea, double imageArea, double scoreMultiplier) {
        return Math.max(0.0, contourArea / imageArea) * scoreMultiplier;
    }

    private DetectedProductBoxContour defaultAdjustableContour(
            int sourceWidth,
            int sourceHeight,
            Integer coordinateWidthPx,
            Integer coordinateHeightPx
    ) {
        boolean portrait = sourceHeight >= sourceWidth;
        double leftX = portrait ? sourceWidth * 0.29 : sourceWidth * 0.14;
        double rightX = portrait ? sourceWidth * 0.84 : sourceWidth * 0.86;
        double topY = portrait ? sourceHeight * 0.06 : sourceHeight * 0.12;
        double bottomY = portrait ? sourceHeight * 0.98 : sourceHeight * 0.88;
        Point[] defaultPoints = {
                new Point(clamp(leftX, 0, sourceWidth - 2.0), clamp(topY, 0, sourceHeight - 2.0)),
                new Point(clamp(rightX, leftX + 10.0, sourceWidth - 1.0), clamp(topY, 0, sourceHeight - 2.0)),
                new Point(clamp(rightX, leftX + 10.0, sourceWidth - 1.0), clamp(bottomY, topY + 10.0, sourceHeight - 1.0)),
                new Point(clamp(leftX, 0, sourceWidth - 2.0), clamp(bottomY, topY + 10.0, sourceHeight - 1.0))
        };

        ProductBoxTextureProcessRequest sourcePoints = toRequest(defaultPoints);
        ProductBoxTextureProcessRequest coordinatePoints = fromSourceCoordinateRequest(
                sourcePoints,
                sourceWidth,
                sourceHeight,
                coordinateWidthPx,
                coordinateHeightPx
        );

        return new DetectedProductBoxContour(
                true,
                BigDecimal.valueOf(MIN_FALLBACK_CONFIDENCE).setScale(4, RoundingMode.HALF_UP),
                coordinatePoints,
                "No reliable rectangular contour was detected. Default adjustable corners were initialized."
        );
    }

    private ProductBoxTextureProcessRequest toSourceCoordinateRequest(
            ProductBoxTextureProcessRequest request,
            int sourceWidth,
            int sourceHeight,
            Integer coordinateWidthPx,
            Integer coordinateHeightPx
    ) {
        int coordinateWidth = normalizeCoordinateDimension(coordinateWidthPx, sourceWidth);
        int coordinateHeight = normalizeCoordinateDimension(coordinateHeightPx, sourceHeight);

        return new ProductBoxTextureProcessRequest(
                toSourceCoordinatePoint(request.topLeft(), sourceWidth, sourceHeight, coordinateWidth, coordinateHeight),
                toSourceCoordinatePoint(request.topRight(), sourceWidth, sourceHeight, coordinateWidth, coordinateHeight),
                toSourceCoordinatePoint(request.bottomRight(), sourceWidth, sourceHeight, coordinateWidth, coordinateHeight),
                toSourceCoordinatePoint(request.bottomLeft(), sourceWidth, sourceHeight, coordinateWidth, coordinateHeight),
                normalizeEnhancementMode(request.enhancementMode())
        );
    }

    private ProductBoxTextureProcessRequest fromSourceCoordinateRequest(
            ProductBoxTextureProcessRequest request,
            int sourceWidth,
            int sourceHeight,
            Integer coordinateWidthPx,
            Integer coordinateHeightPx
    ) {
        int coordinateWidth = normalizeCoordinateDimension(coordinateWidthPx, sourceWidth);
        int coordinateHeight = normalizeCoordinateDimension(coordinateHeightPx, sourceHeight);

        return new ProductBoxTextureProcessRequest(
                fromSourceCoordinatePoint(request.topLeft(), sourceWidth, sourceHeight, coordinateWidth, coordinateHeight),
                fromSourceCoordinatePoint(request.topRight(), sourceWidth, sourceHeight, coordinateWidth, coordinateHeight),
                fromSourceCoordinatePoint(request.bottomRight(), sourceWidth, sourceHeight, coordinateWidth, coordinateHeight),
                fromSourceCoordinatePoint(request.bottomLeft(), sourceWidth, sourceHeight, coordinateWidth, coordinateHeight),
                normalizeEnhancementMode(request.enhancementMode())
        );
    }

    private int normalizeCoordinateDimension(Integer coordinateDimension, int sourceDimension) {
        if (coordinateDimension == null || coordinateDimension <= 0) {
            return sourceDimension;
        }

        return coordinateDimension;
    }

    private ProductBoxTexturePointRequest toSourceCoordinatePoint(
            ProductBoxTexturePointRequest point,
            int sourceWidth,
            int sourceHeight,
            int coordinateWidth,
            int coordinateHeight
    ) {
        BigDecimal x = scaleAndClamp(point.x(), coordinateWidth, sourceWidth);
        BigDecimal y = scaleAndClamp(point.y(), coordinateHeight, sourceHeight);
        return new ProductBoxTexturePointRequest(x, y);
    }

    private ProductBoxTexturePointRequest fromSourceCoordinatePoint(
            ProductBoxTexturePointRequest point,
            int sourceWidth,
            int sourceHeight,
            int coordinateWidth,
            int coordinateHeight
    ) {
        BigDecimal x = scaleAndClamp(point.x(), sourceWidth, coordinateWidth);
        BigDecimal y = scaleAndClamp(point.y(), sourceHeight, coordinateHeight);
        return new ProductBoxTexturePointRequest(x, y);
    }

    private BigDecimal scaleAndClamp(BigDecimal value, int coordinateDimension, int sourceDimension) {
        double raw = value != null ? value.doubleValue() : 0.0;
        double scaled = raw;

        if (coordinateDimension > 1 && sourceDimension > 1 && coordinateDimension != sourceDimension) {
            scaled = raw * ((sourceDimension - 1.0) / (coordinateDimension - 1.0));
        }

        double max = Math.max(0, sourceDimension - 1.0);
        double clamped = Math.max(0.0, Math.min(max, Math.round(scaled)));
        return BigDecimal.valueOf(clamped);
    }

    private ProductBoxTextureProcessRequest toRequest(Point[] points) {
        return new ProductBoxTextureProcessRequest(
                toPointRequest(points[0]),
                toPointRequest(points[1]),
                toPointRequest(points[2]),
                toPointRequest(points[3])
        );
    }

    private ProductBoxTexturePointRequest toPointRequest(Point point) {
        return new ProductBoxTexturePointRequest(
                BigDecimal.valueOf(Math.round(point.x)),
                BigDecimal.valueOf(Math.round(point.y))
        );
    }

    private Point[] orderPoints(Point[] points) {
        if (points.length != 4) {
            throw new BadRequestException("Product box contour detection returned invalid points");
        }

        Point topLeft = null;
        Point topRight = null;
        Point bottomRight = null;
        Point bottomLeft = null;

        double minSum = Double.MAX_VALUE;
        double maxSum = -Double.MAX_VALUE;
        double minDiff = Double.MAX_VALUE;
        double maxDiff = -Double.MAX_VALUE;

        for (Point point : points) {
            double sum = point.x + point.y;
            double diff = point.y - point.x;

            if (sum < minSum) {
                minSum = sum;
                topLeft = point;
            }

            if (sum > maxSum) {
                maxSum = sum;
                bottomRight = point;
            }

            if (diff < minDiff) {
                minDiff = diff;
                topRight = point;
            }

            if (diff > maxDiff) {
                maxDiff = diff;
                bottomLeft = point;
            }
        }

        return new Point[] { topLeft, topRight, bottomRight, bottomLeft };
    }

    private ProductBoxTextureEnhancementMode normalizeEnhancementMode(ProductBoxTextureEnhancementMode enhancementMode) {
        return enhancementMode != null ? enhancementMode : ProductBoxTextureEnhancementMode.BASIC;
    }

    private Mat applyEnhancement(Mat source, ProductBoxTextureEnhancementMode enhancementMode) {
        return switch (normalizeEnhancementMode(enhancementMode)) {
            case NONE -> source.clone();
            case BASIC -> applyReadablePackageEnhancement(source, 0.5, 99.5, 0.84, 1.35, 1.08, 10.0, 0.12);
            case STRONG -> applyReadablePackageEnhancement(source, 0.8, 99.2, 0.76, 1.45, 1.12, 18.0, 0.18);
        };
    }

    private Mat applyReadablePackageEnhancement(
            Mat source,
            double lowPercentile,
            double highPercentile,
            double gammaValue,
            double saturationScale,
            double contrastAlpha,
            double brightnessBeta,
            double sharpenAmount
    ) {
        Mat colorBalanced = applySimplestColorBalance(source, lowPercentile, highPercentile);
        Mat gammaCorrected = applyGamma(colorBalanced, gammaValue);
        Mat saturated = applySaturation(gammaCorrected, saturationScale);
        Mat adjusted = new Mat();
        Mat sharpened = null;

        try {
            saturated.convertTo(adjusted, -1, contrastAlpha, brightnessBeta);
            sharpened = applyUnsharpMask(adjusted, sharpenAmount);
            return sharpened.clone();
        } finally {
            release(colorBalanced);
            release(gammaCorrected);
            release(saturated);
            release(adjusted);
            release(sharpened);
        }
    }

    private Mat applySimplestColorBalance(Mat source, double lowPercentile, double highPercentile) {
        List<Mat> channels = new ArrayList<>();
        List<Mat> balancedChannels = new ArrayList<>();
        Mat result = new Mat();

        try {
            Core.split(source, channels);
            for (Mat channel : channels) {
                double low = percentile(channel, lowPercentile);
                double high = percentile(channel, highPercentile);
                Mat balanced = new Mat();

                if (high <= low) {
                    channel.copyTo(balanced);
                } else {
                    double alpha = 255.0 / (high - low);
                    double beta = -low * alpha;
                    channel.convertTo(balanced, CvType.CV_8U, alpha, beta);
                }

                balancedChannels.add(balanced);
            }

            Core.merge(balancedChannels, result);
            return result;
        } finally {
            channels.forEach(Mat::release);
            balancedChannels.forEach(Mat::release);
        }
    }

    private double percentile(Mat channel, double percentile) {
        Mat hist = new Mat();
        try {
            Imgproc.calcHist(
                    List.of(channel),
                    new MatOfInt(0),
                    new Mat(),
                    hist,
                    new MatOfInt(256),
                    new MatOfFloat(0, 256)
            );

            double total = channel.rows() * (double) channel.cols();
            double target = Math.max(0.0, Math.min(100.0, percentile)) / 100.0 * total;
            double cumulative = 0.0;

            for (int i = 0; i < 256; i++) {
                cumulative += hist.get(i, 0)[0];
                if (cumulative >= target) {
                    return i;
                }
            }

            return 255.0;
        } finally {
            release(hist);
        }
    }

    private Mat applySaturation(Mat source, double saturationScale) {
        Mat hsv = new Mat();
        Mat result = new Mat();
        List<Mat> channels = new ArrayList<>();

        try {
            Imgproc.cvtColor(source, hsv, Imgproc.COLOR_BGR2HSV);
            Core.split(hsv, channels);
            channels.get(1).convertTo(channels.get(1), CvType.CV_8U, saturationScale, 0.0);
            Core.merge(channels, hsv);
            Imgproc.cvtColor(hsv, result, Imgproc.COLOR_HSV2BGR);
            return result;
        } finally {
            release(hsv);
            channels.forEach(Mat::release);
        }
    }

    private Mat applyUnsharpMask(Mat source, double amount) {
        if (amount <= 0) {
            return source.clone();
        }

        Mat blurred = new Mat();
        Mat result = new Mat();

        try {
            Imgproc.GaussianBlur(source, blurred, new Size(0, 0), 1.0);
            Core.addWeighted(source, 1.0 + amount, blurred, -amount, 0.0, result);
            return result;
        } finally {
            release(blurred);
        }
    }

    private Mat applyGamma(Mat source, double gammaValue) {
        Mat lookupTable = new Mat(1, 256, CvType.CV_8UC1);
        Mat result = new Mat();
        byte[] data = new byte[256];

        try {
            double safeGamma = gammaValue > 0 ? gammaValue : 1.0;
            for (int i = 0; i < data.length; i++) {
                int adjusted = (int) Math.round(255.0 * Math.pow(i / 255.0, safeGamma));
                data[i] = (byte) Math.max(0, Math.min(255, adjusted));
            }
            lookupTable.put(0, 0, data);
            Core.LUT(source, lookupTable, result);
            return result;
        } finally {
            release(lookupTable);
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static synchronized void ensureOpenCvLoaded() {
        if (!openCvLoaded) {
            nu.pattern.OpenCV.loadLocally();
            openCvLoaded = true;
        }
    }

    private void validatePoints(ProductBoxTextureProcessRequest request, int imageWidth, int imageHeight) {
        validatePoint("topLeft", request.topLeft(), imageWidth, imageHeight);
        validatePoint("topRight", request.topRight(), imageWidth, imageHeight);
        validatePoint("bottomRight", request.bottomRight(), imageWidth, imageHeight);
        validatePoint("bottomLeft", request.bottomLeft(), imageWidth, imageHeight);

        double polygonArea = polygonArea(request);
        if (polygonArea < 10.0) {
            throw new BadRequestException("Product box texture points do not define a valid face area");
        }
    }

    private void validatePoint(String pointName, ProductBoxTexturePointRequest point, int imageWidth, int imageHeight) {
        double x = point.x().doubleValue();
        double y = point.y().doubleValue();

        if (x < 0 || y < 0 || x >= imageWidth || y >= imageHeight) {
            throw new BadRequestException("Product box texture point " + pointName + " is outside the original image bounds");
        }
    }

    private double polygonArea(ProductBoxTextureProcessRequest request) {
        Point[] points = {
                toPoint(request.topLeft()),
                toPoint(request.topRight()),
                toPoint(request.bottomRight()),
                toPoint(request.bottomLeft())
        };

        double area = 0.0;
        for (int i = 0; i < points.length; i++) {
            Point current = points[i];
            Point next = points[(i + 1) % points.length];
            area += current.x * next.y - next.x * current.y;
        }

        return Math.abs(area) / 2.0;
    }

    private Point toPoint(ProductBoxTexturePointRequest point) {
        return new Point(point.x().doubleValue(), point.y().doubleValue());
    }

    private TargetTextureSize calculateTargetSize(ProductBoxModel productBoxModel, ProductBoxFaceName faceName) {
        BigDecimal targetWidthCm = switch (faceName) {
            case FRONT, BACK, TOP, BOTTOM -> productBoxModel.getWidth();
            case LEFT, RIGHT -> productBoxModel.getDepth();
        };

        BigDecimal targetHeightCm = switch (faceName) {
            case FRONT, BACK, LEFT, RIGHT -> productBoxModel.getHeight();
            case TOP, BOTTOM -> productBoxModel.getDepth();
        };

        if (targetWidthCm == null || targetHeightCm == null || BigDecimal.ZERO.compareTo(targetHeightCm) == 0) {
            throw new BadRequestException("Invalid product box dimensions");
        }

        BigDecimal aspectRatio = targetWidthCm.divide(targetHeightCm, 6, RoundingMode.HALF_UP);
        double ratio = aspectRatio.doubleValue();

        if (ratio <= 0) {
            throw new BadRequestException("Invalid product box face aspect ratio");
        }

        int targetWidth;
        int targetHeight;

        if (ratio >= 1.0) {
            targetWidth = MAX_TARGET_SIDE_PX;
            targetHeight = Math.max(1, (int) Math.round(MAX_TARGET_SIDE_PX / ratio));
        } else {
            targetHeight = MAX_TARGET_SIDE_PX;
            targetWidth = Math.max(1, (int) Math.round(MAX_TARGET_SIDE_PX * ratio));
        }

        return new TargetTextureSize(targetWidth, targetHeight, aspectRatio);
    }

    private void release(Mat mat) {
        if (mat != null) {
            mat.release();
        }
    }

    public record ProcessedProductBoxTexture(
            byte[] bytes,
            String filename,
            String contentType,
            Integer widthPx,
            Integer heightPx,
            BigDecimal targetAspectRatio,
            ProductBoxTextureProcessRequest appliedPoints,
            ProductBoxTextureEnhancementMode enhancementMode
    ) {
    }

    public record DetectedProductBoxContour(
            boolean detected,
            BigDecimal confidence,
            ProductBoxTextureProcessRequest points,
            String message
    ) {
    }

    private record TargetTextureSize(int width, int height, BigDecimal aspectRatio) {
    }

    private record LineSegment(double x1, double y1, double x2, double y2) {
        double length() {
            return Math.hypot(x2 - x1, y2 - y1);
        }

        double angleDegrees() {
            return Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
        }

        double midX() {
            return (x1 + x2) / 2.0;
        }

        double midY() {
            return (y1 + y2) / 2.0;
        }

        double minX() {
            return Math.min(x1, x2);
        }

        double maxX() {
            return Math.max(x1, x2);
        }
    }

    private record ContourCandidate(Point[] orderedPoints, BigDecimal confidence) {
    }
}
