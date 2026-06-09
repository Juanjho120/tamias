package com.tamias.scheduledmaintenance.service;

import com.tamias.catalog.maintenancecategory.repository.MaintenanceCategoryRepository;
import com.tamias.catalog.maintenanceperson.repository.MaintenancePersonRepository;
import com.tamias.catalog.maintenancetype.repository.MaintenanceTypeRepository;
import com.tamias.common.dto.PageResponse;
import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.maintenance.entity.MaintenanceRecord;
import com.tamias.maintenance.enums.MaintenanceStatus;
import com.tamias.maintenance.repository.MaintenanceRecordRepository;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.property.repository.PropertyRepository;
import com.tamias.scheduledmaintenance.dto.ScheduledMaintenanceRequest;
import com.tamias.scheduledmaintenance.dto.ScheduledMaintenanceRescheduleRequest;
import com.tamias.scheduledmaintenance.dto.ScheduledMaintenanceResponse;
import com.tamias.scheduledmaintenance.dto.ScheduledMaintenanceSummaryResponse;
import com.tamias.scheduledmaintenance.entity.ScheduledMaintenance;
import com.tamias.scheduledmaintenance.enums.ScheduledMaintenanceFrequency;
import com.tamias.scheduledmaintenance.enums.ScheduledMaintenanceStatus;
import com.tamias.scheduledmaintenance.history.dto.ScheduledMaintenanceHistoryResponse;
import com.tamias.scheduledmaintenance.history.service.ScheduledMaintenanceHistoryService;
import com.tamias.scheduledmaintenance.mapper.ScheduledMaintenanceMapper;
import com.tamias.scheduledmaintenance.repository.ScheduledMaintenanceRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.entity.User;
import com.tamias.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduledMaintenanceService {

    private final ScheduledMaintenanceRepository scheduledMaintenanceRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final OrganizationRepository organizationRepository;
    private final PropertyRepository propertyRepository;
    private final MaintenanceCategoryRepository maintenanceCategoryRepository;
    private final MaintenanceTypeRepository maintenanceTypeRepository;
    private final MaintenancePersonRepository maintenancePersonRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ScheduledMaintenanceMapper scheduledMaintenanceMapper;
    private final ScheduledMaintenanceHistoryService historyService;

    public ScheduledMaintenanceService(
            ScheduledMaintenanceRepository scheduledMaintenanceRepository,
            MaintenanceRecordRepository maintenanceRecordRepository,
            OrganizationRepository organizationRepository,
            PropertyRepository propertyRepository,
            MaintenanceCategoryRepository maintenanceCategoryRepository,
            MaintenanceTypeRepository maintenanceTypeRepository,
            MaintenancePersonRepository maintenancePersonRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            ScheduledMaintenanceMapper scheduledMaintenanceMapper,
            ScheduledMaintenanceHistoryService historyService
    ) {
        this.scheduledMaintenanceRepository = scheduledMaintenanceRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.organizationRepository = organizationRepository;
        this.propertyRepository = propertyRepository;
        this.maintenanceCategoryRepository = maintenanceCategoryRepository;
        this.maintenanceTypeRepository = maintenanceTypeRepository;
        this.maintenancePersonRepository = maintenancePersonRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.scheduledMaintenanceMapper = scheduledMaintenanceMapper;
        this.historyService = historyService;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public PageResponse<ScheduledMaintenanceSummaryResponse> findAll(
            UUID propertyId,
            ScheduledMaintenanceStatus status,
            Pageable pageable
    ) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        Page<ScheduledMaintenance> page;

        if (propertyId == null && status == null) {
            page = scheduledMaintenanceRepository.findByOrganization_IdAndDeletedAtIsNull(
                    organizationId,
                    pageable
            );
        } else if (propertyId != null && status == null) {
            page = scheduledMaintenanceRepository.findByOrganization_IdAndProperty_IdAndDeletedAtIsNull(
                    organizationId,
                    propertyId,
                    pageable
            );
        } else if (propertyId == null) {
            page = scheduledMaintenanceRepository.findByOrganization_IdAndStatusAndDeletedAtIsNull(
                    organizationId,
                    status,
                    pageable
            );
        } else {
            page = scheduledMaintenanceRepository.findByOrganization_IdAndProperty_IdAndStatusAndDeletedAtIsNull(
                    organizationId,
                    propertyId,
                    status,
                    pageable
            );
        }

        return PageResponse.from(page.map(scheduledMaintenanceMapper::toSummaryResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public PageResponse<ScheduledMaintenanceSummaryResponse> findDue(LocalDate dueDate, Pageable pageable) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        LocalDate effectiveDueDate = dueDate != null ? dueDate : LocalDate.now();

        var page = scheduledMaintenanceRepository
                .findByOrganization_IdAndNextDueDateLessThanEqualAndStatusAndDeletedAtIsNull(
                        organizationId,
                        effectiveDueDate,
                        ScheduledMaintenanceStatus.ACTIVE,
                        pageable
                )
                .map(scheduledMaintenanceMapper::toSummaryResponse);

        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public ScheduledMaintenanceResponse findById(UUID id) {
        return scheduledMaintenanceMapper.toResponse(findEntityInCurrentOrganization(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public List<ScheduledMaintenanceHistoryResponse> findHistory(UUID id) {
        ScheduledMaintenance scheduledMaintenance = findEntityInCurrentOrganization(id);
        return historyService.findHistory(scheduledMaintenance);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public ScheduledMaintenanceResponse create(ScheduledMaintenanceRequest request) {
        validateDates(request);

        UUID organizationId = currentUserService.getCurrentOrganizationId();

        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        var property = propertyRepository
                .findByIdAndOrganization_IdAndDeletedAtIsNull(request.propertyId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Property not found"));

        User currentUser = getCurrentUser();

        ScheduledMaintenance entity = new ScheduledMaintenance();
        entity.setOrganization(organization);
        entity.setProperty(property);
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        scheduledMaintenanceMapper.updateEntity(entity, request);
        setOptionalRelations(entity, request, organizationId);

        ScheduledMaintenance saved = scheduledMaintenanceRepository.save(entity);

        historyService.recordChange(
                saved,
                null,
                saved.getStatus(),
                null,
                saved.getNextDueDate(),
                "Scheduled maintenance created",
                currentUser
        );

        return scheduledMaintenanceMapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public ScheduledMaintenanceResponse update(UUID id, ScheduledMaintenanceRequest request) {
        validateDates(request);

        UUID organizationId = currentUserService.getCurrentOrganizationId();

        ScheduledMaintenance entity = findEntityInCurrentOrganization(id);
        ScheduledMaintenanceStatus previousStatus = entity.getStatus();
        LocalDate previousNextDueDate = entity.getNextDueDate();

        var property = propertyRepository
                .findByIdAndOrganization_IdAndDeletedAtIsNull(request.propertyId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Property not found"));

        User currentUser = getCurrentUser();

        entity.setProperty(property);
        entity.setUpdatedBy(currentUser);

        scheduledMaintenanceMapper.updateEntity(entity, request);
        setOptionalRelations(entity, request, organizationId);

        ScheduledMaintenance saved = scheduledMaintenanceRepository.save(entity);

        recordIfChanged(
                saved,
                previousStatus,
                saved.getStatus(),
                previousNextDueDate,
                saved.getNextDueDate(),
                "Scheduled maintenance updated",
                currentUser
        );

        return scheduledMaintenanceMapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public ScheduledMaintenanceResponse reschedule(UUID id, ScheduledMaintenanceRescheduleRequest request) {
        ScheduledMaintenance entity = findEntityInCurrentOrganization(id);

        if (entity.getStatus() == ScheduledMaintenanceStatus.COMPLETED
                || entity.getStatus() == ScheduledMaintenanceStatus.CANCELLED
                || entity.getStatus() == ScheduledMaintenanceStatus.DELETED) {
            throw new BadRequestException("Only active or paused scheduled maintenance can be rescheduled");
        }

        if (request.nextDueDate().isBefore(entity.getStartDate())) {
            throw new BadRequestException("Next due date cannot be before start date");
        }

        if (entity.getEndDate() != null && request.nextDueDate().isAfter(entity.getEndDate())) {
            throw new BadRequestException("Next due date cannot be after end date");
        }

        User currentUser = getCurrentUser();

        ScheduledMaintenanceStatus previousStatus = entity.getStatus();
        LocalDate previousNextDueDate = entity.getNextDueDate();

        entity.setNextDueDate(request.nextDueDate());
        entity.setStatus(ScheduledMaintenanceStatus.ACTIVE);
        entity.setUpdatedBy(currentUser);

        ScheduledMaintenance saved = scheduledMaintenanceRepository.save(entity);

        historyService.recordChange(
                saved,
                previousStatus,
                saved.getStatus(),
                previousNextDueDate,
                saved.getNextDueDate(),
                request.reason(),
                currentUser
        );

        return scheduledMaintenanceMapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public ScheduledMaintenanceResponse pause(UUID id, String reason) {
        return changeStatus(id, ScheduledMaintenanceStatus.PAUSED, reason);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public ScheduledMaintenanceResponse resume(UUID id, String reason) {
        return changeStatus(id, ScheduledMaintenanceStatus.ACTIVE, reason);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public ScheduledMaintenanceResponse cancel(UUID id, String reason) {
        return changeStatus(id, ScheduledMaintenanceStatus.CANCELLED, reason);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public ScheduledMaintenanceResponse generateMaintenanceRecord(UUID id) {
        ScheduledMaintenance schedule = findEntityInCurrentOrganization(id);

        if (schedule.getStatus() != ScheduledMaintenanceStatus.ACTIVE) {
            throw new BadRequestException("Only active scheduled maintenance can generate maintenance records");
        }

        User currentUser = getCurrentUser();

        MaintenanceRecord record = new MaintenanceRecord();
        record.setOrganization(schedule.getOrganization());
        record.setProperty(schedule.getProperty());
        record.setMaintenanceCategory(schedule.getMaintenanceCategory());
        record.setMaintenanceType(schedule.getMaintenanceType());
        record.setMaintenancePerson(schedule.getMaintenancePerson());
        record.setTitle(schedule.getTitle());
        record.setDescription(schedule.getDescription());
        record.setScheduledAt(schedule.getNextDueDate().atStartOfDay().atOffset(OffsetDateTime.now().getOffset()));
        record.setCost(schedule.getEstimatedCost());
        record.setStatus(MaintenanceStatus.PENDING);
        record.setCreatedBy(currentUser);
        record.setUpdatedBy(currentUser);

        maintenanceRecordRepository.save(record);

        ScheduledMaintenanceStatus previousStatus = schedule.getStatus();
        LocalDate previousNextDueDate = schedule.getNextDueDate();

        schedule.setLastGeneratedAt(OffsetDateTime.now());
        schedule.setNextDueDate(calculateNextDueDate(
                schedule.getNextDueDate(),
                schedule.getFrequency(),
                schedule.getIntervalValue()
        ));
        schedule.setUpdatedBy(currentUser);

        if (schedule.getEndDate() != null && schedule.getNextDueDate().isAfter(schedule.getEndDate())) {
            schedule.setStatus(ScheduledMaintenanceStatus.COMPLETED);
        }

        ScheduledMaintenance saved = scheduledMaintenanceRepository.save(schedule);

        historyService.recordChange(
                saved,
                previousStatus,
                saved.getStatus(),
                previousNextDueDate,
                saved.getNextDueDate(),
                "Maintenance record generated from schedule",
                currentUser
        );

        return scheduledMaintenanceMapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public void delete(UUID id) {
        ScheduledMaintenance entity = findEntityInCurrentOrganization(id);
        User currentUser = getCurrentUser();

        ScheduledMaintenanceStatus previousStatus = entity.getStatus();
        LocalDate previousNextDueDate = entity.getNextDueDate();

        entity.setStatus(ScheduledMaintenanceStatus.DELETED);
        entity.setDeletedAt(OffsetDateTime.now());
        entity.setDeletedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        ScheduledMaintenance saved = scheduledMaintenanceRepository.save(entity);

        historyService.recordChange(
                saved,
                previousStatus,
                ScheduledMaintenanceStatus.DELETED,
                previousNextDueDate,
                saved.getNextDueDate(),
                "Scheduled maintenance deleted",
                currentUser
        );
    }

    private ScheduledMaintenanceResponse changeStatus(
            UUID id,
            ScheduledMaintenanceStatus newStatus,
            String reason
    ) {
        ScheduledMaintenance entity = findEntityInCurrentOrganization(id);

        if (entity.getStatus() == ScheduledMaintenanceStatus.DELETED) {
            throw new BadRequestException("Deleted scheduled maintenance cannot change status");
        }

        if (entity.getStatus() == ScheduledMaintenanceStatus.COMPLETED) {
            throw new BadRequestException("Completed scheduled maintenance cannot change status");
        }

        if (entity.getStatus() == ScheduledMaintenanceStatus.CANCELLED
                && newStatus != ScheduledMaintenanceStatus.ACTIVE) {
            throw new BadRequestException("Cancelled scheduled maintenance can only be resumed to active");
        }

        User currentUser = getCurrentUser();

        ScheduledMaintenanceStatus previousStatus = entity.getStatus();
        LocalDate previousNextDueDate = entity.getNextDueDate();

        entity.setStatus(newStatus);
        entity.setUpdatedBy(currentUser);

        ScheduledMaintenance saved = scheduledMaintenanceRepository.save(entity);

        recordIfChanged(
                saved,
                previousStatus,
                saved.getStatus(),
                previousNextDueDate,
                saved.getNextDueDate(),
                reason,
                currentUser
        );

        return scheduledMaintenanceMapper.toResponse(saved);
    }

    private void recordIfChanged(
            ScheduledMaintenance scheduledMaintenance,
            ScheduledMaintenanceStatus previousStatus,
            ScheduledMaintenanceStatus newStatus,
            LocalDate previousNextDueDate,
            LocalDate newNextDueDate,
            String reason,
            User currentUser
    ) {
        boolean statusChanged = previousStatus != newStatus;
        boolean dateChanged = previousNextDueDate == null
                ? newNextDueDate != null
                : !previousNextDueDate.equals(newNextDueDate);

        if (!statusChanged && !dateChanged) {
            return;
        }

        historyService.recordChange(
                scheduledMaintenance,
                previousStatus,
                newStatus,
                previousNextDueDate,
                newNextDueDate,
                reason,
                currentUser
        );
    }

    private ScheduledMaintenance findEntityInCurrentOrganization(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        return scheduledMaintenanceRepository
                .findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new NotFoundException("Scheduled maintenance not found"));
    }

    private void setOptionalRelations(
            ScheduledMaintenance entity,
            ScheduledMaintenanceRequest request,
            UUID organizationId
    ) {
        if (request.maintenanceCategoryId() == null) {
            entity.setMaintenanceCategory(null);
        } else {
            var category = maintenanceCategoryRepository
                    .findByIdAndOrganization_IdAndDeletedAtIsNull(request.maintenanceCategoryId(), organizationId)
                    .orElseThrow(() -> new NotFoundException("Maintenance category not found"));

            entity.setMaintenanceCategory(category);
        }

        if (request.maintenanceTypeId() == null) {
            entity.setMaintenanceType(null);
        } else {
            var type = maintenanceTypeRepository
                    .findByIdAndOrganization_IdAndDeletedAtIsNull(request.maintenanceTypeId(), organizationId)
                    .orElseThrow(() -> new NotFoundException("Maintenance type not found"));

            entity.setMaintenanceType(type);
        }

        if (request.maintenancePersonId() == null) {
            entity.setMaintenancePerson(null);
        } else {
            var person = maintenancePersonRepository
                    .findByIdAndOrganization_IdAndDeletedAtIsNull(request.maintenancePersonId(), organizationId)
                    .orElseThrow(() -> new NotFoundException("Maintenance person not found"));

            entity.setMaintenancePerson(person);
        }
    }

    private void validateDates(ScheduledMaintenanceRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        if (request.nextDueDate() != null && request.nextDueDate().isBefore(request.startDate())) {
            throw new BadRequestException("Next due date cannot be before start date");
        }
    }

    private LocalDate calculateNextDueDate(
            LocalDate currentDueDate,
            ScheduledMaintenanceFrequency frequency,
            Integer intervalValue
    ) {
        int interval = intervalValue != null ? intervalValue : 1;

        return switch (frequency) {
            case DAILY -> currentDueDate.plusDays(interval);
            case WEEKLY -> currentDueDate.plusWeeks(interval);
            case MONTHLY -> currentDueDate.plusMonths(interval);
            case YEARLY -> currentDueDate.plusYears(interval);
        };
    }

    private User getCurrentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
