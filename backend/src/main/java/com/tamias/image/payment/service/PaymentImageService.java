package com.tamias.image.payment.service;

import com.tamias.common.exception.NotFoundException;
import com.tamias.document.storage.FileStorageService;
import com.tamias.image.dto.ImageResponse;
import com.tamias.image.dto.ImageUploadResponse;
import com.tamias.image.enums.ImageStatus;
import com.tamias.image.mapper.ImageMapper;
import com.tamias.image.payment.entity.PaymentImage;
import com.tamias.image.payment.repository.PaymentImageRepository;
import com.tamias.image.service.ImageValidationService;
import com.tamias.payment.entity.Payment;
import com.tamias.payment.repository.PaymentRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.entity.User;
import com.tamias.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PaymentImageService {

    private final PaymentImageRepository paymentImageRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final ImageValidationService imageValidationService;
    private final ImageMapper imageMapper;

    public PaymentImageService(
            PaymentImageRepository paymentImageRepository,
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            FileStorageService fileStorageService,
            ImageValidationService imageValidationService,
            ImageMapper imageMapper
    ) {
        this.paymentImageRepository = paymentImageRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
        this.imageValidationService = imageValidationService;
        this.imageMapper = imageMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public List<ImageResponse> findAll(UUID paymentId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        validatePayment(paymentId, organizationId);

        return paymentImageRepository
                .findByPayment_IdAndOrganization_IdAndStatusOrderByCreatedAtDesc(
                        paymentId,
                        organizationId,
                        ImageStatus.ACTIVE
                )
                .stream()
                .map(imageMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public ImageResponse findById(UUID paymentId, UUID imageId) {
        return imageMapper.toResponse(findImage(paymentId, imageId));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public ImageUploadResponse upload(UUID paymentId, MultipartFile file) {
        imageValidationService.validateImage(file);

        UUID organizationId = currentUserService.getCurrentOrganizationId();
        Payment payment = validatePayment(paymentId, organizationId);
        User currentUser = getCurrentUser();

        var storedFile = fileStorageService.store(
                file,
                payment.getOrganization().getId() + "/payments/" + payment.getId()
        );

        PaymentImage entity = new PaymentImage();
        entity.setOrganization(payment.getOrganization());
        entity.setPayment(payment);
        entity.setOriginalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image");
        entity.setS3Key(storedFile.storageKey());
        entity.setFilepath(storedFile.filepath());
        entity.setContentType(storedFile.contentType());
        entity.setSizeBytes(storedFile.sizeBytes());
        entity.setStatus(ImageStatus.ACTIVE);
        entity.setCreatedBy(currentUser);

        return imageMapper.toUploadResponse(paymentImageRepository.save(entity));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public Resource getFile(UUID paymentId, UUID imageId) {
        PaymentImage image = findImage(paymentId, imageId);
        return fileStorageService.loadAsResource(image.getS3Key());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public MediaType getMediaType(UUID paymentId, UUID imageId) {
        PaymentImage image = findImage(paymentId, imageId);
        return MediaType.parseMediaType(image.getContentType());
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public void delete(UUID paymentId, UUID imageId) {
        PaymentImage image = findImage(paymentId, imageId);

        fileStorageService.delete(image.getS3Key());
        paymentImageRepository.delete(image);
    }

    private Payment validatePayment(UUID paymentId, UUID organizationId) {
        return paymentRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(paymentId, organizationId)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
    }

    private PaymentImage findImage(UUID paymentId, UUID imageId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        validatePayment(paymentId, organizationId);

        return paymentImageRepository
                .findByIdAndPayment_IdAndOrganization_IdAndStatus(
                        imageId,
                        paymentId,
                        organizationId,
                        ImageStatus.ACTIVE
                )
                .orElseThrow(() -> new NotFoundException("Payment image not found"));
    }

    private User getCurrentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
