package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.support.AiReadOnlyToolSupport;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ScheduledReservationGuestReadOnlyToolService extends AiReadOnlyToolSupport {

    public ScheduledReservationGuestReadOnlyToolService(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer upcomingReservations() {
        return super.upcomingReservations();
    }

    public AiToolAnswer scheduledMaintenanceSearch(String userQuestion) {
        return super.scheduledMaintenanceSearch(userQuestion);
    }

    public AiToolAnswer upcomingScheduledMaintenance() {
        return super.upcomingScheduledMaintenance();
    }

    public AiToolAnswer dueTodayScheduledMaintenance() {
        return super.dueTodayScheduledMaintenance();
    }

    public AiToolAnswer dueThisWeekScheduledMaintenance() {
        return super.dueThisWeekScheduledMaintenance();
    }

    public AiToolAnswer scheduledMaintenanceByProperty(String userQuestion) {
        return super.scheduledMaintenanceByProperty(userQuestion);
    }

    public AiToolAnswer scheduledMaintenanceByType(String userQuestion) {
        return super.scheduledMaintenanceByType(userQuestion);
    }

    public AiToolAnswer scheduledMaintenanceByStatus(String userQuestion) {
        return super.scheduledMaintenanceByStatus(userQuestion);
    }

    public AiToolAnswer nextDueScheduledMaintenance(String userQuestion) {
        return super.nextDueScheduledMaintenance(userQuestion);
    }

    public AiToolAnswer scheduledMaintenanceFrequencySummary() {
        return super.scheduledMaintenanceFrequencySummary();
    }

    public AiToolAnswer scheduledMaintenanceHistory(String userQuestion) {
        return super.scheduledMaintenanceHistory(userQuestion);
    }

    public AiToolAnswer scheduledMaintenanceComplianceSummary() {
        return super.scheduledMaintenanceComplianceSummary();
    }

    public AiToolAnswer reservationsToday() {
        return super.reservationsToday();
    }

    public AiToolAnswer currentReservations() {
        return super.currentReservations();
    }

    public AiToolAnswer reservationsThisWeek() {
        return super.reservationsThisWeek();
    }

    public AiToolAnswer reservationsThisMonth() {
        return super.reservationsThisMonth();
    }

    public AiToolAnswer reservationsByProperty(String userQuestion) {
        return super.reservationsByProperty(userQuestion);
    }

    public AiToolAnswer reservationsByGuest(String userQuestion) {
        return super.reservationsByGuest(userQuestion);
    }

    public AiToolAnswer reservationsByStatus(String userQuestion) {
        return super.reservationsByStatus(userQuestion);
    }

    public AiToolAnswer reservationsByPlatform(String userQuestion) {
        return super.reservationsByPlatform(userQuestion);
    }

    public AiToolAnswer reservationSearch(String userQuestion) {
        return super.reservationSearch(userQuestion);
    }

    public AiToolAnswer nextCheckIn() {
        return super.nextCheckIn();
    }

    public AiToolAnswer nextCheckOut() {
        return super.nextCheckOut();
    }

    public AiToolAnswer reservationCalendarEvents() {
        return super.reservationCalendarEvents();
    }

    public AiToolAnswer reservationRevenueSummary(String userQuestion) {
        return super.reservationRevenueSummary(userQuestion);
    }

    public AiToolAnswer reservationNightsSummary(String userQuestion) {
        return super.reservationNightsSummary(userQuestion);
    }

    public AiToolAnswer reservationGuestCountSummary(String userQuestion) {
        return super.reservationGuestCountSummary(userQuestion);
    }

    public AiToolAnswer reservationOccupancySummary(String userQuestion) {
        return super.reservationOccupancySummary(userQuestion);
    }

    public AiToolAnswer reservationGapsBetweenReservations() {
        return super.reservationGapsBetweenReservations();
    }

    public AiToolAnswer guestSearch(String userQuestion) {
        return super.guestSearch(userQuestion);
    }

    public AiToolAnswer guestsByReservation(String userQuestion) {
        return super.guestsByReservation(userQuestion);
    }

    public AiToolAnswer recentGuests() {
        return super.recentGuests();
    }

    public AiToolAnswer returningGuests() {
        return super.returningGuests();
    }

    public AiToolAnswer upcomingGuests() {
        return super.upcomingGuests();
    }

    public AiToolAnswer guestCountByDateRange(String userQuestion) {
        return super.guestCountByDateRange(userQuestion);
    }

    public AiToolAnswer overdueScheduledMaintenance() {
        return super.overdueScheduledMaintenance();
    }

}
