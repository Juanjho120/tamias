package com.tamias.catalog.maintenanceperson.controller;

import com.tamias.catalog.dto.MaintenancePersonRequest;
import com.tamias.catalog.dto.MaintenancePersonResponse;
import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.maintenanceperson.service.MaintenancePersonService;
import com.tamias.common.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalogs/maintenance-people")
public class MaintenancePersonController {

    private final MaintenancePersonService service;

    public MaintenancePersonController(MaintenancePersonService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<MaintenancePersonResponse> findAll(
            @RequestParam(required = false) CatalogStatus status,
            Pageable pageable
    ) {
        return service.findAll(status, pageable);
    }

    @GetMapping("/{id}")
    public MaintenancePersonResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenancePersonResponse create(@Valid @RequestBody MaintenancePersonRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public MaintenancePersonResponse update(@PathVariable UUID id, @Valid @RequestBody MaintenancePersonRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
