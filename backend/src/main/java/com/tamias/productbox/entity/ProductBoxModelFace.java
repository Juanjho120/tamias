package com.tamias.productbox.entity;

import com.tamias.common.entity.AuditableEntity;
import com.tamias.organization.entity.Organization;
import com.tamias.productbox.enums.ProductBoxFaceName;
import com.tamias.productbox.enums.ProductBoxFaceNameConverter;
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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "product_box_model_faces",
    uniqueConstraints = @UniqueConstraint(name = "ux_product_box_model_faces_model_face", columnNames = {
        "product_box_model_id", "face_name"
    })
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

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key;

    @Column(name = "filepath", nullable = false, length = 300)
    private String filepath;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

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
