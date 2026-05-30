package com.tamias.property.mapper;

import com.tamias.property.dto.PropertyRequest;
import com.tamias.property.dto.PropertyResponse;
import com.tamias.property.dto.PropertySummaryResponse;
import com.tamias.property.entity.Property;
import org.springframework.stereotype.Component;

@Component
public class PropertyMapper {

    public Property toEntity(PropertyRequest request) {
        Property property = new Property();
        property.setName(request.name());
        property.setAddress(request.address());
        property.setDescription(request.description());
        property.setStatus(request.status());
        return property;
    }

    public void updateEntity(Property property, PropertyRequest request) {
        property.setName(request.name());
        property.setAddress(request.address());
        property.setDescription(request.description());
        property.setStatus(request.status());
    }

    public PropertySummaryResponse toSummaryResponse(Property property) {
        return new PropertySummaryResponse(
                property.getId(),
                property.getName(),
                property.getAddress(),
                property.getDescription(),
                property.getStatus(),
                property.getCreatedAt()
        );
    }

    public PropertyResponse toResponse(Property property) {
        return new PropertyResponse(
                property.getId(),
                property.getName(),
                property.getAddress(),
                property.getDescription(),
                property.getStatus(),
                property.getCreatedAt(),
                property.getUpdatedAt()
        );
    }
}
