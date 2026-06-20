package com.tamias.productbox.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ProductBoxTextureStatusConverter implements AttributeConverter<ProductBoxTextureStatus, String> {
    @Override
    public String convertToDatabaseColumn(ProductBoxTextureStatus attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public ProductBoxTextureStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? ProductBoxTextureStatus.fromValue(dbData) : null;
    }
}
