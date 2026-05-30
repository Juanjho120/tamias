package com.tamias.catalog.brand.entity;

import com.tamias.catalog.entity.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "brands")
public class Brand extends BaseCatalogEntity {
}
