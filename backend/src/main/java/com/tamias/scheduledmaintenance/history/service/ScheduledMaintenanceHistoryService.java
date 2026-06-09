package com.tamias.scheduledmaintenance.history.service;

import com.tamias.scheduledmaintenance.entity.ScheduledMaintenance;
import com.tamias.scheduledmaintenance.enums.ScheduledMaintenanceStatus;
import com.tamias.scheduledmaintenance.history.dto.ScheduledMaintenanceHistoryResponse;
import com.tamias.scheduledmaintenance.history.entity.ScheduledMaintenanceHistory;
import com.tamias.scheduledmaintenance.history.mapper.ScheduledMaintenanceHistoryMapper;
import com.tamias.scheduledmaintenance.history.repository.ScheduledMaintenanceHistoryRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.entity.User;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduledMaintenanceHistoryService {

    private final ScheduledMaintenanceHistoryRepository historyRepository;
    private final ScheduledMaintenanceHistoryMapper historyMapper;
    private final CurrentUserService currentUserService;

    public ScheduledMaintenanceHistoryService(
            ScheduledMaintenanceHistoryRepository historyRepository,
            ScheduledMaintenanceHistoryMapper historyMapper,
            CurrentUserService currentUserService
    ) {
        this.historyRepository = historyRepository;
        this.historyMapper = historyMapper;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<ScheduledMaintenanceHistoryResponse> findHistory(ScheduledMaintenance scheduledMaintenance) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();

        return historyRepository
                .findByScheduledMaintenance_IdAndOrganization_IdOrderByChangedAtDesc(
                        scheduledMaintenance.getId(),
                        organizationId
                )
                .stream()
                .map(historyMapper::toResponse)
                .toList();
    }

    @Transactional
    public void recordChange(
            ScheduledMaintenance scheduledMaintenance,
            ScheduledMaintenanceStatus previousStatus,
            ScheduledMaintenanceStatus newStatus,
            LocalDate previousPlannedDate,
            LocalDate newPlannedDate,
            String reason,
            User changedBy
    ) {
        ScheduledMaintenanceHistory history = new ScheduledMaintenanceHistory();
        history.setOrganization(scheduledMaintenance.getOrganization());
        history.setScheduledMaintenance(scheduledMaintenance);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setPreviousPlannedDate(previousPlannedDate);
        history.setNewPlannedDate(newPlannedDate);
        history.setPreviousPlannedTime(null);
        history.setNewPlannedTime(null);
        history.setReason(reason);
        history.setChangedBy(changedBy);

        historyRepository.save(history);
    }
}
