package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.support.AiReadOnlyToolSupport;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReservationSupplyTaskReadOnlyToolService extends AiReadOnlyToolSupport {

    public ReservationSupplyTaskReadOnlyToolService(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer pendingTaskLists() {
        return super.pendingTaskLists();
    }

    public AiToolAnswer reservationSupplySearch(String userQuestion) {
        return super.reservationSupplySearch(userQuestion);
    }

    public AiToolAnswer reservationSuppliesByReservation(String userQuestion) {
        return super.reservationSuppliesByReservation(userQuestion);
    }

    public AiToolAnswer reservationSuppliesByProperty(String userQuestion) {
        return super.reservationSuppliesByProperty(userQuestion);
    }

    public AiToolAnswer reservationSuppliesForUpcomingReservations() {
        return super.reservationSuppliesForUpcomingReservations();
    }

    public AiToolAnswer reservationSuppliesForLatestPastReservation() {
        return super.reservationSuppliesForLatestPastReservation();
    }

    public AiToolAnswer reservationSupplySummaryByItem(String userQuestion) {
        return super.reservationSupplySummaryByItem(userQuestion);
    }

    public AiToolAnswer reservationSupplySummaryByDateRange(String userQuestion) {
        return super.reservationSupplySummaryByDateRange(userQuestion);
    }

    public AiToolAnswer reservationSupplyLastUsed(String userQuestion) {
        return super.reservationSupplyLastUsed(userQuestion);
    }

    public AiToolAnswer reservationSupplyMostUsed() {
        return super.reservationSupplyMostUsed();
    }

    public AiToolAnswer reservationSupplyMissingForUpcomingReservations() {
        return super.reservationSupplyMissingForUpcomingReservations();
    }

    public AiToolAnswer taskListSearch(String userQuestion) {
        return super.taskListSearch(userQuestion);
    }

    public AiToolAnswer taskListsByProperty(String userQuestion) {
        return super.taskListsByProperty(userQuestion);
    }

    public AiToolAnswer taskListsByReservation(String userQuestion) {
        return super.taskListsByReservation(userQuestion);
    }

    public AiToolAnswer taskListsForNextReservation() {
        return super.taskListsForNextReservation();
    }

    public AiToolAnswer activeTaskLists() {
        return super.activeTaskLists();
    }

    public AiToolAnswer completedTaskLists() {
        return super.completedTaskLists();
    }

    public AiToolAnswer overdueTaskLists() {
        return super.overdueTaskLists();
    }

    public AiToolAnswer dueTodayTaskLists() {
        return super.dueTodayTaskLists();
    }

    public AiToolAnswer dueThisWeekTaskLists() {
        return super.dueThisWeekTaskLists();
    }

    public AiToolAnswer taskListProgressSummary() {
        return super.taskListProgressSummary();
    }

    public AiToolAnswer taskListCompletionSummary() {
        return super.taskListCompletionSummary();
    }

    public AiToolAnswer taskItemSearch(String userQuestion) {
        return super.taskItemSearch(userQuestion);
    }

    public AiToolAnswer taskItemsByTaskList(String userQuestion) {
        return super.taskItemsByTaskList(userQuestion);
    }

    public AiToolAnswer pendingTaskItems() {
        return super.pendingTaskItems();
    }

    public AiToolAnswer completedTaskItems() {
        return super.completedTaskItems();
    }

    public AiToolAnswer overdueTaskItems() {
        return super.overdueTaskItems();
    }

    public AiToolAnswer taskItemAssignedSummary() {
        return super.taskItemAssignedSummary();
    }

    public AiToolAnswer taskItemPrioritySummary() {
        return super.taskItemPrioritySummary();
    }

}
