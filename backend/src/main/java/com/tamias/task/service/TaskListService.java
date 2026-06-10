package com.tamias.task.service;

import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.tasktemplate.entity.TaskTemplate;
import com.tamias.catalog.tasktemplate.repository.TaskTemplateRepository;
import com.tamias.common.dto.PageResponse;
import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.maintenance.repository.MaintenanceRecordRepository;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.property.repository.PropertyRepository;
import com.tamias.reservation.repository.ReservationRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.task.dto.TaskItemCompletionRequest;
import com.tamias.task.dto.TaskItemRequest;
import com.tamias.task.dto.TaskItemResponse;
import com.tamias.task.dto.TaskItemUpdateRequest;
import com.tamias.task.dto.TaskListRequest;
import com.tamias.task.dto.TaskListResponse;
import com.tamias.task.dto.TaskListSummaryResponse;
import com.tamias.task.entity.TaskItem;
import com.tamias.task.entity.TaskList;
import com.tamias.task.enums.TaskListStatus;
import com.tamias.task.mapper.TaskMapper;
import com.tamias.task.repository.TaskItemRepository;
import com.tamias.task.repository.TaskListRepository;
import com.tamias.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskListService {
    private final TaskListRepository taskListRepository;
    private final TaskItemRepository taskItemRepository;
    private final OrganizationRepository organizationRepository;
    private final PropertyRepository propertyRepository;
    private final ReservationRepository reservationRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final TaskTemplateRepository taskTemplateRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final TaskMapper taskMapper;

    public TaskListService(
        TaskListRepository taskListRepository,
        TaskItemRepository taskItemRepository,
        OrganizationRepository organizationRepository,
        PropertyRepository propertyRepository,
        ReservationRepository reservationRepository,
        MaintenanceRecordRepository maintenanceRecordRepository,
        TaskTemplateRepository taskTemplateRepository,
        UserRepository userRepository,
        CurrentUserService currentUserService,
        TaskMapper taskMapper
    ) {
        this.taskListRepository = taskListRepository;
        this.taskItemRepository = taskItemRepository;
        this.organizationRepository = organizationRepository;
        this.propertyRepository = propertyRepository;
        this.reservationRepository = reservationRepository;
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.taskTemplateRepository = taskTemplateRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.taskMapper = taskMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public PageResponse<TaskListSummaryResponse> findAll(
        UUID propertyId,
        UUID reservationId,
        UUID maintenanceRecordId,
        TaskListStatus status,
        Pageable pageable
    ) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        Page<TaskList> page;

        if (reservationId != null) {
            page = taskListRepository.findByOrganization_IdAndReservation_IdAndDeletedAtIsNull(
                organizationId,
                reservationId,
                pageable
            );
        } else if (maintenanceRecordId != null) {
            page = taskListRepository.findByOrganization_IdAndMaintenanceRecord_IdAndDeletedAtIsNull(
                organizationId,
                maintenanceRecordId,
                pageable
            );
        } else if (propertyId == null && status == null) {
            page = taskListRepository.findByOrganization_IdAndDeletedAtIsNull(organizationId, pageable);
        } else if (propertyId != null && status == null) {
            page = taskListRepository.findByOrganization_IdAndProperty_IdAndDeletedAtIsNull(
                organizationId,
                propertyId,
                pageable
            );
        } else if (propertyId == null) {
            page = taskListRepository.findByOrganization_IdAndStatusAndDeletedAtIsNull(
                organizationId,
                status,
                pageable
            );
        } else {
            page = taskListRepository.findByOrganization_IdAndProperty_IdAndStatusAndDeletedAtIsNull(
                organizationId,
                propertyId,
                status,
                pageable
            );
        }

        return PageResponse.from(page.map(this::toSummaryResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public TaskListResponse findById(UUID id) {
        TaskList taskList = findTaskList(id);

        return taskMapper.toResponse(
            taskList,
            taskItemRepository.findByTaskList_IdOrderBySortOrderAscCreatedAtAsc(taskList.getId())
        );
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public TaskListResponse create(TaskListRequest request) {
        validateWritableStatus(request.status());

        UUID organizationId = currentUserService.getCurrentOrganizationId();

        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));

        var property = propertyRepository
            .findByIdAndOrganization_IdAndDeletedAtIsNull(request.propertyId(), organizationId)
            .orElseThrow(() -> new NotFoundException("Property not found"));

        var currentUser = userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        TaskList taskList = new TaskList();
        taskList.setOrganization(organization);
        taskList.setProperty(property);
        taskList.setCreatedBy(currentUser);
        taskList.setUpdatedBy(currentUser);

        taskMapper.updateTaskList(taskList, request);
        setOptionalRelations(taskList, request, organizationId);

        TaskList saved = taskListRepository.save(taskList);

        if (request.items() != null) {
            for (TaskItemRequest itemRequest : request.items()) {
                createItemEntity(saved, itemRequest, organizationId);
            }
        }

        return findById(saved.getId());
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public TaskListResponse update(UUID id, TaskListRequest request) {
        validateWritableStatus(request.status());

        UUID organizationId = currentUserService.getCurrentOrganizationId();
        TaskList taskList = findTaskList(id);

        var property = propertyRepository
            .findByIdAndOrganization_IdAndDeletedAtIsNull(request.propertyId(), organizationId)
            .orElseThrow(() -> new NotFoundException("Property not found"));

        var currentUser = userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        taskList.setProperty(property);
        taskList.setUpdatedBy(currentUser);

        taskMapper.updateTaskList(taskList, request);
        setOptionalRelations(taskList, request, organizationId);

        TaskList saved = taskListRepository.save(taskList);

        return findById(saved.getId());
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public void delete(UUID id) {
        TaskList taskList = findTaskList(id);

        var currentUser = userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        taskList.setStatus(TaskListStatus.DELETED);
        taskList.setDeletedAt(OffsetDateTime.now());
        taskList.setDeletedBy(currentUser);
        taskList.setUpdatedBy(currentUser);

        taskListRepository.save(taskList);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public TaskItemResponse createItem(UUID taskListId, TaskItemRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        TaskList taskList = findTaskList(taskListId);

        TaskItem saved = createItemEntity(taskList, request, organizationId);

        return taskMapper.toItemResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public TaskItemResponse updateItem(UUID taskListId, UUID itemId, TaskItemUpdateRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        findTaskList(taskListId);

        TaskItem taskItem = taskItemRepository
            .findByIdAndTaskList_IdAndOrganization_Id(itemId, taskListId, organizationId)
            .orElseThrow(() -> new NotFoundException("Task item not found"));

        TaskTemplate taskTemplate = resolveTaskTemplate(request.taskTemplateId(), organizationId);

        taskMapper.updateTaskItem(taskItem, request, taskTemplate);
        applyCompletionDate(taskItem);

        return taskMapper.toItemResponse(taskItemRepository.save(taskItem));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public TaskItemResponse updateItemCompletion(
        UUID taskListId,
        UUID itemId,
        TaskItemCompletionRequest request
    ) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        findTaskList(taskListId);

        TaskItem taskItem = taskItemRepository
            .findByIdAndTaskList_IdAndOrganization_Id(itemId, taskListId, organizationId)
            .orElseThrow(() -> new NotFoundException("Task item not found"));

        taskItem.setCompleted(request.completed());
        applyCompletionDate(taskItem);

        return taskMapper.toItemResponse(taskItemRepository.save(taskItem));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public void deleteItem(UUID taskListId, UUID itemId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        findTaskList(taskListId);

        TaskItem taskItem = taskItemRepository
            .findByIdAndTaskList_IdAndOrganization_Id(itemId, taskListId, organizationId)
            .orElseThrow(() -> new NotFoundException("Task item not found"));

        taskItemRepository.delete(taskItem);
    }

    private TaskList findTaskList(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        return taskListRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
            .orElseThrow(() -> new NotFoundException("Task list not found"));
    }

    private TaskListSummaryResponse toSummaryResponse(TaskList taskList) {
        return taskMapper.toSummaryResponse(
            taskList,
            taskItemRepository.countByTaskList_Id(taskList.getId()),
            taskItemRepository.countByTaskList_IdAndCompleted(taskList.getId(), true)
        );
    }

    private TaskItem createItemEntity(TaskList taskList, TaskItemRequest request, UUID organizationId) {
        TaskTemplate taskTemplate = resolveTaskTemplate(request.taskTemplateId(), organizationId);

        TaskItem taskItem = new TaskItem();
        taskItem.setOrganization(taskList.getOrganization());
        taskItem.setTaskList(taskList);

        taskMapper.updateTaskItem(taskItem, request, taskTemplate);
        applyCompletionDate(taskItem);

        return taskItemRepository.save(taskItem);
    }

    private TaskTemplate resolveTaskTemplate(UUID taskTemplateId, UUID organizationId) {
        if (taskTemplateId == null) {
            return null;
        }

        TaskTemplate taskTemplate = taskTemplateRepository
            .findByIdAndOrganization_IdAndDeletedAtIsNull(taskTemplateId, organizationId)
            .orElseThrow(() -> new NotFoundException("Task template not found"));

        if (taskTemplate.getStatus() != CatalogStatus.ACTIVE) {
            throw new BadRequestException("Task template is not active");
        }

        return taskTemplate;
    }

    private void setOptionalRelations(TaskList taskList, TaskListRequest request, UUID organizationId) {
        if (request.reservationId() != null && request.maintenanceRecordId() != null) {
            throw new BadRequestException("Task list cannot be linked to both reservation and maintenance record");
        }

        if (request.reservationId() == null) {
            taskList.setReservation(null);
        } else {
            var reservation = reservationRepository
                .findByIdAndOrganization_IdAndDeletedAtIsNull(request.reservationId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));

            taskList.setReservation(reservation);
        }

        if (request.maintenanceRecordId() == null) {
            taskList.setMaintenanceRecord(null);
        } else {
            var maintenanceRecord = maintenanceRecordRepository
                .findByIdAndOrganization_IdAndDeletedAtIsNull(request.maintenanceRecordId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Maintenance record not found"));

            taskList.setMaintenanceRecord(maintenanceRecord);
        }
    }

    private void applyCompletionDate(TaskItem taskItem) {
        if (Boolean.TRUE.equals(taskItem.getCompleted()) && taskItem.getCompletionDate() == null) {
            taskItem.setCompletionDate(OffsetDateTime.now());
        }

        if (!Boolean.TRUE.equals(taskItem.getCompleted())) {
            taskItem.setCompletionDate(null);
        }
    }

    private void validateWritableStatus(TaskListStatus status) {
        if (status == TaskListStatus.DELETED) {
            throw new BadRequestException("Use the delete endpoint to delete a task list");
        }
    }
}
