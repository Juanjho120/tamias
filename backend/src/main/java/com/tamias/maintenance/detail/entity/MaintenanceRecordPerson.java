package com.tamias.maintenance.detail.entity;

import com.tamias.catalog.maintenanceperson.entity.MaintenancePerson;
import com.tamias.common.entity.BaseEntity;
import com.tamias.maintenance.entity.MaintenanceRecord;
import com.tamias.organization.entity.Organization;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "maintenance_record_people")
public class MaintenanceRecordPerson extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maintenance_record_id", nullable = false)
    private MaintenanceRecord maintenanceRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maintenance_person_id", nullable = false)
    private MaintenancePerson maintenancePerson;
}
