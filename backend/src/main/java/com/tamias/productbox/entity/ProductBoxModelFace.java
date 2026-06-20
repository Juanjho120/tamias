package com.tamias.productbox.entity;

import com.tamias.common.entity.AuditableEntity;
import com.tamias.organization.entity.Organization;
import com.tamias.productbox.enums.ProductBoxFaceName;
import com.tamias.productbox.enums.ProductBoxFaceNameConverter;
import com.tamias.productbox.enums.ProductBoxTextureStatus;
import com.tamias.productbox.enums.ProductBoxTextureStatusConverter;
import com.tamias.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(
    name = "product_box_model_faces",
    uniqueConstraints = @UniqueConstraint(
        name = "ux_product_box_model_faces_model_face",
        columnNames = { "product_box_model_id", "face_name" }
    )
)
public class ProductBoxModelFace extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_box_model_id", nullable = false)
    private ProductBoxModel productBoxModel;

    @Convert(converter = ProductBoxFaceNameConverter.class)
    @Column(name = "face_name", nullable = false, length = 20)
    private ProductBoxFaceName faceName;

    @Column(name = "s3_key", length = 500)
    private String s3Key;

    @Column(name = "filepath", length = 300)
    private String filepath;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "original_s3_key", length = 500)
    private String originalS3Key;

    @Column(name = "original_filepath", length = 300)
    private String originalFilepath;

    @Column(name = "original_upload_filename", length = 255)
    private String originalUploadFilename;

    @Column(name = "original_content_type", length = 100)
    private String originalContentType;

    @Column(name = "original_size_bytes")
    private Long originalSizeBytes;

    @Column(name = "original_width_px")
    private Integer originalWidthPx;

    @Column(name = "original_height_px")
    private Integer originalHeightPx;

    @Column(name = "processed_s3_key", length = 500)
    private String processedS3Key;

    @Column(name = "processed_filepath", length = 300)
    private String processedFilepath;

    @Column(name = "processed_filename", length = 255)
    private String processedFilename;

    @Column(name = "processed_content_type", length = 100)
    private String processedContentType;

    @Column(name = "processed_size_bytes")
    private Long processedSizeBytes;

    @Column(name = "processed_width_px")
    private Integer processedWidthPx;

    @Column(name = "processed_height_px")
    private Integer processedHeightPx;

    @Column(name = "target_aspect_ratio", precision = 12, scale = 6)
    private BigDecimal targetAspectRatio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "points_json", columnDefinition = "jsonb")
    private String pointsJson;

    @Convert(converter = ProductBoxTextureStatusConverter.class)
    @Column(name = "texture_status", nullable = false, length = 30)
    private ProductBoxTextureStatus textureStatus = ProductBoxTextureStatus.ACCEPTED;

    @Column(name = "processing_error", columnDefinition = "TEXT")
    private String processingError;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    @Column(name = "rotation_degrees", precision = 10, scale = 2)
    private BigDecimal rotationDegrees;

    @Column(name = "flip_horizontal", nullable = false)
    private Boolean flipHorizontal = false;

    @Column(name = "flip_vertical", nullable = false)
    private Boolean flipVertical = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;
}
