package com.tamias.productbox.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ProductBoxFaceNameConverter implements AttributeConverter<ProductBoxFaceName, String> {

    @Override
    public String convertToDatabaseColumn(ProductBoxFaceName attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public ProductBoxFaceName convertToEntityAttribute(String dbData) {
        return dbData != null ? ProductBoxFaceName.fromValue(dbData) : null;
    }
}
