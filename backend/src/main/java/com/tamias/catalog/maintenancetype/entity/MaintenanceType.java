package com.tamias.catalog.maintenancetype.entity;

import com.tamias.catalog.entity.BaseCatalogEntity;
import com.tamias.catalog.maintenancecategory.entity.MaintenanceCategory;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "maintenance_types")
public class MaintenanceType extends BaseCatalogEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_category_id")
    private MaintenanceCategory maintenanceCategory;
}
