package com.tamias.task.repository;

import com.tamias.task.entity.TaskItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskItemRepository extends JpaRepository<TaskItem, UUID> {

    List<TaskItem> findByTaskList_IdOrderBySortOrderAscCreatedAtAsc(UUID taskListId);

    Optional<TaskItem> findByIdAndTaskList_IdAndOrganization_Id(UUID id, UUID taskListId, UUID organizationId);

    long countByTaskList_Id(UUID taskListId);

    long countByTaskList_IdAndCompleted(UUID taskListId, Boolean completed);

    void deleteByTaskList_Id(UUID taskListId);
}
