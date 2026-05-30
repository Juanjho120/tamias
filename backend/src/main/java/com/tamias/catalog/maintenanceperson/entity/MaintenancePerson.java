package com.tamias.catalog.maintenanceperson.entity;

import com.tamias.catalog.entity.BaseCatalogEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "maintenance_people")
public class MaintenancePerson extends BaseCatalogEntity {

    @Column(length = 50)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
