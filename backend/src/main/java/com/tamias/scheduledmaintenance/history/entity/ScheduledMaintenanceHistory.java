package com.tamias.scheduledmaintenance.history.entity;

import com.tamias.common.entity.BaseEntity;
import com.tamias.organization.entity.Organization;
import com.tamias.scheduledmaintenance.entity.ScheduledMaintenance;
import com.tamias.scheduledmaintenance.enums.ScheduledMaintenanceStatus;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "scheduled_maintenance_history")
public class ScheduledMaintenanceHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheduled_maintenance_id", nullable = false)
    private ScheduledMaintenance scheduledMaintenance;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private ScheduledMaintenanceStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private ScheduledMaintenanceStatus newStatus;

    @Column(name = "previous_planned_date")
    private LocalDate previousPlannedDate;

    @Column(name = "new_planned_date")
    private LocalDate newPlannedDate;

    @Column(name = "previous_planned_time")
    private LocalTime previousPlannedTime;

    @Column(name = "new_planned_time")
    private LocalTime newPlannedTime;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private OffsetDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        this.changedAt = OffsetDateTime.now();
    }
}
