package com.tamias.catalog.mapper;

import com.tamias.catalog.dto.CatalogRequest;
import com.tamias.catalog.dto.CatalogResponse;
import com.tamias.catalog.dto.CityRequest;
import com.tamias.catalog.dto.CityResponse;
import com.tamias.catalog.dto.MaintenancePersonRequest;
import com.tamias.catalog.dto.MaintenancePersonResponse;
import com.tamias.catalog.dto.MaintenanceTypeRequest;
import com.tamias.catalog.dto.MaintenanceTypeResponse;
import com.tamias.catalog.dto.SupplierRequest;
import com.tamias.catalog.dto.SupplierResponse;
import com.tamias.catalog.dto.TaskTemplateRequest;
import com.tamias.catalog.dto.TaskTemplateResponse;
import com.tamias.catalog.entity.BaseCatalogEntity;
import com.tamias.catalog.city.entity.City;
import com.tamias.catalog.maintenanceperson.entity.MaintenancePerson;
import com.tamias.catalog.maintenancetype.entity.MaintenanceType;
import com.tamias.catalog.supplier.entity.Supplier;
import com.tamias.catalog.tasktemplate.entity.TaskTemplate;
import org.springframework.stereotype.Component;

@Component
public class CatalogMapper {

    public <T extends BaseCatalogEntity> void updateBaseCatalog(T entity, CatalogRequest request) {
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setStatus(request.status());
    }

    public CatalogResponse toCatalogResponse(BaseCatalogEntity entity) {
        return new CatalogResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public MaintenanceTypeResponse toMaintenanceTypeResponse(MaintenanceType entity) {
        return new MaintenanceTypeResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public void updateMaintenanceType(MaintenanceType entity, MaintenanceTypeRequest request) {
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setStatus(request.status());
    }

    public MaintenancePersonResponse toMaintenancePersonResponse(MaintenancePerson entity) {
        return new MaintenancePersonResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getNotes(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public void updateMaintenancePerson(MaintenancePerson entity, MaintenancePersonRequest request) {
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setPhone(request.phone());
        entity.setEmail(request.email());
        entity.setNotes(request.notes());
        entity.setStatus(request.status());
    }

    public SupplierResponse toSupplierResponse(Supplier entity) {
        return new SupplierResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getAddress(),
                entity.getNotes(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public void updateSupplier(Supplier entity, SupplierRequest request) {
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setPhone(request.phone());
        entity.setEmail(request.email());
        entity.setAddress(request.address());
        entity.setNotes(request.notes());
        entity.setStatus(request.status());
    }

    public CityResponse toCityResponse(City entity) {
        return new CityResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getDepartment(),
                entity.getCountry(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public void updateCity(City entity, CityRequest request) {
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setDepartment(request.department());
        entity.setCountry(request.country());
        entity.setStatus(request.status());
    }

    public TaskTemplateResponse toTaskTemplateResponse(TaskTemplate entity) {
        return new TaskTemplateResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public void updateTaskTemplate(TaskTemplate entity, TaskTemplateRequest request) {
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setStatus(request.status());
    }
}
