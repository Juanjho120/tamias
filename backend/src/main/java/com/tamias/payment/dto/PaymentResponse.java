package com.tamias.payment.dto;

import com.tamias.payment.enums.PaymentMethod;
import com.tamias.payment.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID propertyId,
        String propertyName,
        UUID categoryId,
        String categoryName,
        String name,
        String description,
        PaymentMethod method,
        BigDecimal amount,
        String responsible,
        LocalDate payDate,
        PaymentStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
