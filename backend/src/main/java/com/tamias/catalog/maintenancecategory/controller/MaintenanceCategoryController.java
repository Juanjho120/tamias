package com.tamias.catalog.maintenancecategory.controller;

import com.tamias.catalog.dto.CatalogRequest;
import com.tamias.catalog.dto.CatalogResponse;
import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.maintenancecategory.service.MaintenanceCategoryService;
import com.tamias.common.dto.PageResponse;
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
@RequestMapping("/api/v1/catalogs/maintenance-categories")
public class MaintenanceCategoryController {

    private final MaintenanceCategoryService service;

    public MaintenanceCategoryController(MaintenanceCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<CatalogResponse> findAll(
            @RequestParam(required = false) CatalogStatus status,
            Pageable pageable
    ) {
        return service.findAll(status, pageable);
    }

    @GetMapping("/{id}")
    public CatalogResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogResponse create(@Valid @RequestBody CatalogRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CatalogResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody CatalogRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
