package com.tamias.scheduledmaintenance.entity;

import com.tamias.catalog.maintenancecategory.entity.MaintenanceCategory;
import com.tamias.catalog.maintenanceperson.entity.MaintenancePerson;
import com.tamias.catalog.maintenancetype.entity.MaintenanceType;
import com.tamias.common.entity.AuditableEntity;
import com.tamias.organization.entity.Organization;
import com.tamias.property.entity.Property;
import com.tamias.scheduledmaintenance.enums.ScheduledMaintenanceFrequency;
import com.tamias.scheduledmaintenance.enums.ScheduledMaintenanceStatus;
import com.tamias.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "scheduled_maintenance")
public class ScheduledMaintenance extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_category_id")
    private MaintenanceCategory maintenanceCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_type_id")
    private MaintenanceType maintenanceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maintenance_person_id")
    private MaintenancePerson maintenancePerson;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScheduledMaintenanceFrequency frequency;

    @Column(name = "interval_value", nullable = false)
    private Integer intervalValue = 1;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    @Column(name = "last_generated_at")
    private OffsetDateTime lastGeneratedAt;

    @Column(name = "estimated_cost", precision = 12, scale = 2)
    private BigDecimal estimatedCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScheduledMaintenanceStatus status = ScheduledMaintenanceStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;
}
