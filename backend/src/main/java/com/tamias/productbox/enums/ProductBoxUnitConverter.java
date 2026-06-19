package com.tamias.productbox.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ProductBoxUnitConverter implements AttributeConverter<ProductBoxUnit, String> {

    @Override
    public String convertToDatabaseColumn(ProductBoxUnit attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public ProductBoxUnit convertToEntityAttribute(String dbData) {
        return ProductBoxUnit.fromValue(dbData);
    }
}
