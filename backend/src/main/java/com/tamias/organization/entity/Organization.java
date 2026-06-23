package com.tamias.organization.entity;

import com.tamias.common.entity.AuditableEntity;
import com.tamias.organization.enums.OrganizationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "organizations")
public class Organization extends AuditableEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    @Column(name = "logo_original_filename", length = 255)
    private String logoOriginalFilename;

    @Column(name = "logo_s3_key", columnDefinition = "TEXT")
    private String logoS3Key;

    @Column(name = "logo_filepath", length = 300)
    private String logoFilepath;

    @Column(name = "logo_content_type", length = 100)
    private String logoContentType;

    @Column(name = "logo_size_bytes")
    private Long logoSizeBytes;

    @Column(name = "logo_updated_at")
    private OffsetDateTime logoUpdatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
