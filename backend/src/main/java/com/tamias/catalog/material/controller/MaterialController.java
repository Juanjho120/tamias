package com.tamias.catalog.material.controller;

import com.tamias.catalog.dto.MaterialRequest;
import com.tamias.catalog.dto.MaterialResponse;
import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.material.service.MaterialService;
import com.tamias.common.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalogs/materials")
public class MaterialController {

    private final MaterialService service;

    public MaterialController(MaterialService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<MaterialResponse> findAll(
            @RequestParam(required = false) CatalogStatus status,
            Pageable pageable
    ) {
        return service.findAll(status, pageable);
    }

    @GetMapping("/{id}")
    public MaterialResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialResponse create(@Valid @RequestBody MaterialRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public MaterialResponse update(@PathVariable UUID id, @Valid @RequestBody MaterialRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
