package com.tamias.scheduledmaintenance.controller;

import com.tamias.common.dto.PageResponse;
import com.tamias.scheduledmaintenance.dto.ScheduledMaintenanceRequest;
import com.tamias.scheduledmaintenance.dto.ScheduledMaintenanceResponse;
import com.tamias.scheduledmaintenance.dto.ScheduledMaintenanceSummaryResponse;
import com.tamias.scheduledmaintenance.enums.ScheduledMaintenanceStatus;
import com.tamias.scheduledmaintenance.service.ScheduledMaintenanceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/scheduled-maintenance")
public class ScheduledMaintenanceController {

    private final ScheduledMaintenanceService scheduledMaintenanceService;

    public ScheduledMaintenanceController(ScheduledMaintenanceService scheduledMaintenanceService) {
        this.scheduledMaintenanceService = scheduledMaintenanceService;
    }

    @GetMapping
    public PageResponse<ScheduledMaintenanceSummaryResponse> findAll(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) ScheduledMaintenanceStatus status,
            Pageable pageable
    ) {
        return scheduledMaintenanceService.findAll(propertyId, status, pageable);
    }

    @GetMapping("/due")
    public PageResponse<ScheduledMaintenanceSummaryResponse> findDue(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueDate,
            Pageable pageable
    ) {
        return scheduledMaintenanceService.findDue(dueDate, pageable);
    }

    @GetMapping("/{id}")
    public ScheduledMaintenanceResponse findById(@PathVariable UUID id) {
        return scheduledMaintenanceService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduledMaintenanceResponse create(@Valid @RequestBody ScheduledMaintenanceRequest request) {
        return scheduledMaintenanceService.create(request);
    }

    @PutMapping("/{id}")
    public ScheduledMaintenanceResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody ScheduledMaintenanceRequest request
    ) {
        return scheduledMaintenanceService.update(id, request);
    }

    @PostMapping("/{id}/generate-record")
    public ScheduledMaintenanceResponse generateMaintenanceRecord(@PathVariable UUID id) {
        return scheduledMaintenanceService.generateMaintenanceRecord(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        scheduledMaintenanceService.delete(id);
    }
}
