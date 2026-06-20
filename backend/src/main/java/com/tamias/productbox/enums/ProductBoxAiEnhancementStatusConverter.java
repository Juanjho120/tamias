package com.tamias.productbox.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ProductBoxAiEnhancementStatusConverter implements AttributeConverter<ProductBoxAiEnhancementStatus, String> {

    @Override
    public String convertToDatabaseColumn(ProductBoxAiEnhancementStatus attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public ProductBoxAiEnhancementStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? ProductBoxAiEnhancementStatus.fromValue(dbData) : ProductBoxAiEnhancementStatus.NOT_REQUESTED;
    }
}
