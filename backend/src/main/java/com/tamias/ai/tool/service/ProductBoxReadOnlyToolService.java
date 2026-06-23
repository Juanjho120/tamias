package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.repository.ProductBoxToolRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductBoxReadOnlyToolService {

    private final ProductBoxToolRepository repository;

    public ProductBoxReadOnlyToolService(ProductBoxToolRepository repository) {
        this.repository = repository;
    }

    public AiToolAnswer productBoxSummary() {
        return repository.productBoxSummary();
    }

    public AiToolAnswer productBoxSearch(String userQuestion) {
        return repository.productBoxSearch(userQuestion);
    }

    public AiToolAnswer productBoxIncompleteModels() {
        return repository.productBoxIncompleteModels();
    }

    public AiToolAnswer productBoxInventoryLinks(String userQuestion) {
        return repository.productBoxInventoryLinks(userQuestion);
    }

    public AiToolAnswer inventoryItemsWithoutProductBox(String userQuestion) {
        return repository.inventoryItemsWithoutProductBox(userQuestion);
    }

    public AiToolAnswer productBoxPurchaseLinks(String userQuestion) {
        return repository.productBoxPurchaseLinks(userQuestion);
    }

    public AiToolAnswer productBoxTextureStatus(String userQuestion) {
        return repository.productBoxTextureStatus(userQuestion);
    }
}
