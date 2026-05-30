package com.tamias.catalog.maintenancecategory.entity;

import com.tamias.catalog.entity.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "maintenance_categories")
public class MaintenanceCategory extends BaseCatalogEntity {
}
