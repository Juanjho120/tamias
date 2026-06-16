package com.tamias.ai.tool.handler;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.context.AiToolRequestContext;
import com.tamias.ai.tool.service.AiReadOnlyToolService;
import com.tamias.ai.tool.support.AiToolRoutingSupport;
import java.util.List;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(110)
public class ScheduledReservationGuestToolHandler extends AiToolRoutingSupport implements AiToolHandler {

    private final AiReadOnlyToolService readOnlyToolService;

    public ScheduledReservationGuestToolHandler(AiReadOnlyToolService readOnlyToolService) {
        this.readOnlyToolService = readOnlyToolService;
    }

    @Override
    public Optional<AiToolAnswer> tryHandle(AiToolRequestContext context) {
        return tryHandleScheduledReservationGuestQuestion(context.question(), context.normalizedQuestion());
    }


private Optional<AiToolAnswer> tryHandleScheduledReservationGuestQuestion(String question, String normalized) {
        if (isGuestToolQuestion(normalized)) {
            if (isReturningGuestQuestion(normalized)) {
                return Optional.of(readOnlyToolService.returningGuests());
            }
            if (isUpcomingGuestQuestion(normalized)) {
                return Optional.of(readOnlyToolService.upcomingGuests());
            }
            if (isGuestCountQuestion(normalized)) {
                return Optional.of(readOnlyToolService.guestCountByDateRange(question));
            }
            if (isGuestByReservationQuestion(normalized)) {
                return Optional.of(readOnlyToolService.guestsByReservation(question));
            }
            if (isRecentGuestQuestion(normalized)) {
                return Optional.of(readOnlyToolService.recentGuests());
            }
            return Optional.of(readOnlyToolService.guestSearch(question));
        }

        if (isReservationToolQuestion(normalized)) {
            if (isReservationGapQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationGapsBetweenReservations());
            }
            if (isNextCheckOutQuestion(normalized)) {
                return Optional.of(readOnlyToolService.nextCheckOut());
            }
            if (isNextCheckInQuestion(normalized)) {
                return Optional.of(readOnlyToolService.nextCheckIn());
            }
            if (isReservationRevenueQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationRevenueSummary(question));
            }
            if (isReservationNightsQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationNightsSummary(question));
            }
            if (isReservationGuestCountQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationGuestCountSummary(question));
            }
            if (isReservationOccupancyQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationOccupancySummary(question));
            }
            if (isCurrentReservationToolQuestion(normalized)) {
                return Optional.of(readOnlyToolService.currentReservations());
            }
            if (isReservationTodayQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationsToday());
            }
            if (isReservationThisWeekQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationsThisWeek());
            }
            if (isReservationThisMonthQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationsThisMonth());
            }
            if (isReservationByGuestQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationsByGuest(question));
            }
            if (isReservationByPlatformQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationsByPlatform(question));
            }
            if (isReservationByStatusQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationsByStatus(question));
            }
            if (isReservationByPropertyQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationsByProperty(question));
            }
            if (isReservationCalendarQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationCalendarEvents());
            }
            if (isUpcomingReservationQuestion(normalized)) {
                return Optional.of(readOnlyToolService.upcomingReservations());
            }
            return Optional.of(readOnlyToolService.reservationSearch(question));
        }

        if (isScheduledMaintenanceToolQuestion(normalized)) {
            if (isScheduledMaintenanceComplianceQuestion(normalized)) {
                return Optional.of(readOnlyToolService.scheduledMaintenanceComplianceSummary());
            }
            if (isScheduledMaintenanceFrequencyQuestion(normalized)) {
                return Optional.of(readOnlyToolService.scheduledMaintenanceFrequencySummary());
            }
            if (isScheduledMaintenanceHistoryQuestion(normalized)) {
                return Optional.of(readOnlyToolService.scheduledMaintenanceHistory(question));
            }
            if (isScheduledMaintenanceDueTodayQuestion(normalized)) {
                return Optional.of(readOnlyToolService.dueTodayScheduledMaintenance());
            }
            if (isScheduledMaintenanceDueThisWeekQuestion(normalized)) {
                return Optional.of(readOnlyToolService.dueThisWeekScheduledMaintenance());
            }
            if (isOverdueScheduledMaintenanceQuestion(normalized)) {
                return Optional.of(readOnlyToolService.overdueScheduledMaintenance());
            }
            if (isScheduledMaintenanceNextDueQuestion(normalized)) {
                return Optional.of(readOnlyToolService.nextDueScheduledMaintenance(question));
            }
            if (isScheduledMaintenanceByStatusQuestion(normalized)) {
                return Optional.of(readOnlyToolService.scheduledMaintenanceByStatus(question));
            }
            if (isScheduledMaintenanceByPropertyQuestion(normalized)) {
                return Optional.of(readOnlyToolService.scheduledMaintenanceByProperty(question));
            }
            if (isScheduledMaintenanceByTypeQuestion(normalized)) {
                return Optional.of(readOnlyToolService.scheduledMaintenanceByType(question));
            }
            if (isScheduledMaintenanceUpcomingQuestion(normalized)) {
                return Optional.of(readOnlyToolService.upcomingScheduledMaintenance());
            }
            return Optional.of(readOnlyToolService.scheduledMaintenanceSearch(question));
        }

        return Optional.empty();
    }
}
