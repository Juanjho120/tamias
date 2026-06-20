package com.tamias.productbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.document.storage.FileStorageService;
import com.tamias.image.service.ImageValidationService;
import com.tamias.productbox.dto.ProductBoxModelFaceResponse;
import com.tamias.productbox.dto.ProductBoxTextureContourDetectionResponse;
import com.tamias.productbox.dto.ProductBoxTextureProcessRequest;
import com.tamias.productbox.entity.ProductBoxModel;
import com.tamias.productbox.entity.ProductBoxModelFace;
import com.tamias.productbox.enums.ProductBoxFaceName;
import com.tamias.productbox.enums.ProductBoxTextureEnhancementMode;
import com.tamias.productbox.enums.ProductBoxTextureStatus;
import com.tamias.productbox.mapper.ProductBoxModelMapper;
import com.tamias.productbox.repository.ProductBoxModelFaceRepository;
import com.tamias.productbox.repository.ProductBoxModelRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.entity.User;
import com.tamias.user.repository.UserRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductBoxModelFaceService {

    private static final String PROCESSED_TEXTURE_CONTENT_TYPE = "image/png";

    private final ProductBoxModelRepository productBoxModelRepository;
    private final ProductBoxModelFaceRepository productBoxModelFaceRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final ImageValidationService imageValidationService;
    private final ProductBoxModelMapper productBoxModelMapper;
    private final ProductBoxTextureProcessingService productBoxTextureProcessingService;
    private final ObjectMapper objectMapper;

    public ProductBoxModelFaceService(
        ProductBoxModelRepository productBoxModelRepository,
        ProductBoxModelFaceRepository productBoxModelFaceRepository,
        UserRepository userRepository,
        CurrentUserService currentUserService,
        FileStorageService fileStorageService,
        ImageValidationService imageValidationService,
        ProductBoxModelMapper productBoxModelMapper,
        ProductBoxTextureProcessingService productBoxTextureProcessingService,
        ObjectMapper objectMapper
    ) {
        this.productBoxModelRepository = productBoxModelRepository;
        this.productBoxModelFaceRepository = productBoxModelFaceRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
        this.imageValidationService = imageValidationService;
        this.productBoxModelMapper = productBoxModelMapper;
        this.productBoxTextureProcessingService = productBoxTextureProcessingService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public List<ProductBoxModelFaceResponse> findAll(UUID productBoxModelId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        validateProductBoxModel(productBoxModelId, organizationId);

        return productBoxModelFaceRepository
            .findByProductBoxModel_IdAndOrganization_IdOrderByFaceNameAsc(productBoxModelId, organizationId)
            .stream()
            .map(productBoxModelMapper::toFaceResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public ProductBoxModelFaceResponse findByFaceName(UUID productBoxModelId, String faceNameValue) {
        return productBoxModelMapper.toFaceResponse(findFace(productBoxModelId, faceNameValue));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public ProductBoxModelFaceResponse uploadOriginal(
        UUID productBoxModelId,
        String faceNameValue,
        MultipartFile file
    ) {
        imageValidationService.validateImage(file);
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        ProductBoxModel productBoxModel = validateProductBoxModel(productBoxModelId, organizationId);
        ProductBoxFaceName faceName = ProductBoxFaceName.fromValue(faceNameValue);
        User currentUser = findCurrentUser();

        ProductBoxModelFace face = productBoxModelFaceRepository
            .findByProductBoxModel_IdAndOrganization_IdAndFaceName(productBoxModelId, organizationId, faceName)
            .orElse(null);

        ImageDimensions dimensions = readDimensions(file);
        var storedFile = fileStorageService.store(
            file,
            productBoxModel.getOrganization().getId()
                + "/catalogs/product_box_models/"
                + productBoxModel.getId()
                + "/faces/"
                + faceName.getValue()
                + "/original"
        );

        if (face == null) {
            face = new ProductBoxModelFace();
            face.setOrganization(productBoxModel.getOrganization());
            face.setProductBoxModel(productBoxModel);
            face.setFaceName(faceName);
            face.setCreatedBy(currentUser);
        } else {
            cleanupPreviousDraftFiles(face, storedFile.storageKey());
        }

        face.setOriginalS3Key(storedFile.storageKey());
        face.setOriginalFilepath(storedFile.filepath());
        face.setOriginalUploadFilename(normalizeOriginalFilename(file));
        face.setOriginalContentType(storedFile.contentType());
        face.setOriginalSizeBytes(storedFile.sizeBytes());
        face.setOriginalWidthPx(dimensions.width());
        face.setOriginalHeightPx(dimensions.height());
        face.setTargetAspectRatio(calculateTargetAspectRatio(productBoxModel, faceName));
        face.setProcessedS3Key(null);
        face.setProcessedFilepath(null);
        face.setProcessedFilename(null);
        face.setProcessedContentType(null);
        face.setProcessedSizeBytes(null);
        face.setProcessedWidthPx(null);
        face.setProcessedHeightPx(null);
        face.setPointsJson(null);
        face.setProcessingError(null);
        face.setProcessedAt(null);
        face.setAutoDetectedPoints(false);
        face.setContourConfidence(null);
        face.setEnhancementMode(ProductBoxTextureEnhancementMode.BASIC.getValue());
        face.setTextureStatus(ProductBoxTextureStatus.UPLOADED);
        face.setUpdatedBy(currentUser);

        return productBoxModelMapper.toFaceResponse(productBoxModelFaceRepository.save(face));
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public ProductBoxTextureContourDetectionResponse detectContour(UUID productBoxModelId, String faceNameValue) {
        ProductBoxModelFace face = findFace(productBoxModelId, faceNameValue);
        User currentUser = findCurrentUser();

        if (face.getOriginalS3Key() == null || face.getOriginalS3Key().isBlank()) {
            throw new BadRequestException("Original product box face image is required before detecting contour");
        }

        Resource originalResource = fileStorageService.loadAsResource(face.getOriginalS3Key());
        ProductBoxTextureProcessingService.DetectedProductBoxContour detection;

        try {
            detection = productBoxTextureProcessingService.detectContour(
                originalResource,
                face.getOriginalWidthPx(),
                face.getOriginalHeightPx()
            );
        } catch (RuntimeException ex) {
            face.setAutoDetectedPoints(false);
            face.setContourConfidence(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
            face.setProcessingError(truncate(ex.getMessage(), 1000));
            face.setUpdatedBy(currentUser);
            productBoxModelFaceRepository.save(face);
            throw ex;
        }

        face.setAutoDetectedPoints(detection.detected());
        face.setContourConfidence(detection.confidence());
        face.setProcessingError(detection.detected() ? null : detection.message());

        if (detection.detected() && detection.points() != null) {
            face.setPointsJson(toJson(detection.points()));
            face.setTextureStatus(ProductBoxTextureStatus.POINTS_SELECTED);
        }

        face.setUpdatedBy(currentUser);
        productBoxModelFaceRepository.save(face);

        return new ProductBoxTextureContourDetectionResponse(
            detection.detected(),
            detection.confidence(),
            detection.points(),
            detection.message()
        );
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public ProductBoxModelFaceResponse processTexture(
        UUID productBoxModelId,
        String faceNameValue,
        ProductBoxTextureProcessRequest request
    ) {
        ProductBoxModelFace face = findFace(productBoxModelId, faceNameValue);
        ProductBoxModel productBoxModel = face.getProductBoxModel();
        ProductBoxFaceName faceName = face.getFaceName();
        User currentUser = findCurrentUser();

        if (face.getOriginalS3Key() == null || face.getOriginalS3Key().isBlank()) {
            throw new BadRequestException("Original product box face image is required before processing texture");
        }

        Resource originalResource = fileStorageService.loadAsResource(face.getOriginalS3Key());
        ProductBoxTextureProcessingService.ProcessedProductBoxTexture processedTexture;

        try {
            processedTexture = productBoxTextureProcessingService.process(
                originalResource,
                productBoxModel,
                faceName,
                request,
                face.getOriginalWidthPx(),
                face.getOriginalHeightPx()
            );
        } catch (RuntimeException ex) {
            face.setTextureStatus(ProductBoxTextureStatus.FAILED);
            face.setProcessingError(truncate(ex.getMessage(), 1000));
            face.setUpdatedBy(currentUser);
            productBoxModelFaceRepository.save(face);
            throw ex;
        }

        MultipartFile processedFile = new ByteArrayMultipartFile(
            "file",
            processedTexture.filename(),
            PROCESSED_TEXTURE_CONTENT_TYPE,
            processedTexture.bytes()
        );

        var storedFile = fileStorageService.store(
            processedFile,
            productBoxModel.getOrganization().getId()
                + "/catalogs/product_box_models/"
                + productBoxModel.getId()
                + "/faces/"
                + faceName.getValue()
                + "/processed"
        );

        String previousProcessedS3Key = face.getProcessedS3Key();
        if (shouldDeleteDraftKey(face, previousProcessedS3Key)) {
            try {
                fileStorageService.delete(previousProcessedS3Key);
            } catch (RuntimeException ex) {
                cleanupNewFile(storedFile.storageKey());
                throw ex;
            }
        }

        face.setProcessedS3Key(storedFile.storageKey());
        face.setProcessedFilepath(storedFile.filepath());
        face.setProcessedFilename(processedTexture.filename());
        face.setProcessedContentType(storedFile.contentType());
        face.setProcessedSizeBytes(storedFile.sizeBytes());
        face.setProcessedWidthPx(processedTexture.widthPx());
        face.setProcessedHeightPx(processedTexture.heightPx());
        face.setTargetAspectRatio(processedTexture.targetAspectRatio());
        face.setPointsJson(toJson(request));
        face.setTextureStatus(ProductBoxTextureStatus.PROCESSED);
        face.setProcessingError(null);
        face.setProcessedAt(OffsetDateTime.now());
        face.setEnhancementMode(processedTexture.enhancementMode().getValue());
        face.setUpdatedBy(currentUser);

        return productBoxModelMapper.toFaceResponse(productBoxModelFaceRepository.save(face));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public ProductBoxModelFaceResponse acceptProcessedTexture(UUID productBoxModelId, String faceNameValue) {
        ProductBoxModelFace face = findFace(productBoxModelId, faceNameValue);
        User currentUser = findCurrentUser();

        if (face.getProcessedS3Key() == null || face.getProcessedS3Key().isBlank()) {
            throw new BadRequestException("Processed product box texture is required before accepting texture");
        }

        String previousActiveS3Key = face.getS3Key();
        if (shouldDeleteActiveKey(previousActiveS3Key, face.getProcessedS3Key())) {
            fileStorageService.delete(previousActiveS3Key);
        }

        face.setS3Key(face.getProcessedS3Key());
        face.setFilepath(face.getProcessedFilepath());
        face.setOriginalFilename(face.getProcessedFilename());
        face.setContentType(face.getProcessedContentType());
        face.setSizeBytes(face.getProcessedSizeBytes());
        face.setTextureStatus(ProductBoxTextureStatus.ACCEPTED);
        face.setProcessingError(null);
        face.setAcceptedAt(OffsetDateTime.now());
        face.setUpdatedBy(currentUser);

        return productBoxModelMapper.toFaceResponse(productBoxModelFaceRepository.save(face));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public void deleteTexture(UUID productBoxModelId, String faceNameValue) {
        delete(productBoxModelId, faceNameValue);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public ProductBoxModelFaceResponse uploadOrReplace(
        UUID productBoxModelId,
        String faceNameValue,
        MultipartFile file,
        BigDecimal rotationDegrees,
        Boolean flipHorizontal,
        Boolean flipVertical
    ) {
        imageValidationService.validateImage(file);
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        ProductBoxModel productBoxModel = validateProductBoxModel(productBoxModelId, organizationId);
        ProductBoxFaceName faceName = ProductBoxFaceName.fromValue(faceNameValue);
        User currentUser = findCurrentUser();

        ProductBoxModelFace existingFace = productBoxModelFaceRepository
            .findByProductBoxModel_IdAndOrganization_IdAndFaceName(productBoxModelId, organizationId, faceName)
            .orElse(null);

        var storedFile = fileStorageService.store(
            file,
            productBoxModel.getOrganization().getId()
                + "/catalogs/product_box_models/"
                + productBoxModel.getId()
                + "/faces/"
                + faceName.getValue()
        );

        if (existingFace != null) {
            if (existingFace.getS3Key() != null && !existingFace.getS3Key().isBlank()) {
                try {
                    fileStorageService.delete(existingFace.getS3Key());
                } catch (RuntimeException ex) {
                    cleanupNewFile(storedFile.storageKey());
                    throw ex;
                }
            }

            existingFace.setOriginalFilename(normalizeOriginalFilename(file));
            existingFace.setS3Key(storedFile.storageKey());
            existingFace.setFilepath(storedFile.filepath());
            existingFace.setContentType(storedFile.contentType());
            existingFace.setSizeBytes(storedFile.sizeBytes());
            existingFace.setRotationDegrees(rotationDegrees);
            existingFace.setFlipHorizontal(Boolean.TRUE.equals(flipHorizontal));
            existingFace.setFlipVertical(Boolean.TRUE.equals(flipVertical));
            existingFace.setTextureStatus(ProductBoxTextureStatus.ACCEPTED);
            existingFace.setProcessingError(null);
            existingFace.setAcceptedAt(OffsetDateTime.now());
            existingFace.setUpdatedBy(currentUser);
            return productBoxModelMapper.toFaceResponse(productBoxModelFaceRepository.save(existingFace));
        }

        ProductBoxModelFace entity = new ProductBoxModelFace();
        entity.setOrganization(productBoxModel.getOrganization());
        entity.setProductBoxModel(productBoxModel);
        entity.setFaceName(faceName);
        entity.setOriginalFilename(normalizeOriginalFilename(file));
        entity.setS3Key(storedFile.storageKey());
        entity.setFilepath(storedFile.filepath());
        entity.setContentType(storedFile.contentType());
        entity.setSizeBytes(storedFile.sizeBytes());
        entity.setRotationDegrees(rotationDegrees);
        entity.setFlipHorizontal(Boolean.TRUE.equals(flipHorizontal));
        entity.setFlipVertical(Boolean.TRUE.equals(flipVertical));
        entity.setTargetAspectRatio(calculateTargetAspectRatio(productBoxModel, faceName));
        entity.setTextureStatus(ProductBoxTextureStatus.ACCEPTED);
        entity.setAcceptedAt(OffsetDateTime.now());
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        return productBoxModelMapper.toFaceResponse(productBoxModelFaceRepository.save(entity));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public Resource getFile(UUID productBoxModelId, String faceNameValue) {
        ProductBoxModelFace face = findFace(productBoxModelId, faceNameValue);
        if (face.getS3Key() == null || face.getS3Key().isBlank()) {
            throw new NotFoundException("Accepted product box face image not found");
        }
        return fileStorageService.loadAsResource(face.getS3Key());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public MediaType getMediaType(UUID productBoxModelId, String faceNameValue) {
        ProductBoxModelFace face = findFace(productBoxModelId, faceNameValue);
        if (face.getContentType() == null || face.getContentType().isBlank()) {
            throw new NotFoundException("Accepted product box face image not found");
        }
        return MediaType.parseMediaType(face.getContentType());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public Resource getOriginalFile(UUID productBoxModelId, String faceNameValue) {
        ProductBoxModelFace face = findFace(productBoxModelId, faceNameValue);
        if (face.getOriginalS3Key() == null || face.getOriginalS3Key().isBlank()) {
            throw new NotFoundException("Original product box face image not found");
        }
        return fileStorageService.loadAsResource(face.getOriginalS3Key());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public MediaType getOriginalMediaType(UUID productBoxModelId, String faceNameValue) {
        ProductBoxModelFace face = findFace(productBoxModelId, faceNameValue);
        if (face.getOriginalContentType() == null || face.getOriginalContentType().isBlank()) {
            throw new NotFoundException("Original product box face image not found");
        }
        return MediaType.parseMediaType(face.getOriginalContentType());
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public void delete(UUID productBoxModelId, String faceNameValue) {
        ProductBoxModelFace face = findFace(productBoxModelId, faceNameValue);
        deleteAllStorageKeys(face);
        productBoxModelFaceRepository.delete(face);
    }

    private ProductBoxModel validateProductBoxModel(UUID productBoxModelId, UUID organizationId) {
        return productBoxModelRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(productBoxModelId, organizationId)
            .orElseThrow(() -> new NotFoundException("Product box model not found"));
    }

    private ProductBoxModelFace findFace(UUID productBoxModelId, String faceNameValue) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        validateProductBoxModel(productBoxModelId, organizationId);
        ProductBoxFaceName faceName = ProductBoxFaceName.fromValue(faceNameValue);

        return productBoxModelFaceRepository
            .findByProductBoxModel_IdAndOrganization_IdAndFaceName(productBoxModelId, organizationId, faceName)
            .orElseThrow(() -> new NotFoundException("Product box model face not found"));
    }

    private User findCurrentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private String normalizeOriginalFilename(MultipartFile file) {
        return file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()
            ? file.getOriginalFilename()
            : "image";
    }

    private void cleanupPreviousDraftFiles(ProductBoxModelFace face, String newStorageKey) {
        List<String> keysToDelete = new ArrayList<>();
        addDraftKeyIfPresent(keysToDelete, face, face.getProcessedS3Key());
        addDraftKeyIfPresent(keysToDelete, face, face.getOriginalS3Key());

        try {
            for (String storageKey : keysToDelete) {
                fileStorageService.delete(storageKey);
            }
        } catch (RuntimeException ex) {
            cleanupNewFile(newStorageKey);
            throw ex;
        }
    }

    private void addDraftKeyIfPresent(List<String> storageKeys, ProductBoxModelFace face, String storageKey) {
        if (shouldDeleteDraftKey(face, storageKey)) {
            addIfPresent(storageKeys, storageKey);
        }
    }

    private boolean shouldDeleteDraftKey(ProductBoxModelFace face, String storageKey) {
        return storageKey != null && !storageKey.isBlank() && !storageKey.equals(face.getS3Key());
    }

    private boolean shouldDeleteActiveKey(String activeStorageKey, String acceptedStorageKey) {
        return activeStorageKey != null && !activeStorageKey.isBlank() && !activeStorageKey.equals(acceptedStorageKey);
    }

    private void deleteAllStorageKeys(ProductBoxModelFace face) {
        List<String> keysToDelete = new ArrayList<>();
        addIfPresent(keysToDelete, face.getS3Key());
        addIfPresent(keysToDelete, face.getOriginalS3Key());
        addIfPresent(keysToDelete, face.getProcessedS3Key());

        for (String storageKey : keysToDelete) {
            fileStorageService.delete(storageKey);
        }
    }

    private void addIfPresent(List<String> storageKeys, String storageKey) {
        if (storageKey != null && !storageKey.isBlank() && !storageKeys.contains(storageKey)) {
            storageKeys.add(storageKey);
        }
    }

    private void cleanupNewFile(String storageKey) {
        try {
            fileStorageService.delete(storageKey);
        } catch (RuntimeException ignored) {
            // Best effort cleanup. The original exception remains the failure reason.
        }
    }

    private ImageDimensions readDimensions(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                return ImageDimensions.empty();
            }
            return new ImageDimensions(image.getWidth(), image.getHeight());
        } catch (IOException ex) {
            return ImageDimensions.empty();
        }
    }

    private BigDecimal calculateTargetAspectRatio(ProductBoxModel productBoxModel, ProductBoxFaceName faceName) {
        BigDecimal targetWidth = switch (faceName) {
            case FRONT, BACK, TOP, BOTTOM -> productBoxModel.getWidth();
            case LEFT, RIGHT -> productBoxModel.getDepth();
        };

        BigDecimal targetHeight = switch (faceName) {
            case FRONT, BACK, LEFT, RIGHT -> productBoxModel.getHeight();
            case TOP, BOTTOM -> productBoxModel.getDepth();
        };

        if (targetWidth == null || targetHeight == null || BigDecimal.ZERO.compareTo(targetHeight) == 0) {
            throw new BadRequestException("Invalid product box dimensions");
        }

        return targetWidth.divide(targetHeight, 6, RoundingMode.HALF_UP);
    }

    private String toJson(ProductBoxTextureProcessRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Product box texture points could not be serialized");
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record ImageDimensions(Integer width, Integer height) {
        private static ImageDimensions empty() {
            return new ImageDimensions(null, null);
        }
    }

    private static class ByteArrayMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] bytes;

        private ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] bytes) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.bytes = bytes != null ? bytes : new byte[0];
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes.clone();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            java.nio.file.Files.write(dest.toPath(), bytes);
        }
    }
}
