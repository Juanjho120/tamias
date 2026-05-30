package com.tamias.catalog.city.entity;

import com.tamias.catalog.entity.BaseCatalogEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cities")
public class City extends BaseCatalogEntity {

    @Column(length = 150)
    private String department;

    @Column(length = 150)
    private String country;
}
