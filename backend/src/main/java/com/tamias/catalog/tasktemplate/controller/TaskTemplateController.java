package com.tamias.catalog.tasktemplate.controller;

import com.tamias.catalog.dto.TaskTemplateRequest;
import com.tamias.catalog.dto.TaskTemplateResponse;
import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.tasktemplate.service.TaskTemplateService;
import com.tamias.common.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalogs/task-templates")
public class TaskTemplateController {

    private final TaskTemplateService service;

    public TaskTemplateController(TaskTemplateService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<TaskTemplateResponse> findAll(
            @RequestParam(required = false) CatalogStatus status,
            Pageable pageable
    ) {
        return service.findAll(status, pageable);
    }

    @GetMapping("/{id}")
    public TaskTemplateResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskTemplateResponse create(@Valid @RequestBody TaskTemplateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public TaskTemplateResponse update(@PathVariable UUID id, @Valid @RequestBody TaskTemplateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
