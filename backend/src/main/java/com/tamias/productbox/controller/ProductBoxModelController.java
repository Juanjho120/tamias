package com.tamias.productbox.controller;

import com.tamias.common.dto.PageResponse;
import com.tamias.productbox.dto.ProductBoxModelRequest;
import com.tamias.productbox.dto.ProductBoxModelResponse;
import com.tamias.productbox.service.ProductBoxModelService;
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
@RequestMapping("/api/v1/product-box-models")
public class ProductBoxModelController {

    private final ProductBoxModelService productBoxModelService;

    public ProductBoxModelController(ProductBoxModelService productBoxModelService) {
        this.productBoxModelService = productBoxModelService;
    }

    @GetMapping
    public PageResponse<ProductBoxModelResponse> findAll(
        @RequestParam(required = false) UUID inventoryItemId,
        @RequestParam(required = false) UUID purchaseItemId,
        @RequestParam(required = false) String search,
        Pageable pageable
    ) {
        return productBoxModelService.findAll(inventoryItemId, purchaseItemId, search, pageable);
    }

    @GetMapping("/{id}")
    public ProductBoxModelResponse findById(@PathVariable UUID id) {
        return productBoxModelService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductBoxModelResponse create(@Valid @RequestBody ProductBoxModelRequest request) {
        return productBoxModelService.create(request);
    }

    @PutMapping("/{id}")
    public ProductBoxModelResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody ProductBoxModelRequest request
    ) {
        return productBoxModelService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        productBoxModelService.delete(id);
    }
}
