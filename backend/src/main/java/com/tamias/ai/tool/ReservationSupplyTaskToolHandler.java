package com.tamias.ai.tool;

import java.util.List;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(70)
public class ReservationSupplyTaskToolHandler extends AiToolRoutingSupport implements AiToolHandler {

    private final AiReadOnlyToolService readOnlyToolService;

    public ReservationSupplyTaskToolHandler(AiReadOnlyToolService readOnlyToolService) {
        this.readOnlyToolService = readOnlyToolService;
    }

    @Override
    public Optional<AiToolAnswer> tryHandle(AiToolRequestContext context) {
        return tryHandleReservationSupplyAndTaskQuestion(context.question(), context.normalizedQuestion());
    }


private Optional<AiToolAnswer> tryHandleReservationSupplyAndTaskQuestion(String question, String normalized) {
        if (isReservationSupplyToolQuestion(normalized)) {
            if (isReservationSupplyMissingQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationSupplyMissingForUpcomingReservations());
            }
            if (isReservationSupplyMostUsedQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationSupplyMostUsed());
            }
            if (isReservationSupplyLatestReservationQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationSuppliesForLatestPastReservation());
            }
            if (isReservationSupplyLastUsedQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationSupplyLastUsed(question));
            }
            if (isReservationSupplyUpcomingQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationSuppliesForUpcomingReservations());
            }
            if (isReservationSupplySummaryByDateQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationSupplySummaryByDateRange(question));
            }
            if (isReservationSupplySummaryQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationSupplySummaryByItem(question));
            }
            if (isReservationSupplyByPropertyQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationSuppliesByProperty(question));
            }
            if (isReservationSupplyByReservationQuestion(normalized)) {
                return Optional.of(readOnlyToolService.reservationSuppliesByReservation(question));
            }
            return Optional.of(readOnlyToolService.reservationSupplySearch(question));
        }

        if (isTaskItemToolQuestion(normalized)) {
            if (isTaskItemAssignedSummaryQuestion(normalized)) {
                return Optional.of(readOnlyToolService.taskItemAssignedSummary());
            }
            if (isTaskItemPrioritySummaryQuestion(normalized)) {
                return Optional.of(readOnlyToolService.taskItemPrioritySummary());
            }
            if (isTaskItemOverdueQuestion(normalized)) {
                return Optional.of(readOnlyToolService.overdueTaskItems());
            }
            if (isTaskItemCompletedQuestion(normalized)) {
                return Optional.of(readOnlyToolService.completedTaskItems());
            }
            if (isTaskItemPendingQuestion(normalized)) {
                return Optional.of(readOnlyToolService.pendingTaskItems());
            }
            if (isTaskItemByTaskListQuestion(normalized)) {
                return Optional.of(readOnlyToolService.taskItemsByTaskList(question));
            }
            return Optional.of(readOnlyToolService.taskItemSearch(question));
        }

        if (isTaskListToolQuestion(normalized)) {
            if (isTaskListProgressQuestion(normalized)) {
                return Optional.of(readOnlyToolService.taskListProgressSummary());
            }
            if (isTaskListCompletionQuestion(normalized)) {
                return Optional.of(readOnlyToolService.taskListCompletionSummary());
            }
            if (isTaskListDueTodayQuestion(normalized)) {
                return Optional.of(readOnlyToolService.dueTodayTaskLists());
            }
            if (isTaskListDueThisWeekQuestion(normalized)) {
                return Optional.of(readOnlyToolService.dueThisWeekTaskLists());
            }
            if (isTaskListOverdueQuestion(normalized)) {
                return Optional.of(readOnlyToolService.overdueTaskLists());
            }
            if (isTaskListCompletedQuestion(normalized)) {
                return Optional.of(readOnlyToolService.completedTaskLists());
            }
            if (isTaskListForNextReservationQuestion(normalized)) {
                return Optional.of(readOnlyToolService.taskListsForNextReservation());
            }
            if (isTaskListByReservationQuestion(normalized)) {
                return Optional.of(readOnlyToolService.taskListsByReservation(question));
            }
            if (isTaskListByPropertyQuestion(normalized)) {
                return Optional.of(readOnlyToolService.taskListsByProperty(question));
            }
            if (isTaskListActiveQuestion(normalized) || isPendingTaskQuestion(normalized)) {
                return Optional.of(readOnlyToolService.activeTaskLists());
            }
            return Optional.of(readOnlyToolService.taskListSearch(question));
        }

        return Optional.empty();
    }
}
