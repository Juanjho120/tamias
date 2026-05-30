package com.tamias.catalog.platform.entity;

import com.tamias.catalog.entity.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "platforms")
public class Platform extends BaseCatalogEntity {
}
