package com.tamias.catalog.paymentcategory.entity;

import com.tamias.catalog.entity.BaseCatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "payment_categories")
public class PaymentCategory extends BaseCatalogEntity {
}
