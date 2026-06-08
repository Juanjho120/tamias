package com.tamias.task.controller;

import com.tamias.common.dto.PageResponse;
import com.tamias.task.dto.TaskItemCompletionRequest;
import com.tamias.task.dto.TaskItemRequest;
import com.tamias.task.dto.TaskItemResponse;
import com.tamias.task.dto.TaskItemUpdateRequest;
import com.tamias.task.dto.TaskListRequest;
import com.tamias.task.dto.TaskListResponse;
import com.tamias.task.dto.TaskListSummaryResponse;
import com.tamias.task.enums.TaskListStatus;
import com.tamias.task.service.TaskListService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/task-lists")
public class TaskListController {

    private final TaskListService taskListService;

    public TaskListController(TaskListService taskListService) {
        this.taskListService = taskListService;
    }

    @GetMapping
    public PageResponse<TaskListSummaryResponse> findAll(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) UUID reservationId,
            @RequestParam(required = false) UUID maintenanceRecordId,
            @RequestParam(required = false) TaskListStatus status,
            Pageable pageable
    ) {
        return taskListService.findAll(propertyId, reservationId, maintenanceRecordId, status, pageable);
    }

    @GetMapping("/{id}")
    public TaskListResponse findById(@PathVariable UUID id) {
        return taskListService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskListResponse create(@Valid @RequestBody TaskListRequest request) {
        return taskListService.create(request);
    }

    @PutMapping("/{id}")
    public TaskListResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody TaskListRequest request
    ) {
        return taskListService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        taskListService.delete(id);
    }

    @PostMapping("/{taskListId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskItemResponse createItem(
            @PathVariable UUID taskListId,
            @Valid @RequestBody TaskItemRequest request
    ) {
        return taskListService.createItem(taskListId, request);
    }

    @PutMapping("/{taskListId}/items/{itemId}")
    public TaskItemResponse updateItem(
            @PathVariable UUID taskListId,
            @PathVariable UUID itemId,
            @Valid @RequestBody TaskItemUpdateRequest request
    ) {
        return taskListService.updateItem(taskListId, itemId, request);
    }

    @PatchMapping("/{taskListId}/items/{itemId}/completion")
    public TaskItemResponse updateItemCompletion(
            @PathVariable UUID taskListId,
            @PathVariable UUID itemId,
            @Valid @RequestBody TaskItemCompletionRequest request
    ) {
        return taskListService.updateItemCompletion(taskListId, itemId, request);
    }

    @DeleteMapping("/{taskListId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(
            @PathVariable UUID taskListId,
            @PathVariable UUID itemId
    ) {
        taskListService.deleteItem(taskListId, itemId);
    }
}
