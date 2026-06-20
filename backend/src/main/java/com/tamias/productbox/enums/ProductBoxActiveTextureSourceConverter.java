package com.tamias.productbox.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ProductBoxActiveTextureSourceConverter implements AttributeConverter<ProductBoxActiveTextureSource, String> {

    @Override
    public String convertToDatabaseColumn(ProductBoxActiveTextureSource attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public ProductBoxActiveTextureSource convertToEntityAttribute(String dbData) {
        return dbData != null ? ProductBoxActiveTextureSource.fromValue(dbData) : ProductBoxActiveTextureSource.UNKNOWN;
    }
}
