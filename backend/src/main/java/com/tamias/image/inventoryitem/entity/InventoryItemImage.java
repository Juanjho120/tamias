package com.tamias.image.inventoryitem.entity;

import com.tamias.catalog.inventoryitem.entity.InventoryItem;
import com.tamias.common.entity.BaseEntity;
import com.tamias.image.enums.ImageStatus;
import com.tamias.organization.entity.Organization;
import com.tamias.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "inventory_item_images")
public class InventoryItemImage extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "inventory_item_id", nullable = false)
  private InventoryItem inventoryItem;

  @Column(name = "original_filename", nullable = false, length = 255)
  private String originalFilename;

  @Column(name = "s3_key", nullable = false, columnDefinition = "TEXT")
  private String s3Key;

  @Column(name = "filepath", length = 300)
  private String filepath;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private Long sizeBytes;

  @Column(name = "is_cover", nullable = false)
  private Boolean cover = false;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private ImageStatus status = ImageStatus.ACTIVE;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  void prePersist() {
    if (createdAt == null) {
      createdAt = OffsetDateTime.now();
    }
    if (status == null) {
      status = ImageStatus.ACTIVE;
    }
    if (cover == null) {
      cover = false;
    }
  }
}
