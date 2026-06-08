package com.tamias.task.mapper;

import com.tamias.catalog.tasktemplate.entity.TaskTemplate;
import com.tamias.task.dto.TaskItemRequest;
import com.tamias.task.dto.TaskItemResponse;
import com.tamias.task.dto.TaskItemUpdateRequest;
import com.tamias.task.dto.TaskListRequest;
import com.tamias.task.dto.TaskListResponse;
import com.tamias.task.dto.TaskListSummaryResponse;
import com.tamias.task.entity.TaskItem;
import com.tamias.task.entity.TaskList;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public void updateTaskList(TaskList entity, TaskListRequest request) {
        entity.setTitle(request.title());
        entity.setCreationDate(request.creationDate() != null ? request.creationDate() : LocalDate.now());
        entity.setDueDate(request.dueDate());
        entity.setStatus(request.status());
    }

    public void updateTaskItem(TaskItem entity, TaskItemRequest request, TaskTemplate taskTemplate) {
        entity.setTaskTemplate(taskTemplate);
        entity.setTaskName(request.taskName());
        entity.setResponsiblePerson(request.responsiblePerson());
        entity.setCompleted(Boolean.TRUE.equals(request.completed()));
        entity.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
    }

    public void updateTaskItem(TaskItem entity, TaskItemUpdateRequest request, TaskTemplate taskTemplate) {
        entity.setTaskTemplate(taskTemplate);
        entity.setTaskName(request.taskName());
        entity.setResponsiblePerson(request.responsiblePerson());
        entity.setCompleted(Boolean.TRUE.equals(request.completed()));
        entity.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
    }

    public TaskListSummaryResponse toSummaryResponse(
            TaskList entity,
            long totalItems,
            long completedItems
    ) {
        return new TaskListSummaryResponse(
                entity.getId(),
                entity.getProperty().getId(),
                entity.getProperty().getName(),
                entity.getReservation() != null ? entity.getReservation().getId() : null,
                entity.getMaintenanceRecord() != null ? entity.getMaintenanceRecord().getId() : null,
                entity.getTitle(),
                entity.getCreationDate(),
                entity.getDueDate(),
                entity.getStatus(),
                totalItems,
                completedItems,
                entity.getCreatedAt()
        );
    }

    public TaskListResponse toResponse(TaskList entity, List<TaskItem> items) {
        return new TaskListResponse(
                entity.getId(),
                entity.getProperty().getId(),
                entity.getProperty().getName(),
                entity.getReservation() != null ? entity.getReservation().getId() : null,
                entity.getMaintenanceRecord() != null ? entity.getMaintenanceRecord().getId() : null,
                entity.getTitle(),
                entity.getCreationDate(),
                entity.getDueDate(),
                entity.getStatus(),
                items.stream().map(this::toItemResponse).toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public TaskItemResponse toItemResponse(TaskItem entity) {
        return new TaskItemResponse(
                entity.getId(),
                entity.getTaskTemplate() != null ? entity.getTaskTemplate().getId() : null,
                entity.getTaskTemplate() != null ? entity.getTaskTemplate().getName() : null,
                entity.getTaskName(),
                entity.getResponsiblePerson(),
                entity.getCompleted(),
                entity.getCompletionDate(),
                entity.getSortOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
