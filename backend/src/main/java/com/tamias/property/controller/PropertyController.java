package com.tamias.property.controller;

import com.tamias.common.dto.PageResponse;
import com.tamias.property.dto.PropertyRequest;
import com.tamias.property.dto.PropertyResponse;
import com.tamias.property.dto.PropertySummaryResponse;
import com.tamias.property.enums.PropertyStatus;
import com.tamias.property.service.PropertyService;
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
@RequestMapping("/api/v1/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @GetMapping
    public PageResponse<PropertySummaryResponse> findAll(
            @RequestParam(required = false) PropertyStatus status,
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        return propertyService.findAll(status, search, pageable);
    }

    @GetMapping("/{id}")
    public PropertyResponse findById(@PathVariable UUID id) {
        return propertyService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse create(@Valid @RequestBody PropertyRequest request) {
        return propertyService.create(request);
    }

    @PutMapping("/{id}")
    public PropertyResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody PropertyRequest request
    ) {
        return propertyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        propertyService.delete(id);
    }
}
