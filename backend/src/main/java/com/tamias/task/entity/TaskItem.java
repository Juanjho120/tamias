package com.tamias.task.entity;

import com.tamias.catalog.tasktemplate.entity.TaskTemplate;
import com.tamias.common.entity.AuditableEntity;
import com.tamias.organization.entity.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "task_items")
public class TaskItem extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_list_id", nullable = false)
    private TaskList taskList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_template_id")
    private TaskTemplate taskTemplate;

    @Column(name = "task_name", nullable = false, length = 150)
    private String taskName;

    @Column(name = "responsible_person", length = 150)
    private String responsiblePerson;

    @Column(nullable = false)
    private Boolean completed = false;

    @Column(name = "completion_date")
    private OffsetDateTime completionDate;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
