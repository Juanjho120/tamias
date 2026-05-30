package com.tamias.catalog.maintenancetype.controller;

import com.tamias.catalog.dto.MaintenanceTypeRequest;
import com.tamias.catalog.dto.MaintenanceTypeResponse;
import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.maintenancetype.service.MaintenanceTypeService;
import com.tamias.common.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalogs/maintenance-types")
public class MaintenanceTypeController {

    private final MaintenanceTypeService service;

    public MaintenanceTypeController(MaintenanceTypeService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<MaintenanceTypeResponse> findAll(
            @RequestParam(required = false) CatalogStatus status,
            Pageable pageable
    ) {
        return service.findAll(status, pageable);
    }

    @GetMapping("/{id}")
    public MaintenanceTypeResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceTypeResponse create(@Valid @RequestBody MaintenanceTypeRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public MaintenanceTypeResponse update(@PathVariable UUID id, @Valid @RequestBody MaintenanceTypeRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
