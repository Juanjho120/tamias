package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.repository.EntityImageToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EntityImageReadOnlyToolService {

    private final EntityImageToolRepository repository;

    public EntityImageReadOnlyToolService(EntityImageToolRepository repository) {
        this.repository = repository;
    }

    public AiToolAnswer reservationImages(String userQuestion, boolean withoutImages) {
        return repository.reservationImages(userQuestion, withoutImages);
    }

    public AiToolAnswer inventoryItemImages(String userQuestion, boolean withoutImages) {
        return repository.inventoryItemImages(userQuestion, withoutImages);
    }

    public AiToolAnswer purchaseImages(String userQuestion, boolean withoutImages) {
        return repository.purchaseImages(userQuestion, withoutImages);
    }
}
