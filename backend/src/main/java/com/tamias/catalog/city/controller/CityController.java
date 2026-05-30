package com.tamias.catalog.city.controller;

import com.tamias.catalog.city.service.CityService;
import com.tamias.catalog.dto.CityRequest;
import com.tamias.catalog.dto.CityResponse;
import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.common.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalogs/cities")
public class CityController {

    private final CityService service;

    public CityController(CityService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<CityResponse> findAll(
            @RequestParam(required = false) CatalogStatus status,
            Pageable pageable
    ) {
        return service.findAll(status, pageable);
    }

    @GetMapping("/{id}")
    public CityResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CityResponse create(@Valid @RequestBody CityRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CityResponse update(@PathVariable UUID id, @Valid @RequestBody CityRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
