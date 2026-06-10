package com.tamias.reservation.service;

import com.tamias.catalog.inventoryitem.entity.InventoryItem;
import com.tamias.catalog.inventoryitem.repository.InventoryItemRepository;
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
import com.tamias.reservation.dto.ReservationSummaryResponse;
import com.tamias.reservation.dto.ReservationSupplyRequest;
import com.tamias.reservation.dto.ReservationSupplyResponse;
import com.tamias.reservation.dto.ReservationSupplyUpdateRequest;
import com.tamias.reservation.entity.Reservation;
import com.tamias.reservation.entity.ReservationGuest;
import com.tamias.reservation.entity.ReservationSupply;
import com.tamias.reservation.enums.ReservationStatus;
import com.tamias.reservation.mapper.ReservationMapper;
import com.tamias.reservation.repository.ReservationGuestRepository;
import com.tamias.reservation.repository.ReservationRepository;
import com.tamias.reservation.repository.ReservationSupplyRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.entity.User;
import com.tamias.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationGuestRepository reservationGuestRepository;
    private final ReservationSupplyRepository reservationSupplyRepository;
    private final GuestRepository guestRepository;
    private final OrganizationRepository organizationRepository;
    private final PropertyRepository propertyRepository;
    private final PlatformRepository platformRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ReservationMapper reservationMapper;

    public ReservationService(
            ReservationRepository reservationRepository,
            ReservationGuestRepository reservationGuestRepository,
            ReservationSupplyRepository reservationSupplyRepository,
            GuestRepository guestRepository,
            OrganizationRepository organizationRepository,
            PropertyRepository propertyRepository,
            PlatformRepository platformRepository,
            InventoryItemRepository inventoryItemRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            ReservationMapper reservationMapper
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationGuestRepository = reservationGuestRepository;
        this.reservationSupplyRepository = reservationSupplyRepository;
        this.guestRepository = guestRepository;
        this.organizationRepository = organizationRepository;
        this.propertyRepository = propertyRepository;
        this.platformRepository = platformRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.reservationMapper = reservationMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'READ_ONLY')")
    public PageResponse<ReservationSummaryResponse> findAll(
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
    public PageResponse<ReservationSummaryResponse> findCalendar(
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

        return toResponse(reservation);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'READ_ONLY')")
    public List<ReservationSupplyResponse> findSupplies(UUID reservationId) {
        Reservation reservation = findEntityInCurrentOrganization(reservationId);
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        return reservationSupplyRepository
                .findByReservation_IdAndOrganization_IdOrderByCreatedAtAsc(reservation.getId(), organizationId)
                .stream()
                .map(reservationMapper::toSupplyResponse)
                .toList();
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

        User currentUser = findCurrentUser();

        Reservation entity = new Reservation();
        entity.setOrganization(organization);
        entity.setProperty(property);
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        reservationMapper.updateEntity(entity, request);
        setOptionalPlatform(entity, request.platformId(), organizationId);

        Reservation saved = reservationRepository.save(entity);
        replaceGuests(saved, request.guests(), organization, currentUser);
        replaceSupplies(saved, request.supplies());

        return toResponse(saved);
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

        User currentUser = findCurrentUser();

        entity.setProperty(property);
        entity.setUpdatedBy(currentUser);

        reservationMapper.updateEntity(entity, request);
        setOptionalPlatform(entity, request.platformId(), organizationId);

        Reservation saved = reservationRepository.save(entity);

        reservationGuestRepository.deleteByReservation_Id(saved.getId());
        reservationGuestRepository.flush();
        replaceGuests(saved, request.guests(), saved.getOrganization(), currentUser);

        if (request.supplies() != null) {
            replaceSupplies(saved, request.supplies());
        }

        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public ReservationSupplyResponse addSupply(UUID reservationId, ReservationSupplyRequest request) {
        Reservation reservation = findEntityInCurrentOrganization(reservationId);
        ReservationSupply saved = createSupplyEntity(reservation, request);

        return reservationMapper.toSupplyResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public ReservationSupplyResponse updateSupply(
            UUID reservationId,
            UUID supplyId,
            ReservationSupplyUpdateRequest request
    ) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        ReservationSupply entity = reservationSupplyRepository
                .findByIdAndReservation_IdAndOrganization_Id(supplyId, reservationId, organizationId)
                .orElseThrow(() -> new NotFoundException("Reservation supply not found"));

        InventoryItem inventoryItem = resolveReservationInventoryItem(request.inventoryItemId(), organizationId);

        entity.setInventoryItem(inventoryItem);
        entity.setQuantity(request.quantity());
        entity.setUnit(resolveUnit(request.unit(), inventoryItem));
        entity.setItemNameSnapshot(inventoryItem.getName());
        entity.setInternalCodeSnapshot(inventoryItem.getInternalCode());
        entity.setBarcodeSnapshot(inventoryItem.getBarcode());
        entity.setNotes(request.notes());

        return reservationMapper.toSupplyResponse(reservationSupplyRepository.save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public void deleteSupply(UUID reservationId, UUID supplyId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        ReservationSupply entity = reservationSupplyRepository
                .findByIdAndReservation_IdAndOrganization_Id(supplyId, reservationId, organizationId)
                .orElseThrow(() -> new NotFoundException("Reservation supply not found"));

        reservationSupplyRepository.delete(entity);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public ReservationResponse cancel(UUID id, String reason) {
        Reservation entity = findEntityInCurrentOrganization(id);
        User currentUser = findCurrentUser();

        entity.setStatus(ReservationStatus.CANCELLED);
        entity.setObservations(appendReason(entity.getObservations(), reason));
        entity.setUpdatedBy(currentUser);

        Reservation saved = reservationRepository.save(entity);

        return toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public void delete(UUID id) {
        Reservation entity = findEntityInCurrentOrganization(id);
        User currentUser = findCurrentUser();

        entity.setStatus(ReservationStatus.DELETED);
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setDeletedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        reservationRepository.save(entity);
    }

    private ReservationResponse toResponse(Reservation reservation) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        return reservationMapper.toResponse(
                reservation,
                reservationGuestRepository.findByReservation_Id(reservation.getId()),
                reservationSupplyRepository.findByReservation_IdAndOrganization_IdOrderByCreatedAtAsc(
                        reservation.getId(),
                        organizationId
                )
        );
    }

    private Reservation findEntityInCurrentOrganization(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        return reservationRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
    }

    private User findCurrentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));
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

        for (ReservationGuestRequest guestRequest : guestRequests) {
            Guest guest = resolveGuest(guestRequest, organization, currentUser);

            ReservationGuest reservationGuest = new ReservationGuest();
            reservationGuest.setOrganization(organization);
            reservationGuest.setReservation(reservation);
            reservationGuest.setGuest(guest);
            reservationGuest.setPrimaryGuest(Boolean.TRUE.equals(guestRequest.primary()));

            reservationGuestRepository.save(reservationGuest);
        }
    }

    private void replaceSupplies(Reservation reservation, List<ReservationSupplyRequest> supplyRequests) {
        reservationSupplyRepository.deleteByReservation_Id(reservation.getId());
        reservationSupplyRepository.flush();

        if (supplyRequests == null || supplyRequests.isEmpty()) {
            return;
        }

        for (ReservationSupplyRequest supplyRequest : supplyRequests) {
            createSupplyEntity(reservation, supplyRequest);
        }
    }

    private ReservationSupply createSupplyEntity(Reservation reservation, ReservationSupplyRequest request) {
        UUID organizationId = reservation.getOrganization().getId();
        InventoryItem inventoryItem = resolveReservationInventoryItem(request.inventoryItemId(), organizationId);

        ReservationSupply entity = new ReservationSupply();
        entity.setOrganization(reservation.getOrganization());
        entity.setReservation(reservation);
        entity.setInventoryItem(inventoryItem);
        entity.setQuantity(request.quantity());
        entity.setUnit(resolveUnit(request.unit(), inventoryItem));
        entity.setItemNameSnapshot(inventoryItem.getName());
        entity.setInternalCodeSnapshot(inventoryItem.getInternalCode());
        entity.setBarcodeSnapshot(inventoryItem.getBarcode());
        entity.setNotes(request.notes());

        return reservationSupplyRepository.save(entity);
    }

    private InventoryItem resolveReservationInventoryItem(UUID inventoryItemId, UUID organizationId) {
        InventoryItem inventoryItem = inventoryItemRepository
                .findByIdAndOrganization_IdAndDeletedAtIsNull(inventoryItemId, organizationId)
                .orElseThrow(() -> new NotFoundException("Inventory item not found"));

        if (!Boolean.TRUE.equals(inventoryItem.getAvailableForReservations())) {
            throw new BadRequestException("Inventory item is not available for reservations");
        }

        return inventoryItem;
    }

    private String resolveUnit(String requestedUnit, InventoryItem inventoryItem) {
        if (requestedUnit != null && !requestedUnit.isBlank()) {
            return requestedUnit.trim();
        }

        return inventoryItem.getUnit();
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
