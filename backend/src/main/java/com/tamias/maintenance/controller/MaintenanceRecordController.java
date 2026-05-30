package com.tamias.maintenance.controller;

import com.tamias.common.dto.PageResponse;
import com.tamias.maintenance.dto.MaintenanceRecordRequest;
import com.tamias.maintenance.dto.MaintenanceRecordResponse;
import com.tamias.maintenance.dto.MaintenanceRecordSummaryResponse;
import com.tamias.maintenance.enums.MaintenanceStatus;
import com.tamias.maintenance.service.MaintenanceRecordService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/maintenance-records")
public class MaintenanceRecordController {

    private final MaintenanceRecordService maintenanceRecordService;

    public MaintenanceRecordController(MaintenanceRecordService maintenanceRecordService) {
        this.maintenanceRecordService = maintenanceRecordService;
    }

    @GetMapping
    public PageResponse<MaintenanceRecordSummaryResponse> findAll(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) MaintenanceStatus status,
            Pageable pageable
    ) {
        return maintenanceRecordService.findAll(propertyId, status, pageable);
    }

    @GetMapping("/{id}")
    public MaintenanceRecordResponse findById(@PathVariable UUID id) {
        return maintenanceRecordService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceRecordResponse create(@Valid @RequestBody MaintenanceRecordRequest request) {
        return maintenanceRecordService.create(request);
    }

    @PutMapping("/{id}")
    public MaintenanceRecordResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody MaintenanceRecordRequest request
    ) {
        return maintenanceRecordService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        maintenanceRecordService.delete(id);
    }
}
