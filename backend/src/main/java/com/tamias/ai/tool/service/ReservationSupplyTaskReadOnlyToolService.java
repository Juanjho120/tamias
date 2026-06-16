package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.repository.ReservationSupplyTaskToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReservationSupplyTaskReadOnlyToolService {

    private final ReservationSupplyTaskToolRepository repository;

    public ReservationSupplyTaskReadOnlyToolService(ReservationSupplyTaskToolRepository repository) {
        this.repository = repository;
    }

    public AiToolAnswer pendingTaskLists() {
        return repository.pendingTaskLists();
    }

    public AiToolAnswer reservationSupplySearch(String userQuestion) {
        return repository.reservationSupplySearch(userQuestion);
    }

    public AiToolAnswer reservationSuppliesByReservation(String userQuestion) {
        return repository.reservationSuppliesByReservation(userQuestion);
    }

    public AiToolAnswer reservationSuppliesByProperty(String userQuestion) {
        return repository.reservationSuppliesByProperty(userQuestion);
    }

    public AiToolAnswer reservationSuppliesForUpcomingReservations() {
        return repository.reservationSuppliesForUpcomingReservations();
    }

    public AiToolAnswer reservationSuppliesForLatestPastReservation() {
        return repository.reservationSuppliesForLatestPastReservation();
    }

    public AiToolAnswer reservationSupplySummaryByItem(String userQuestion) {
        return repository.reservationSupplySummaryByItem(userQuestion);
    }

    public AiToolAnswer reservationSupplySummaryByDateRange(String userQuestion) {
        return repository.reservationSupplySummaryByDateRange(userQuestion);
    }

    public AiToolAnswer reservationSupplyLastUsed(String userQuestion) {
        return repository.reservationSupplyLastUsed(userQuestion);
    }

    public AiToolAnswer reservationSupplyMostUsed() {
        return repository.reservationSupplyMostUsed();
    }

    public AiToolAnswer reservationSupplyMissingForUpcomingReservations() {
        return repository.reservationSupplyMissingForUpcomingReservations();
    }

    public AiToolAnswer taskListSearch(String userQuestion) {
        return repository.taskListSearch(userQuestion);
    }

    public AiToolAnswer taskListsByProperty(String userQuestion) {
        return repository.taskListsByProperty(userQuestion);
    }

    public AiToolAnswer taskListsByReservation(String userQuestion) {
        return repository.taskListsByReservation(userQuestion);
    }

    public AiToolAnswer taskListsForNextReservation() {
        return repository.taskListsForNextReservation();
    }

    public AiToolAnswer activeTaskLists() {
        return repository.activeTaskLists();
    }

    public AiToolAnswer completedTaskLists() {
        return repository.completedTaskLists();
    }

    public AiToolAnswer overdueTaskLists() {
        return repository.overdueTaskLists();
    }

    public AiToolAnswer dueTodayTaskLists() {
        return repository.dueTodayTaskLists();
    }

    public AiToolAnswer dueThisWeekTaskLists() {
        return repository.dueThisWeekTaskLists();
    }

    public AiToolAnswer taskListProgressSummary() {
        return repository.taskListProgressSummary();
    }

    public AiToolAnswer taskListCompletionSummary() {
        return repository.taskListCompletionSummary();
    }

    public AiToolAnswer taskItemSearch(String userQuestion) {
        return repository.taskItemSearch(userQuestion);
    }

    public AiToolAnswer taskItemsByTaskList(String userQuestion) {
        return repository.taskItemsByTaskList(userQuestion);
    }

    public AiToolAnswer pendingTaskItems() {
        return repository.pendingTaskItems();
    }

    public AiToolAnswer completedTaskItems() {
        return repository.completedTaskItems();
    }

    public AiToolAnswer overdueTaskItems() {
        return repository.overdueTaskItems();
    }

    public AiToolAnswer taskItemAssignedSummary() {
        return repository.taskItemAssignedSummary();
    }

    public AiToolAnswer taskItemPrioritySummary() {
        return repository.taskItemPrioritySummary();
    }
}
