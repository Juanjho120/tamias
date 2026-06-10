package com.tamias.catalog.inventoryitem.entity;

import com.tamias.catalog.entity.BaseCatalogEntity;
import com.tamias.catalog.enums.InventoryItemType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "inventory_items")
public class InventoryItem extends BaseCatalogEntity {

    @Column(length = 50)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 50)
    private InventoryItemType itemType = InventoryItemType.MATERIAL;

    @Column(name = "internal_code", length = 100)
    private String internalCode;

    @Column(length = 100)
    private String barcode;

    @Column(name = "available_for_maintenance", nullable = false)
    private Boolean availableForMaintenance = true;

    @Column(name = "available_for_reservations", nullable = false)
    private Boolean availableForReservations = false;

    @Column(name = "available_for_purchases", nullable = false)
    private Boolean availableForPurchases = true;
}
