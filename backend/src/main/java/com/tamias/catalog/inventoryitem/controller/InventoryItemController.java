package com.tamias.catalog.inventoryitem.controller;

import com.tamias.catalog.dto.InventoryItemRequest;
import com.tamias.catalog.dto.InventoryItemResponse;
import com.tamias.catalog.enums.CatalogStatus;
import com.tamias.catalog.enums.InventoryItemType;
import com.tamias.catalog.inventoryitem.service.InventoryItemService;
import com.tamias.common.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory-items")
public class InventoryItemController {

    private final InventoryItemService service;

    public InventoryItemController(InventoryItemService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<InventoryItemResponse> findAll(
            @RequestParam(required = false) CatalogStatus status,
            @RequestParam(required = false) InventoryItemType itemType,
            @RequestParam(required = false) Boolean availableForMaintenance,
            @RequestParam(required = false) Boolean availableForReservations,
            @RequestParam(required = false) Boolean availableForPurchases,
            @RequestParam(required = false) String search,
            Pageable pageable
    ) {
        return service.findAll(
                status,
                itemType,
                availableForMaintenance,
                availableForReservations,
                availableForPurchases,
                search,
                pageable
        );
    }

    @GetMapping("/{id}")
    public InventoryItemResponse findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryItemResponse create(@Valid @RequestBody InventoryItemRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public InventoryItemResponse update(@PathVariable UUID id, @Valid @RequestBody InventoryItemRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
