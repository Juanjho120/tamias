package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.repository.ScheduledReservationGuestToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ScheduledReservationGuestReadOnlyToolService {

    private final ScheduledReservationGuestToolRepository repository;

    public ScheduledReservationGuestReadOnlyToolService(ScheduledReservationGuestToolRepository repository) {
        this.repository = repository;
    }

    public AiToolAnswer upcomingReservations() {
        return repository.upcomingReservations();
    }

    public AiToolAnswer scheduledMaintenanceSearch(String userQuestion) {
        return repository.scheduledMaintenanceSearch(userQuestion);
    }

    public AiToolAnswer upcomingScheduledMaintenance() {
        return repository.upcomingScheduledMaintenance();
    }

    public AiToolAnswer dueTodayScheduledMaintenance() {
        return repository.dueTodayScheduledMaintenance();
    }

    public AiToolAnswer dueThisWeekScheduledMaintenance() {
        return repository.dueThisWeekScheduledMaintenance();
    }

    public AiToolAnswer scheduledMaintenanceByProperty(String userQuestion) {
        return repository.scheduledMaintenanceByProperty(userQuestion);
    }

    public AiToolAnswer scheduledMaintenanceByType(String userQuestion) {
        return repository.scheduledMaintenanceByType(userQuestion);
    }

    public AiToolAnswer scheduledMaintenanceByStatus(String userQuestion) {
        return repository.scheduledMaintenanceByStatus(userQuestion);
    }

    public AiToolAnswer nextDueScheduledMaintenance(String userQuestion) {
        return repository.nextDueScheduledMaintenance(userQuestion);
    }

    public AiToolAnswer scheduledMaintenanceFrequencySummary() {
        return repository.scheduledMaintenanceFrequencySummary();
    }

    public AiToolAnswer scheduledMaintenanceHistory(String userQuestion) {
        return repository.scheduledMaintenanceHistory(userQuestion);
    }

    public AiToolAnswer scheduledMaintenanceComplianceSummary() {
        return repository.scheduledMaintenanceComplianceSummary();
    }

    public AiToolAnswer reservationsToday() {
        return repository.reservationsToday();
    }

    public AiToolAnswer currentReservations() {
        return repository.currentReservations();
    }

    public AiToolAnswer reservationsThisWeek() {
        return repository.reservationsThisWeek();
    }

    public AiToolAnswer reservationsThisMonth() {
        return repository.reservationsThisMonth();
    }

    public AiToolAnswer reservationsByProperty(String userQuestion) {
        return repository.reservationsByProperty(userQuestion);
    }

    public AiToolAnswer reservationsByGuest(String userQuestion) {
        return repository.reservationsByGuest(userQuestion);
    }

    public AiToolAnswer reservationsByStatus(String userQuestion) {
        return repository.reservationsByStatus(userQuestion);
    }

    public AiToolAnswer reservationsByPlatform(String userQuestion) {
        return repository.reservationsByPlatform(userQuestion);
    }

    public AiToolAnswer reservationSearch(String userQuestion) {
        return repository.reservationSearch(userQuestion);
    }

    public AiToolAnswer nextCheckIn() {
        return repository.nextCheckIn();
    }

    public AiToolAnswer nextCheckOut() {
        return repository.nextCheckOut();
    }

    public AiToolAnswer reservationCalendarEvents() {
        return repository.reservationCalendarEvents();
    }

    public AiToolAnswer reservationRevenueSummary(String userQuestion) {
        return repository.reservationRevenueSummary(userQuestion);
    }

    public AiToolAnswer reservationNightsSummary(String userQuestion) {
        return repository.reservationNightsSummary(userQuestion);
    }

    public AiToolAnswer reservationGuestCountSummary(String userQuestion) {
        return repository.reservationGuestCountSummary(userQuestion);
    }

    public AiToolAnswer reservationOccupancySummary(String userQuestion) {
        return repository.reservationOccupancySummary(userQuestion);
    }

    public AiToolAnswer reservationGapsBetweenReservations() {
        return repository.reservationGapsBetweenReservations();
    }

    public AiToolAnswer guestSearch(String userQuestion) {
        return repository.guestSearch(userQuestion);
    }

    public AiToolAnswer guestsByReservation(String userQuestion) {
        return repository.guestsByReservation(userQuestion);
    }

    public AiToolAnswer recentGuests() {
        return repository.recentGuests();
    }

    public AiToolAnswer returningGuests() {
        return repository.returningGuests();
    }

    public AiToolAnswer upcomingGuests() {
        return repository.upcomingGuests();
    }

    public AiToolAnswer guestCountByDateRange(String userQuestion) {
        return repository.guestCountByDateRange(userQuestion);
    }

    public AiToolAnswer overdueScheduledMaintenance() {
        return repository.overdueScheduledMaintenance();
    }
}
