package com.tamias.productbox.service;

import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.document.storage.FileStorageService;
import com.tamias.image.service.ImageValidationService;
import com.tamias.productbox.dto.ProductBoxModelFaceResponse;
import com.tamias.productbox.entity.ProductBoxModel;
import com.tamias.productbox.entity.ProductBoxModelFace;
import com.tamias.productbox.enums.ProductBoxFaceName;
import com.tamias.productbox.enums.ProductBoxTextureStatus;
import com.tamias.productbox.mapper.ProductBoxModelMapper;
import com.tamias.productbox.repository.ProductBoxModelFaceRepository;
import com.tamias.productbox.repository.ProductBoxModelRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.entity.User;
import com.tamias.user.repository.UserRepository;
import java.awt.image.BufferedImage;
import java.io.IOException;
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

    private final ProductBoxModelRepository productBoxModelRepository;
    private final ProductBoxModelFaceRepository productBoxModelFaceRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final ImageValidationService imageValidationService;
    private final ProductBoxModelMapper productBoxModelMapper;

    public ProductBoxModelFaceService(
        ProductBoxModelRepository productBoxModelRepository,
        ProductBoxModelFaceRepository productBoxModelFaceRepository,
        UserRepository userRepository,
        CurrentUserService currentUserService,
        FileStorageService fileStorageService,
        ImageValidationService imageValidationService,
        ProductBoxModelMapper productBoxModelMapper
    ) {
        this.productBoxModelRepository = productBoxModelRepository;
        this.productBoxModelFaceRepository = productBoxModelFaceRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
        this.imageValidationService = imageValidationService;
        this.productBoxModelMapper = productBoxModelMapper;
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
        face.setTextureStatus(ProductBoxTextureStatus.UPLOADED);
        face.setUpdatedBy(currentUser);

        return productBoxModelMapper.toFaceResponse(productBoxModelFaceRepository.save(face));
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
        addIfPresent(keysToDelete, face.getProcessedS3Key());
        addIfPresent(keysToDelete, face.getOriginalS3Key());

        try {
            for (String storageKey : keysToDelete) {
                fileStorageService.delete(storageKey);
            }
        } catch (RuntimeException ex) {
            cleanupNewFile(newStorageKey);
            throw ex;
        }
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

    private record ImageDimensions(Integer width, Integer height) {
        private static ImageDimensions empty() {
            return new ImageDimensions(null, null);
        }
    }
}
