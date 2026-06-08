package com.tamias.purchase.controller;

import com.tamias.common.dto.PageResponse;
import com.tamias.purchase.dto.PurchaseItemPurchasedRequest;
import com.tamias.purchase.dto.PurchaseItemRequest;
import com.tamias.purchase.dto.PurchaseItemResponse;
import com.tamias.purchase.dto.PurchaseItemUpdateRequest;
import com.tamias.purchase.dto.PurchaseListRequest;
import com.tamias.purchase.dto.PurchaseListResponse;
import com.tamias.purchase.dto.PurchaseListSummaryResponse;
import com.tamias.purchase.enums.PurchaseListStatus;
import com.tamias.purchase.service.PurchaseListService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/purchase-lists")
public class PurchaseListController {

    private final PurchaseListService purchaseListService;

    public PurchaseListController(PurchaseListService purchaseListService) {
        this.purchaseListService = purchaseListService;
    }

    @GetMapping
    public PageResponse<PurchaseListSummaryResponse> findAll(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) PurchaseListStatus status,
            Pageable pageable
    ) {
        return purchaseListService.findAll(propertyId, supplierId, cityId, status, pageable);
    }

    @GetMapping("/{id}")
    public PurchaseListResponse findById(@PathVariable UUID id) {
        return purchaseListService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseListResponse create(@Valid @RequestBody PurchaseListRequest request) {
        return purchaseListService.create(request);
    }

    @PutMapping("/{id}")
    public PurchaseListResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody PurchaseListRequest request
    ) {
        return purchaseListService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        purchaseListService.delete(id);
    }

    @PostMapping("/{purchaseListId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseItemResponse createItem(
            @PathVariable UUID purchaseListId,
            @Valid @RequestBody PurchaseItemRequest request
    ) {
        return purchaseListService.createItem(purchaseListId, request);
    }

    @PutMapping("/{purchaseListId}/items/{itemId}")
    public PurchaseItemResponse updateItem(
            @PathVariable UUID purchaseListId,
            @PathVariable UUID itemId,
            @Valid @RequestBody PurchaseItemUpdateRequest request
    ) {
        return purchaseListService.updateItem(purchaseListId, itemId, request);
    }

    @PatchMapping("/{purchaseListId}/items/{itemId}/purchased")
    public PurchaseItemResponse updateItemPurchased(
            @PathVariable UUID purchaseListId,
            @PathVariable UUID itemId,
            @Valid @RequestBody PurchaseItemPurchasedRequest request
    ) {
        return purchaseListService.updateItemPurchased(purchaseListId, itemId, request);
    }

    @DeleteMapping("/{purchaseListId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(
            @PathVariable UUID purchaseListId,
            @PathVariable UUID itemId
    ) {
        purchaseListService.deleteItem(purchaseListId, itemId);
    }
}
