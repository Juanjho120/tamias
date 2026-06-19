package com.tamias.image.reservation.service;

import com.tamias.common.exception.NotFoundException;
import com.tamias.document.storage.FileStorageService;
import com.tamias.image.dto.ImageResponse;
import com.tamias.image.dto.ImageUploadResponse;
import com.tamias.image.enums.ImageStatus;
import com.tamias.image.mapper.ImageMapper;
import com.tamias.image.reservation.entity.ReservationImage;
import com.tamias.image.reservation.repository.ReservationImageRepository;
import com.tamias.image.service.ImageValidationService;
import com.tamias.reservation.entity.Reservation;
import com.tamias.reservation.repository.ReservationRepository;
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
public class ReservationImageService {

    private final ReservationImageRepository reservationImageRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final ImageValidationService imageValidationService;
    private final ImageMapper imageMapper;

    public ReservationImageService(
        ReservationImageRepository reservationImageRepository,
        ReservationRepository reservationRepository,
        UserRepository userRepository,
        CurrentUserService currentUserService,
        FileStorageService fileStorageService,
        ImageValidationService imageValidationService,
        ImageMapper imageMapper
    ) {
        this.reservationImageRepository = reservationImageRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
        this.imageValidationService = imageValidationService;
        this.imageMapper = imageMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public List<ImageResponse> findAll(UUID reservationId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        validateReservation(reservationId, organizationId);

        return reservationImageRepository
            .findByReservation_IdAndOrganization_IdAndStatusOrderByCreatedAtDesc(
                reservationId,
                organizationId,
                ImageStatus.ACTIVE
            )
            .stream()
            .map(imageMapper::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public ImageResponse findById(UUID reservationId, UUID imageId) {
        return imageMapper.toResponse(findImage(reservationId, imageId));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public ImageUploadResponse upload(UUID reservationId, MultipartFile file) {
        imageValidationService.validateImage(file);

        UUID organizationId = currentUserService.getCurrentOrganizationId();
        Reservation reservation = validateReservation(reservationId, organizationId);
        User currentUser = getCurrentUser();

        var storedFile = fileStorageService.store(
            file,
            reservation.getOrganization().getId() + "/reservations/" + reservation.getId()
        );

        ReservationImage entity = new ReservationImage();
        entity.setOrganization(reservation.getOrganization());
        entity.setReservation(reservation);
        entity.setOriginalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "image");
        entity.setS3Key(storedFile.storageKey());
        entity.setFilepath(storedFile.filepath());
        entity.setContentType(storedFile.contentType());
        entity.setSizeBytes(storedFile.sizeBytes());
        entity.setStatus(ImageStatus.ACTIVE);
        entity.setCreatedBy(currentUser);

        return imageMapper.toUploadResponse(reservationImageRepository.save(entity));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public Resource getFile(UUID reservationId, UUID imageId) {
        ReservationImage image = findImage(reservationId, imageId);
        return fileStorageService.loadAsResource(image.getS3Key());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public MediaType getMediaType(UUID reservationId, UUID imageId) {
        ReservationImage image = findImage(reservationId, imageId);
        return MediaType.parseMediaType(image.getContentType());
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public void delete(UUID reservationId, UUID imageId) {
        ReservationImage image = findImage(reservationId, imageId);
        fileStorageService.delete(image.getS3Key());
        reservationImageRepository.delete(image);
    }

    private Reservation validateReservation(UUID reservationId, UUID organizationId) {
        return reservationRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(reservationId, organizationId)
            .orElseThrow(() -> new NotFoundException("Reservation not found"));
    }

    private ReservationImage findImage(UUID reservationId, UUID imageId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        return reservationImageRepository
            .findByIdAndReservation_IdAndOrganization_IdAndStatus(
                imageId,
                reservationId,
                organizationId,
                ImageStatus.ACTIVE
            )
            .orElseThrow(() -> new NotFoundException("Reservation image not found"));
    }

    private User getCurrentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
