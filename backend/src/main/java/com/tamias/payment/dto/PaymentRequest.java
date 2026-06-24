package com.tamias.payment.dto;

import com.tamias.payment.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentRequest(
        UUID propertyId,
        @NotNull(message = "Payment category is required")
        UUID categoryId,
        @NotBlank(message = "Payment name is required")
        @Size(max = 150, message = "Payment name must be at most 150 characters")
        String name,
        String description,
        @NotNull(message = "Payment method is required")
        PaymentMethod method,
        @NotNull(message = "Payment amount is required")
        @DecimalMin(value = "0.00", message = "Payment amount must be greater than or equal to 0")
        BigDecimal amount,
        @Size(max = 150, message = "Responsible must be at most 150 characters")
        String responsible,
        @NotNull(message = "Payment date is required")
        LocalDate payDate
) {
}
