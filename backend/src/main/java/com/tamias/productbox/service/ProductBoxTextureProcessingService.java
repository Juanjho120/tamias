package com.tamias.productbox.service;

import com.tamias.common.exception.BadRequestException;
import com.tamias.productbox.dto.ProductBoxTexturePointRequest;
import com.tamias.productbox.dto.ProductBoxTextureProcessRequest;
import com.tamias.productbox.entity.ProductBoxModel;
import com.tamias.productbox.enums.ProductBoxFaceName;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class ProductBoxTextureProcessingService {

    private static final int MAX_TARGET_SIDE_PX = 1600;
    private static final String PROCESSED_CONTENT_TYPE = "image/png";
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

            encoded = new MatOfByte();
            boolean encodedSuccessfully = Imgcodecs.imencode(".png", warped, encoded);
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
                sourceCoordinateRequest
            );
        } catch (IOException ex) {
            throw new BadRequestException("Original product box texture image could not be read");
        } finally {
            release(source);
            release(perspectiveTransform);
            release(warped);
            release(sourcePoints);
            release(targetPoints);
            release(encoded);
        }
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
            toSourceCoordinatePoint(request.bottomLeft(), sourceWidth, sourceHeight, coordinateWidth, coordinateHeight)
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
        ProductBoxTextureProcessRequest appliedPoints
    ) { }

    private record TargetTextureSize(
        int width,
        int height,
        BigDecimal aspectRatio
    ) { }
}
