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
import com.tamias.productbox.enums.ProductBoxActiveTextureSource;
import com.tamias.productbox.enums.ProductBoxAiEnhancementStatus;
import com.tamias.productbox.enums.ProductBoxFaceName;
import com.tamias.productbox.enums.ProductBoxTextureEnhancementMode;
import com.tamias.productbox.enums.ProductBoxTextureStatus;
import com.tamias.productbox.mapper.ProductBoxModelMapper;
import com.tamias.productbox.repository.ProductBoxModelFaceRepository;
import com.tamias.productbox.repository.ProductBoxModelRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.entity.User;
import com.tamias.user.repository.UserRepository;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
    private final List<ProductBoxAiTextureEnhancementProvider> aiTextureEnhancementProviders;
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
        List<ProductBoxAiTextureEnhancementProvider> aiTextureEnhancementProviders,
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
        this.aiTextureEnhancementProviders = aiTextureEnhancementProviders != null ? aiTextureEnhancementProviders : List.of();
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
        clearAiEnhancementDraftMetadata(face);
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
        String previousAiEnhancedS3Key = face.getAiEnhancedS3Key();
        if (shouldDeleteDraftKey(face, previousProcessedS3Key)) {
            try {
                fileStorageService.delete(previousProcessedS3Key);
            } catch (RuntimeException ex) {
                cleanupNewFile(storedFile.storageKey());
                throw ex;
            }
        }

        if (shouldDeleteDraftKey(face, previousAiEnhancedS3Key)) {
            try {
                fileStorageService.delete(previousAiEnhancedS3Key);
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
        clearAiEnhancementDraftMetadata(face);
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
        String previousAiEnhancedS3Key = face.getAiEnhancedS3Key();

        if (shouldDeleteActiveKey(previousActiveS3Key, face.getProcessedS3Key())) {
            fileStorageService.delete(previousActiveS3Key);
        }

        if (shouldDeleteDraftKey(face, previousAiEnhancedS3Key)) {
            fileStorageService.delete(previousAiEnhancedS3Key);
        }

        clearAiEnhancementMetadata(face);
        face.setS3Key(face.getProcessedS3Key());
        face.setFilepath(face.getProcessedFilepath());
        face.setOriginalFilename(face.getProcessedFilename());
        face.setContentType(face.getProcessedContentType());
        face.setSizeBytes(face.getProcessedSizeBytes());
        face.setTextureStatus(ProductBoxTextureStatus.ACCEPTED);
        face.setActiveTextureSource(ProductBoxActiveTextureSource.OPENCV_PROCESSED);
        face.setProcessingError(null);
        face.setAcceptedAt(OffsetDateTime.now());
        face.setUpdatedBy(currentUser);

        return productBoxModelMapper.toFaceResponse(productBoxModelFaceRepository.save(face));
    }


    @Transactional(noRollbackFor = BadRequestException.class)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public ProductBoxModelFaceResponse generateAiEnhancedTexture(UUID productBoxModelId, String faceNameValue) {
        ProductBoxModelFace face = findFace(productBoxModelId, faceNameValue);
        ProductBoxModel productBoxModel = face.getProductBoxModel();
        User currentUser = findCurrentUser();

        if (face.getProcessedS3Key() == null || face.getProcessedS3Key().isBlank()) {
            throw new BadRequestException("Processed product box texture is required before AI enhancement");
        }

        ProductBoxAiTextureEnhancementProvider provider = findAvailableAiEnhancementProvider();
        face.setAiEnhancementStatus(ProductBoxAiEnhancementStatus.PROCESSING);
        face.setAiEnhancementProvider(provider.getProviderName());
        face.setAiEnhancementModel(null);
        face.setAiEnhancementPromptVersion("product-box-texture-enhancement-v1");
        face.setAiEnhancementError(null);
        face.setUpdatedBy(currentUser);
        productBoxModelFaceRepository.save(face);

        try {
            byte[] processedBytes = readResourceBytes(fileStorageService.loadAsResource(face.getProcessedS3Key()));
            ProductBoxAiTextureEnhancementResult aiResult = provider.enhance(new ProductBoxAiTextureEnhancementRequest(
                face.getOrganization().getId(),
                productBoxModel.getId(),
                face.getId(),
                face.getFaceName(),
                face.getProcessedS3Key(),
                face.getProcessedFilename(),
                face.getProcessedContentType(),
                processedBytes,
                face.getTargetAspectRatio(),
                face.getProcessedWidthPx(),
                face.getProcessedHeightPx(),
                "product-box-texture-enhancement-v1"
            ));

            NormalizedImage normalizedImage = normalizeEnhancedImage(
                aiResult.bytes(),
                face.getProcessedWidthPx(),
                face.getProcessedHeightPx()
            );

            MultipartFile aiEnhancedFile = new ByteArrayMultipartFile(
                "file",
                buildAiEnhancedFilename(face),
                PROCESSED_TEXTURE_CONTENT_TYPE,
                normalizedImage.bytes()
            );

            var storedFile = fileStorageService.store(
                aiEnhancedFile,
                productBoxModel.getOrganization().getId()
                    + "/catalogs/product_box_models/"
                    + productBoxModel.getId()
                    + "/faces/"
                    + face.getFaceName().getValue()
                    + "/enhanced"
            );

            String previousAiEnhancedS3Key = face.getAiEnhancedS3Key();
            if (shouldDeleteDraftKey(face, previousAiEnhancedS3Key)) {
                try {
                    fileStorageService.delete(previousAiEnhancedS3Key);
                } catch (RuntimeException ex) {
                    cleanupNewFile(storedFile.storageKey());
                    throw ex;
                }
            }

            face.setAiEnhancedS3Key(storedFile.storageKey());
            face.setAiEnhancedFilepath(storedFile.filepath());
            face.setAiEnhancedFilename(aiEnhancedFile.getOriginalFilename());
            face.setAiEnhancedContentType(storedFile.contentType());
            face.setAiEnhancedSizeBytes(storedFile.sizeBytes());
            face.setAiEnhancedWidthPx(normalizedImage.width());
            face.setAiEnhancedHeightPx(normalizedImage.height());
            face.setAiEnhancementStatus(ProductBoxAiEnhancementStatus.GENERATED);
            face.setAiEnhancementProvider(aiResult.provider());
            face.setAiEnhancementModel(aiResult.model());
            face.setAiEnhancementPromptVersion(aiResult.promptVersion());
            face.setAiEnhancementError(null);
            face.setAiEnhancedAt(OffsetDateTime.now());
            face.setUpdatedBy(currentUser);

            return productBoxModelMapper.toFaceResponse(productBoxModelFaceRepository.save(face));
        } catch (BadRequestException ex) {
            markAiEnhancementFailed(face, currentUser, ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            markAiEnhancementFailed(face, currentUser, ex.getMessage());
            throw new BadRequestException("Product box AI texture enhancement failed: " + ex.getMessage());
        }
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public ProductBoxModelFaceResponse acceptAiEnhancedTexture(UUID productBoxModelId, String faceNameValue) {
        ProductBoxModelFace face = findFace(productBoxModelId, faceNameValue);
        User currentUser = findCurrentUser();

        if (face.getAiEnhancedS3Key() == null || face.getAiEnhancedS3Key().isBlank()) {
            throw new BadRequestException("AI-enhanced product box texture is required before accepting texture");
        }

        String previousActiveS3Key = face.getS3Key();
        if (shouldDeleteReplacedActiveKey(face, previousActiveS3Key, face.getAiEnhancedS3Key())) {
            fileStorageService.delete(previousActiveS3Key);
        }

        face.setS3Key(face.getAiEnhancedS3Key());
        face.setFilepath(face.getAiEnhancedFilepath());
        face.setOriginalFilename(face.getAiEnhancedFilename());
        face.setContentType(face.getAiEnhancedContentType());
        face.setSizeBytes(face.getAiEnhancedSizeBytes());
        face.setTextureStatus(ProductBoxTextureStatus.ACCEPTED);
        face.setAiEnhancementStatus(ProductBoxAiEnhancementStatus.ACCEPTED);
        face.setActiveTextureSource(ProductBoxActiveTextureSource.AI_ENHANCED);
        face.setProcessingError(null);
        face.setAiEnhancementError(null);
        face.setAcceptedAt(OffsetDateTime.now());
        face.setUpdatedBy(currentUser);

        return productBoxModelMapper.toFaceResponse(productBoxModelFaceRepository.save(face));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public ProductBoxModelFaceResponse discardAiEnhancedTexture(UUID productBoxModelId, String faceNameValue) {
        ProductBoxModelFace face = findFace(productBoxModelId, faceNameValue);
        User currentUser = findCurrentUser();

        if (face.getAiEnhancedS3Key() != null
            && face.getAiEnhancedS3Key().equals(face.getS3Key())
            && face.getActiveTextureSource() == ProductBoxActiveTextureSource.AI_ENHANCED) {
            throw new BadRequestException("Cannot discard the active AI-enhanced texture. Accept another texture or delete the face instead.");
        }

        String aiEnhancedS3Key = face.getAiEnhancedS3Key();
        if (shouldDeleteDraftKey(face, aiEnhancedS3Key)) {
            fileStorageService.delete(aiEnhancedS3Key);
        }

        clearAiEnhancementMetadata(face);
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
            String previousAiEnhancedS3Key = existingFace.getAiEnhancedS3Key();

            if (existingFace.getS3Key() != null && !existingFace.getS3Key().isBlank()) {
                try {
                    fileStorageService.delete(existingFace.getS3Key());
                } catch (RuntimeException ex) {
                    cleanupNewFile(storedFile.storageKey());
                    throw ex;
                }
            }

            if (shouldDeleteDraftKey(existingFace, previousAiEnhancedS3Key)) {
                try {
                    fileStorageService.delete(previousAiEnhancedS3Key);
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
            existingFace.setActiveTextureSource(ProductBoxActiveTextureSource.DIRECT_UPLOAD);
            clearAiEnhancementDraftMetadata(existingFace);
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
        entity.setActiveTextureSource(ProductBoxActiveTextureSource.DIRECT_UPLOAD);
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
        addDraftKeyIfPresent(keysToDelete, face, face.getAiEnhancedS3Key());

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
        addIfPresent(keysToDelete, face.getAiEnhancedS3Key());

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

    private void clearAiEnhancementDraftMetadata(ProductBoxModelFace face) {
        if (face.getAiEnhancedS3Key() != null && face.getAiEnhancedS3Key().equals(face.getS3Key())) {
            return;
        }

        clearAiEnhancementMetadata(face);
    }

    private void clearAiEnhancementMetadata(ProductBoxModelFace face) {
        face.setAiEnhancedS3Key(null);
        face.setAiEnhancedFilepath(null);
        face.setAiEnhancedFilename(null);
        face.setAiEnhancedContentType(null);
        face.setAiEnhancedSizeBytes(null);
        face.setAiEnhancedWidthPx(null);
        face.setAiEnhancedHeightPx(null);
        face.setAiEnhancementStatus(ProductBoxAiEnhancementStatus.NOT_REQUESTED);
        face.setAiEnhancementProvider(null);
        face.setAiEnhancementModel(null);
        face.setAiEnhancementPromptVersion(null);
        face.setAiEnhancementError(null);
        face.setAiEnhancedAt(null);
        if (face.getActiveTextureSource() == ProductBoxActiveTextureSource.AI_ENHANCED) {
            face.setActiveTextureSource(ProductBoxActiveTextureSource.UNKNOWN);
        }
    }


    private ProductBoxAiTextureEnhancementProvider findAvailableAiEnhancementProvider() {
        return aiTextureEnhancementProviders.stream()
            .filter(ProductBoxAiTextureEnhancementProvider::isAvailable)
            .findFirst()
            .orElseThrow(() -> new BadRequestException("Product box AI texture enhancement provider is not configured"));
    }

    private byte[] readResourceBytes(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return inputStream.readAllBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Processed product box texture could not be read");
        }
    }

    private NormalizedImage normalizeEnhancedImage(byte[] bytes, Integer targetWidth, Integer targetHeight) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
            if (source == null) {
                throw new BadRequestException("AI-enhanced product box texture is not a readable image");
            }

            int outputWidth = targetWidth != null && targetWidth > 0 ? targetWidth : source.getWidth();
            int outputHeight = targetHeight != null && targetHeight > 0 ? targetHeight : source.getHeight();

            BufferedImage output = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = output.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.drawImage(source, 0, 0, outputWidth, outputHeight, null);
            } finally {
                graphics.dispose();
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(output, "png", outputStream);
            return new NormalizedImage(outputStream.toByteArray(), outputWidth, outputHeight);
        } catch (IOException ex) {
            throw new BadRequestException("AI-enhanced product box texture could not be normalized");
        }
    }

    private void markAiEnhancementFailed(ProductBoxModelFace face, User currentUser, String errorMessage) {
        face.setAiEnhancementStatus(ProductBoxAiEnhancementStatus.FAILED);
        face.setAiEnhancementError(truncate(errorMessage, 1000));
        face.setUpdatedBy(currentUser);
        productBoxModelFaceRepository.save(face);
    }

    private String buildAiEnhancedFilename(ProductBoxModelFace face) {
        return "ai-enhanced-" + face.getFaceName().getValue() + "-" + UUID.randomUUID() + ".png";
    }

    private boolean shouldDeleteReplacedActiveKey(
        ProductBoxModelFace face,
        String previousActiveStorageKey,
        String newActiveStorageKey
    ) {
        return previousActiveStorageKey != null
            && !previousActiveStorageKey.isBlank()
            && !previousActiveStorageKey.equals(newActiveStorageKey)
            && !previousActiveStorageKey.equals(face.getOriginalS3Key())
            && !previousActiveStorageKey.equals(face.getProcessedS3Key())
            && !previousActiveStorageKey.equals(face.getAiEnhancedS3Key());
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

    private record NormalizedImage(byte[] bytes, Integer width, Integer height) {
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
