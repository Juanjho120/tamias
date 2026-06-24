package com.tamias.payment.mapper;

import com.tamias.payment.dto.PaymentRequest;
import com.tamias.payment.dto.PaymentResponse;
import com.tamias.payment.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public void updateEntity(Payment entity, PaymentRequest request) {
        entity.setName(request.name().trim());
        entity.setDescription(normalizeBlank(request.description()));
        entity.setMethod(request.method());
        entity.setAmount(request.amount());
        entity.setResponsible(normalizeBlank(request.responsible()));
        entity.setPayDate(request.payDate());
    }

    public PaymentResponse toResponse(Payment entity) {
        var property = entity.getProperty();
        var category = entity.getCategory();

        return new PaymentResponse(
                entity.getId(),
                property != null ? property.getId() : null,
                property != null ? property.getName() : null,
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                entity.getName(),
                entity.getDescription(),
                entity.getMethod(),
                entity.getAmount(),
                entity.getResponsible(),
                entity.getPayDate(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String normalizeBlank(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }
}
