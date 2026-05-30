package com.tamias.catalog.material.entity;

import com.tamias.catalog.entity.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "materials")
public class Material extends BaseCatalogEntity {
}
