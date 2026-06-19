package com.tamias.productbox.entity;

import com.tamias.catalog.inventoryitem.entity.InventoryItem;
import com.tamias.common.entity.AuditableEntity;
import com.tamias.organization.entity.Organization;
import com.tamias.productbox.enums.ProductBoxUnit;
import com.tamias.productbox.enums.ProductBoxUnitConverter;
import com.tamias.purchase.entity.PurchaseItem;
import com.tamias.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "product_box_models")
public class ProductBoxModel extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id")
    private InventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_item_id")
    private PurchaseItem purchaseItem;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal width;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal height;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal depth;

    @Convert(converter = ProductBoxUnitConverter.class)
    @Column(nullable = false, length = 20)
    private ProductBoxUnit unit = ProductBoxUnit.CM;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    @OneToMany(mappedBy = "productBoxModel", fetch = FetchType.LAZY)
    private List<ProductBoxModelFace> faces = new ArrayList<>();
}
