package com.tamias.scheduledmaintenance.controller;

import com.tamias.common.dto.PageResponse;
import com.tamias.scheduledmaintenance.dto.ScheduledMaintenanceRequest;
import com.tamias.scheduledmaintenance.dto.ScheduledMaintenanceRescheduleRequest;
import com.tamias.scheduledmaintenance.dto.ScheduledMaintenanceResponse;
import com.tamias.scheduledmaintenance.dto.ScheduledMaintenanceStatusChangeRequest;
import com.tamias.scheduledmaintenance.dto.ScheduledMaintenanceSummaryResponse;
import com.tamias.scheduledmaintenance.enums.ScheduledMaintenanceStatus;
import com.tamias.scheduledmaintenance.history.dto.ScheduledMaintenanceHistoryResponse;
import com.tamias.scheduledmaintenance.service.ScheduledMaintenanceService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{id}/history")
    public List<ScheduledMaintenanceHistoryResponse> findHistory(@PathVariable UUID id) {
        return scheduledMaintenanceService.findHistory(id);
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

    @PatchMapping("/{id}/reschedule")
    public ScheduledMaintenanceResponse reschedule(
            @PathVariable UUID id,
            @Valid @RequestBody ScheduledMaintenanceRescheduleRequest request
    ) {
        return scheduledMaintenanceService.reschedule(id, request);
    }

    @PatchMapping("/{id}/pause")
    public ScheduledMaintenanceResponse pause(
            @PathVariable UUID id,
            @RequestBody(required = false) ScheduledMaintenanceStatusChangeRequest request
    ) {
        return scheduledMaintenanceService.pause(id, request != null ? request.reason() : null);
    }

    @PatchMapping("/{id}/resume")
    public ScheduledMaintenanceResponse resume(
            @PathVariable UUID id,
            @RequestBody(required = false) ScheduledMaintenanceStatusChangeRequest request
    ) {
        return scheduledMaintenanceService.resume(id, request != null ? request.reason() : null);
    }

    @PatchMapping("/{id}/cancel")
    public ScheduledMaintenanceResponse cancel(
            @PathVariable UUID id,
            @RequestBody(required = false) ScheduledMaintenanceStatusChangeRequest request
    ) {
        return scheduledMaintenanceService.cancel(id, request != null ? request.reason() : null);
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
