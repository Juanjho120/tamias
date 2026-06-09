package com.tamias.reservation.service;

import com.tamias.catalog.platform.repository.PlatformRepository;
import com.tamias.common.dto.PageResponse;
import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.ConflictException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.guest.entity.Guest;
import com.tamias.guest.enums.GuestStatus;
import com.tamias.guest.repository.GuestRepository;
import com.tamias.organization.entity.Organization;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.property.repository.PropertyRepository;
import com.tamias.reservation.dto.ReservationGuestRequest;
import com.tamias.reservation.dto.ReservationRequest;
import com.tamias.reservation.dto.ReservationResponse;
import com.tamias.reservation.enums.ReservationStatus;
import com.tamias.reservation.mapper.ReservationMapper;
import com.tamias.reservation.entity.Reservation;
import com.tamias.reservation.entity.ReservationGuest;
import com.tamias.reservation.repository.ReservationGuestRepository;
import com.tamias.reservation.repository.ReservationRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.entity.User;
import com.tamias.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationGuestRepository reservationGuestRepository;
    private final GuestRepository guestRepository;
    private final OrganizationRepository organizationRepository;
    private final PropertyRepository propertyRepository;
    private final PlatformRepository platformRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ReservationMapper reservationMapper;

    public ReservationService(
            ReservationRepository reservationRepository,
            ReservationGuestRepository reservationGuestRepository,
            GuestRepository guestRepository,
            OrganizationRepository organizationRepository,
            PropertyRepository propertyRepository,
            PlatformRepository platformRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            ReservationMapper reservationMapper
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationGuestRepository = reservationGuestRepository;
        this.guestRepository = guestRepository;
        this.organizationRepository = organizationRepository;
        this.propertyRepository = propertyRepository;
        this.platformRepository = platformRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.reservationMapper = reservationMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'READ_ONLY')")
    public PageResponse findAll(
            UUID propertyId,
            ReservationStatus status,
            Pageable pageable
    ) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        var page = propertyId == null && status == null
                ? reservationRepository.findByOrganization_IdAndDeletedAtIsNull(organizationId, pageable)
                : propertyId != null && status == null
                ? reservationRepository.findByOrganization_IdAndProperty_IdAndDeletedAtIsNull(organizationId, propertyId, pageable)
                : propertyId == null
                ? reservationRepository.findByOrganization_IdAndStatusAndDeletedAtIsNull(organizationId, status, pageable)
                : reservationRepository.findByOrganization_IdAndProperty_IdAndStatusAndDeletedAtIsNull(organizationId, propertyId, status, pageable);

        return PageResponse.from(page.map(reservation -> reservationMapper.toSummaryResponse(
                reservation,
                reservationGuestRepository.findByReservation_Id(reservation.getId())
        )));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'READ_ONLY')")
    public PageResponse findCalendar(
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Start date and end date are required");
        }

        if (!endDate.isAfter(startDate)) {
            throw new BadRequestException("End date must be after start date");
        }

        UUID organizationId = currentUserService.getCurrentOrganizationId();

        var page = reservationRepository
                .findByOrganization_IdAndCheckInLessThanAndCheckOutGreaterThanAndDeletedAtIsNull(
                        organizationId,
                        endDate,
                        startDate,
                        pageable
                )
                .map(reservation -> reservationMapper.toSummaryResponse(
                        reservation,
                        reservationGuestRepository.findByReservation_Id(reservation.getId())
                ));

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'READ_ONLY')")
    public ReservationResponse findById(UUID id) {
        Reservation reservation = findEntityInCurrentOrganization(id);

        return reservationMapper.toResponse(
                reservation,
                reservationGuestRepository.findByReservation_Id(reservation.getId())
        );
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public ReservationResponse create(ReservationRequest request) {
        validateDates(request);

        UUID organizationId = currentUserService.getCurrentOrganizationId();

        Organization organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        var property = propertyRepository
                .findByIdAndOrganization_IdAndDeletedAtIsNull(request.propertyId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Property not found"));

        validateAvailability(organizationId, request.propertyId(), request.checkIn(), request.checkOut(), null);

        User currentUser = userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        Reservation entity = new Reservation();
        entity.setOrganization(organization);
        entity.setProperty(property);
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        reservationMapper.updateEntity(entity, request);
        setOptionalPlatform(entity, request.platformId(), organizationId);

        Reservation saved = reservationRepository.save(entity);
        replaceGuests(saved, request.guests(), organization, currentUser);

        return reservationMapper.toResponse(
                saved,
                reservationGuestRepository.findByReservation_Id(saved.getId())
        );
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public ReservationResponse update(UUID id, ReservationRequest request) {
        validateDates(request);

        UUID organizationId = currentUserService.getCurrentOrganizationId();

        Reservation entity = findEntityInCurrentOrganization(id);

        var property = propertyRepository
                .findByIdAndOrganization_IdAndDeletedAtIsNull(request.propertyId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Property not found"));

        User currentUser = userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        entity.setProperty(property);
        entity.setUpdatedBy(currentUser);

        reservationMapper.updateEntity(entity, request);
        setOptionalPlatform(entity, request.platformId(), organizationId);

        Reservation saved = reservationRepository.save(entity);

        reservationGuestRepository.deleteByReservation_Id(saved.getId());

        /*
         * Important:
         * deleteByReservation_Id is executed in the same transaction.
         * Flush before inserting replacement guests so PostgreSQL sees the old
         * reservation_guest rows deleted before we insert the same guest again.
         */
        reservationGuestRepository.flush();

        replaceGuests(saved, request.guests(), saved.getOrganization(), currentUser);

        return reservationMapper.toResponse(
                saved,
                reservationGuestRepository.findByReservation_Id(saved.getId())
        );
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public ReservationResponse cancel(UUID id, String reason) {
        Reservation entity = findEntityInCurrentOrganization(id);

        var currentUser = userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        entity.setStatus(ReservationStatus.CANCELLED);
        entity.setObservations(appendReason(entity.getObservations(), reason));
        entity.setUpdatedBy(currentUser);

        Reservation saved = reservationRepository.save(entity);

        return reservationMapper.toResponse(
                saved,
                reservationGuestRepository.findByReservation_Id(saved.getId())
        );
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public void delete(UUID id) {
        Reservation entity = findEntityInCurrentOrganization(id);

        var currentUser = userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        entity.setStatus(ReservationStatus.DELETED);
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setDeletedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        reservationRepository.save(entity);
    }

    private Reservation findEntityInCurrentOrganization(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        return reservationRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
    }

    private void setOptionalPlatform(Reservation entity, UUID platformId, UUID organizationId) {
        if (platformId == null) {
            entity.setPlatform(null);
            return;
        }

        var platform = platformRepository
                .findByIdAndOrganization_IdAndDeletedAtIsNull(platformId, organizationId)
                .orElseThrow(() -> new NotFoundException("Platform not found"));

        entity.setPlatform(platform);
    }

    private void replaceGuests(
            Reservation reservation,
            List<ReservationGuestRequest> guestRequests,
            Organization organization,
            User currentUser
    ) {
        if (guestRequests == null || guestRequests.isEmpty()) {
            return;
        }

        Set<UUID> guestIdsInRequest = new LinkedHashSet<>();

        for (ReservationGuestRequest guestRequest : guestRequests) {
            Guest guest = resolveGuest(guestRequest, organization, currentUser);

            if (!guestIdsInRequest.add(guest.getId())) {
                throw new BadRequestException("Duplicate guest in reservation request");
            }

            ReservationGuest reservationGuest = new ReservationGuest();
            reservationGuest.setOrganization(organization);
            reservationGuest.setReservation(reservation);
            reservationGuest.setGuest(guest);
            reservationGuest.setPrimaryGuest(Boolean.TRUE.equals(guestRequest.primary()));

            reservationGuestRepository.save(reservationGuest);
        }
    }

    private Guest resolveGuest(
            ReservationGuestRequest guestRequest,
            Organization organization,
            User currentUser
    ) {
        UUID organizationId = organization.getId();

        if (guestRequest.guestId() != null) {
            return guestRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(guestRequest.guestId(), organizationId)
                    .orElseThrow(() -> new NotFoundException("Guest not found"));
        }

        if (guestRequest.fullName() == null || guestRequest.fullName().isBlank()) {
            throw new BadRequestException("Guest full name is required when guestId is not provided");
        }

        Guest guest = new Guest();
        guest.setOrganization(organization);
        guest.setFullName(guestRequest.fullName());
        guest.setPhone(guestRequest.phone());
        guest.setStatus(GuestStatus.ACTIVE);
        guest.setCreatedBy(currentUser);
        guest.setUpdatedBy(currentUser);

        return guestRepository.save(guest);
    }

    private void validateDates(ReservationRequest request) {
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw new BadRequestException("Check-out date must be after check-in date");
        }
    }

    private void validateAvailability(
            UUID organizationId,
            UUID propertyId,
            LocalDate checkIn,
            LocalDate checkOut,
            UUID currentReservationId
    ) {
        boolean overlappingReservationExists = reservationRepository
                .existsByOrganization_IdAndProperty_IdAndStatusNotAndDeletedAtIsNullAndCheckInLessThanAndCheckOutGreaterThan(
                        organizationId,
                        propertyId,
                        ReservationStatus.CANCELLED,
                        checkOut,
                        checkIn
                );

        if (!overlappingReservationExists) {
            return;
        }

        throw new ConflictException("Property is not available for the selected dates");
    }

    private String appendReason(String observations, String reason) {
        if (reason == null || reason.isBlank()) {
            return observations;
        }

        if (observations == null || observations.isBlank()) {
            return "Cancellation reason: " + reason;
        }

        return observations + "\nCancellation reason: " + reason;
    }
}
