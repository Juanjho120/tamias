package com.tamias.ai.tool;

import com.tamias.ai.dto.AiChatRequest;
import com.tamias.ai.dto.AiToolEvidenceResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AiToolCallingService {

    private final AiReadOnlyToolService readOnlyToolService;

    public AiToolCallingService(AiReadOnlyToolService readOnlyToolService) {
        this.readOnlyToolService = readOnlyToolService;
    }

    public Optional<AiToolAnswer> tryHandle(AiChatRequest request) {
        return tryHandleResult(request).answerOptional();
    }

    public AiToolResult tryHandleResult(AiChatRequest request) {
        return tryHandleInternal(request)
                .map(AiToolResult::hit)
                .orElseGet(AiToolResult::notApplicable);
    }

    private Optional<AiToolAnswer> tryHandleInternal(AiChatRequest request) {
        String question = request.question();
        String normalized = normalize(question);

        if (isUnsupportedWriteAction(normalized)) {
            return Optional.of(readOnlyGuard());
        }
        if (isCapabilitiesQuestion(normalized)) {
            return Optional.of(readOnlyToolService.capabilities());
        }
        if (isCurrentUserProfileQuestion(normalized)) {
            return Optional.of(readOnlyToolService.currentUserProfile(question));
        }

        Optional<AiToolAnswer> adminRoleOrganizationAnswer = tryHandleAdminRoleOrganizationQuestion(question, normalized);
        if (adminRoleOrganizationAnswer.isPresent()) {
            return adminRoleOrganizationAnswer;
        }

        if (isOrganizationQuestion(normalized)) {
            return Optional.of(readOnlyToolService.currentOrganizationSummary());
        }

        Optional<AiToolAnswer> aiChatHistoryAnswer = tryHandleAiChatHistoryQuestion(request, question, normalized);
        if (aiChatHistoryAnswer.isPresent()) {
            return aiChatHistoryAnswer;
        }

        Optional<AiToolAnswer> fileImageDashboardAnswer = tryHandleFileImageDashboardQuestion(question, normalized);
        if (fileImageDashboardAnswer.isPresent()) {
            return fileImageDashboardAnswer;
        }

        Optional<AiToolAnswer> priorityMaintenanceAnalyticsAnswer = tryHandlePriorityMaintenanceAnalyticsQuestion(question, normalized);
        if (priorityMaintenanceAnalyticsAnswer.isPresent()) {
            return priorityMaintenanceAnalyticsAnswer;
        }

        Optional<AiToolAnswer> reservationSupplyTaskAnswer = tryHandleReservationSupplyAndTaskQuestion(question, normalized);
        if (reservationSupplyTaskAnswer.isPresent()) {
            return reservationSupplyTaskAnswer;
        }

        Optional<AiToolAnswer> purchaseAnalyticsAnswer = tryHandlePurchaseAnalyticsQuestion(question, normalized);
        if (purchaseAnalyticsAnswer.isPresent()) {
            return purchaseAnalyticsAnswer;
        }

        Optional<AiToolAnswer> documentRagAnswer = tryHandleDocumentAndRagQuestion(question, normalized);
        if (documentRagAnswer.isPresent()) {
            return documentRagAnswer;
        }

        Optional<AiToolAnswer> inventoryAnswer = tryHandleInventoryQuestion(question, normalized);
        if (inventoryAnswer.isPresent()) {
            return inventoryAnswer;
        }

        Optional<AiToolAnswer> scheduledReservationGuestAnswer = tryHandleScheduledReservationGuestQuestion(question, normalized);
        if (scheduledReservationGuestAnswer.isPresent()) {
            return scheduledReservationGuestAnswer;
        }

        Optional<AiToolAnswer> assistantAnswer = tryHandleAssistantLevelQuestion(question, normalized);
        if (assistantAnswer.isPresent()) {
            return assistantAnswer;
        }

        Optional<AiToolAnswer> propertyAnswer = tryHandlePropertyQuestion(question, normalized);
        if (propertyAnswer.isPresent()) {
            return propertyAnswer;
        }

        Optional<AiToolAnswer> catalogAnswer = tryHandleCatalogQuestion(question, normalized);
        if (catalogAnswer.isPresent()) {
            return catalogAnswer;
        }

        Optional<AiToolAnswer> maintenanceAnalyticsAnswer = tryHandleMaintenanceAnalyticsQuestion(question, normalized);
        if (maintenanceAnalyticsAnswer.isPresent()) {
            return maintenanceAnalyticsAnswer;
        }

        if (isRagHealthQuestion(normalized)) {
            return Optional.of(readOnlyToolService.ragDocumentIndexStatus());
        }
        if (isDocumentMetadataQuestion(normalized)) {
            return Optional.of(readOnlyToolService.documentMetadata(question));
        }
        if (isOperationalSummaryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.operationalSummary());
        }
        if (isUpcomingReservationQuestion(normalized)) {
            return Optional.of(readOnlyToolService.upcomingReservations());
        }
        if (isLastMaintenanceQuestion(normalized)) {
            return Optional.of(readOnlyToolService.lastPerformedMaintenance(question));
        }
        if (isOverdueScheduledMaintenanceQuestion(normalized)) {
            return Optional.of(readOnlyToolService.overdueScheduledMaintenance());
        }
        if (isLastPurchaseQuestion(normalized)) {
            return Optional.of(readOnlyToolService.lastPurchasedItem(question));
        }
        if (isPendingTaskQuestion(normalized)) {
            return Optional.of(readOnlyToolService.pendingTaskLists());
        }

        return Optional.empty();
    }







    private Optional<AiToolAnswer> tryHandleAiChatHistoryQuestion(AiChatRequest request, String question, String normalized) {
        if (!isAiChatHistoryQuestion(normalized)) {
            return Optional.empty();
        }
        if (isAiChatUsageSummaryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.aiChatUsageSummary());
        }
        if (isAiChatCurrentSessionQuestion(normalized)) {
            return Optional.of(readOnlyToolService.aiChatCurrentSessionSummary(request.chatSessionId()));
        }
        if (isAiChatByPropertyQuestion(normalized)) {
            return Optional.of(readOnlyToolService.aiChatSessionsByProperty(question, request.chatSessionId()));
        }
        if (isAiChatRecentMessagesQuestion(normalized)) {
            return Optional.of(readOnlyToolService.aiChatRecentUserQuestions(request.chatSessionId()));
        }
        if (isAiChatSearchHistoryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.aiChatSearchHistory(question, request.chatSessionId()));
        }
        return Optional.of(readOnlyToolService.aiChatRecentSessions(request.chatSessionId()));
    }

    private Optional<AiToolAnswer> tryHandleAdminRoleOrganizationQuestion(String question, String normalized) {
        if (isOrganizationAdminToolQuestion(normalized)) {
            if (isOrganizationModuleUsageQuestion(normalized)) {
                return Optional.of(readOnlyToolService.organizationModuleUsageSummary());
            }
            if (isOrganizationUserCountQuestion(normalized)) {
                return Optional.of(readOnlyToolService.organizationUserCount());
            }
        }

        if (isRolePermissionSummaryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.rolePermissionSummary(question));
        }

        if (isUserAdminToolQuestion(normalized)) {
            if (isUserAccessSummaryQuestion(normalized)) {
                return Optional.of(readOnlyToolService.userAccessSummary(question));
            }
            if (isUsersByRoleQuestion(normalized)) {
                return Optional.of(readOnlyToolService.usersByRole(question));
            }
            if (isActiveUsersQuestion(normalized)) {
                return Optional.of(readOnlyToolService.activeUsers());
            }
            if (isInactiveUsersQuestion(normalized)) {
                return Optional.of(readOnlyToolService.inactiveUsers());
            }
            return Optional.of(readOnlyToolService.searchUsers(question));
        }

        if (isRoleAdminToolQuestion(normalized)) {
            return Optional.of(readOnlyToolService.roleList());
        }

        return Optional.empty();
    }

    private Optional<AiToolAnswer> tryHandleFileImageDashboardQuestion(String question, String normalized) {
        if (isDashboardAnalyticsQuestion(normalized)) {
            if (isDashboardAttentionTodayQuestion(normalized)) {
                return Optional.of(readOnlyToolService.dashboardAttentionToday());
            }
            if (isDashboardAlertQuestion(normalized)) {
                return Optional.of(readOnlyToolService.dashboardAlertSummary());
            }
            if (isDashboardCalendarQuestion(normalized)) {
                return Optional.of(readOnlyToolService.dashboardCalendarEvents());
            }
            if (isDashboardReservationSummaryQuestion(normalized)) {
                return Optional.of(readOnlyToolService.dashboardReservationSummary());
            }
            if (isDashboardMaintenanceSummaryQuestion(normalized)) {
                return Optional.of(readOnlyToolService.dashboardMaintenanceSummary());
            }
            if (isDashboardPurchaseSummaryQuestion(normalized)) {
                return Optional.of(readOnlyToolService.dashboardPurchaseSummary());
            }
            if (isDashboardTaskSummaryQuestion(normalized)) {
                return Optional.of(readOnlyToolService.dashboardTaskSummary());
            }
            if (isDashboardDocumentSummaryQuestion(normalized)) {
                return Optional.of(readOnlyToolService.dashboardDocumentSummary());
            }
            if (isOperationalSummaryQuestion(normalized)) {
                return Optional.of(readOnlyToolService.operationalSummary());
            }
            List<AiToolAnswer> answers = List.of(
                    readOnlyToolService.operationalSummary(),
                    readOnlyToolService.dashboardReservationSummary(),
                    readOnlyToolService.dashboardMaintenanceSummary(),
                    readOnlyToolService.dashboardTaskSummary(),
                    readOnlyToolService.dashboardAlertSummary()
            );
            return Optional.of(combine(
                    "dashboard.executiveSummary",
                    "Executive dashboard summary",
                    "Operational, reservation, maintenance, task and alert summaries were consulted together.",
                    "Te dejo una vista ejecutiva de la operación con datos del sistema.",
                    answers
            ));
        }

        if (isImageMetadataQuestion(normalized)) {
            if (isMaintenanceImageMetadataQuestion(normalized)) {
                return Optional.of(readOnlyToolService.maintenanceImageMetadataSummary());
            }
            if (isPropertyImageMetadataQuestion(normalized)) {
                return Optional.of(readOnlyToolService.propertyImageMetadataSummary());
            }
        }

        if (isFileMetadataQuestion(normalized)) {
            if (isFileStorageSummaryQuestion(normalized)) {
                return Optional.of(readOnlyToolService.fileStorageSummary());
            }
            if (isFileOrphanCandidateQuestion(normalized)) {
                return Optional.of(readOnlyToolService.orphanFileCandidates());
            }
            if (isFileByMaintenanceQuestion(normalized)) {
                return Optional.of(readOnlyToolService.filesByMaintenance(question));
            }
            if (isFileByDocumentQuestion(normalized)) {
                return Optional.of(readOnlyToolService.filesByDocument(question));
            }
            if (isFileByPropertyQuestion(normalized)) {
                return Optional.of(readOnlyToolService.filesByProperty(question));
            }
            return Optional.of(readOnlyToolService.fileMetadata(question));
        }

        return Optional.empty();
    }

    private Optional<AiToolAnswer> tryHandleDocumentAndRagQuestion(String question, String normalized) {
        if (isRagChunkSummaryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.ragChunkSummary());
        }
        if (isRagMissingChunksQuestion(normalized)) {
            return Optional.of(readOnlyToolService.documentsMissingChunks());
        }
        if (isRagMissingVectorIdsQuestion(normalized)) {
            return Optional.of(readOnlyToolService.documentsMissingVectorIds());
        }
        if (isRagCoverageSummaryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.ragIndexCoverageSummary());
        }
        if (isRagHealthQuestion(normalized)) {
            return Optional.of(readOnlyToolService.ragDocumentIndexStatus());
        }
        if (!isDocumentToolQuestion(normalized)) {
            return Optional.empty();
        }
        if (isDocumentBlueprintQuestion(normalized)) {
            return Optional.of(readOnlyToolService.findBlueprintDocuments());
        }
        if (isDocumentHouseRulesQuestion(normalized)) {
            return Optional.of(readOnlyToolService.findHouseRulesDocuments());
        }
        if (isDocumentManualQuestion(normalized)) {
            return Optional.of(readOnlyToolService.findManualDocuments());
        }
        if (isDocumentCountByTypeQuestion(normalized)) {
            return Optional.of(readOnlyToolService.documentCountByType());
        }
        if (isDocumentCountByPropertyQuestion(normalized)) {
            return Optional.of(readOnlyToolService.documentCountByProperty());
        }
        if (isDocumentProcessedNotIndexedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.processedNotIndexedDocuments());
        }
        if (isDocumentNotIndexedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.notIndexedDocuments());
        }
        if (isDocumentIndexedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.indexedDocuments());
        }
        if (isDocumentFailedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.failedDocuments());
        }
        if (isDocumentUnprocessedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.unprocessedDocuments());
        }
        if (isDocumentProcessedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.processedDocuments());
        }
        if (isDocumentRecentQuestion(normalized)) {
            return Optional.of(readOnlyToolService.recentDocuments());
        }
        if (isDocumentByPropertyQuestion(normalized)) {
            return Optional.of(readOnlyToolService.documentByProperty(question));
        }
        if (isDocumentByTypeQuestion(normalized)) {
            return Optional.of(readOnlyToolService.documentByType(question));
        }
        if (isDocumentByStatusQuestion(normalized)) {
            return Optional.of(readOnlyToolService.documentByStatus(question));
        }
        return Optional.of(readOnlyToolService.documentMetadata(question));
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



    private Optional<AiToolAnswer> tryHandlePurchaseAnalyticsQuestion(String question, String normalized) {
        if (!isPurchaseAnalyticsQuestion(normalized)) {
            return Optional.empty();
        }
        if (isLastPurchaseQuestion(normalized)) {
            return Optional.of(readOnlyToolService.lastPurchasedItem(question));
        }
        if (isPurchaseItemCostTrendQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseItemCostTrend(question));
        }
        if (isPurchaseItemAverageUnitCostQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseItemAverageUnitCost(question));
        }
        if (isPurchaseItemPriceHistoryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseItemPriceHistory(question));
        }
        if (isPurchaseItemMostPurchasedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseItemMostPurchased());
        }
        if (isPurchaseItemLeastPurchasedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseItemLeastPurchased());
        }
        if (isPurchaseItemQuantitySummaryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseItemQuantitySummary(question));
        }
        if (isPurchaseCostByPropertyQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseCostByProperty());
        }
        if (isPurchaseCostByCategoryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseCostByCategory());
        }
        if (isPurchaseCostByMonthQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseCostByMonth());
        }
        if (isPurchaseCostSummaryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseCostSummary(question));
        }
        if (isPurchaseListPendingQuestion(normalized)) {
            return Optional.of(readOnlyToolService.pendingPurchaseLists());
        }
        if (isPurchaseListCompletedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.completedPurchaseLists());
        }
        if (isPurchaseListByPropertyQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseListsByProperty(question));
        }
        if (isPurchaseListRecentQuestion(normalized)) {
            return Optional.of(readOnlyToolService.recentPurchaseLists());
        }
        if (isPurchaseItemListQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseItemSearch(question));
        }
        return Optional.of(readOnlyToolService.purchaseListSearch(question));
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

    private Optional<AiToolAnswer> tryHandlePriorityMaintenanceAnalyticsQuestion(String question, String normalized) {
        if (!isMaintenanceAnalyticsQuestion(normalized)) {
            return Optional.empty();
        }
        if (isMaintenanceCostByPropertyQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceCostByProperty());
        }
        if (isMaintenanceCostByCategoryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceCostByCategory());
        }
        if (isMaintenanceCostByMonthQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceCostByMonth());
        }
        if (isMaintenanceCostQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceCostSummary(question));
        }
        if (isMaintenanceImageQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceImagesSummary(containsAny(normalized, "sin imagen", "sin imagenes", "no tienen imagen", "no tiene imagen", "sin evidencia", "no tienen evidencia", "no tiene evidencia")));
        }
        if (isMaintenanceItemUsageQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryMaintenanceUsage(question));
        }
        return Optional.empty();
    }

    private Optional<AiToolAnswer> tryHandleAssistantLevelQuestion(String question, String normalized) {
        if (isPreparationQuestion(normalized)) {
            List<AiToolAnswer> answers = List.of(
                    readOnlyToolService.upcomingReservations(),
                    readOnlyToolService.overdueScheduledMaintenance(),
                    readOnlyToolService.pendingTaskLists()
            );
            return Optional.of(combine(
                    "assistant.operationalPreparation",
                    "Operational preparation assistant",
                    "Upcoming reservations, overdue scheduled maintenance and pending tasks were consulted together.",
                    "Revisé tus próximas reservaciones, mantenimientos vencidos y tareas pendientes para darte una visión rápida de preparación.",
                    answers
            ));
        }
        if (isOperationalPlanningQuestion(normalized)) {
            List<AiToolAnswer> answers = List.of(
                    readOnlyToolService.operationalSummary(),
                    readOnlyToolService.upcomingReservations(),
                    readOnlyToolService.overdueScheduledMaintenance(),
                    readOnlyToolService.pendingTaskLists()
            );
            return Optional.of(combine(
                    "assistant.operationalPlanning",
                    "Operational planning assistant",
                    "Operational summary, upcoming reservations, overdue scheduled maintenance and pending tasks were consulted together.",
                    "Te dejo un panorama operativo combinando dashboard, próximas reservaciones, mantenimientos vencidos y tareas pendientes.",
                    answers
            ));
        }
        if (isDocumentInventoryQuestion(normalized)) {
            List<AiToolAnswer> answers = List.of(
                    readOnlyToolService.documentMetadata(question),
                    readOnlyToolService.ragDocumentIndexStatus()
            );
            return Optional.of(combine(
                    "assistant.documentOverview",
                    "Document overview assistant",
                    "Document metadata and RAG index status were consulted together.",
                    "Revisé tus documentos cargados y el estado del índice RAG para separar archivos disponibles de contenido ya indexado para IA.",
                    answers
            ));
        }
        if (isPropertyOperationsQuestion(normalized)) {
            List<AiToolAnswer> answers = List.of(
                    readOnlyToolService.propertyOperationalOverview(),
                    readOnlyToolService.upcomingReservations(),
                    readOnlyToolService.overdueScheduledMaintenance(),
                    readOnlyToolService.pendingTaskLists()
            );
            return Optional.of(combine(
                    "assistant.propertyOperations",
                    "Property operations assistant",
                    "Properties, upcoming reservations, overdue scheduled maintenance and pending tasks were consulted together.",
                    "Conecté la información de propiedades con reservaciones, mantenimientos vencidos y tareas pendientes para darte contexto operativo.",
                    answers
            ));
        }
        return Optional.empty();
    }

    private Optional<AiToolAnswer> tryHandlePropertyQuestion(String question, String normalized) {
        if (!isPropertyQuestion(normalized)) {
            return Optional.empty();
        }
        if (isActivePropertyQuestion(normalized)) {
            return Optional.of(readOnlyToolService.activeProperties());
        }
        if (isInactivePropertyQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inactiveProperties());
        }
        if (isPropertyImagesQuestion(normalized)) {
            return Optional.of(readOnlyToolService.propertyImagesSummary(question));
        }
        if (isPropertyOperationalOverviewQuestion(normalized)) {
            return Optional.of(readOnlyToolService.propertyOperationalOverview());
        }
        if (isPropertySummaryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.propertySummary(question));
        }
        return Optional.of(readOnlyToolService.searchProperties(question));
    }

    private Optional<AiToolAnswer> tryHandleCatalogQuestion(String question, String normalized) {
        if (!isCatalogQuestion(normalized)) {
            return Optional.empty();
        }
        if (isMaintenanceCatalogOverviewQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceCatalogOverview());
        }
        if (isMaintenanceCategoryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceCategories());
        }
        if (isMaintenanceTypeQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceTypes());
        }
        if (isReservationPlatformQuestion(normalized)) {
            return Optional.of(readOnlyToolService.reservationPlatforms());
        }
        if (isTaskCategoryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.taskCategories());
        }
        if (isPurchaseCategoryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.purchaseCategories());
        }
        if (isInventoryItemTypeQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryItemTypes());
        }
        return Optional.of(readOnlyToolService.catalogSearch(question));
    }


    private Optional<AiToolAnswer> tryHandleInventoryQuestion(String question, String normalized) {
        if (!isInventoryQuestion(normalized) && !isInventoryWhereUsedQuestion(normalized)) {
            return Optional.empty();
        }
        if (isInventoryWhereUsedQuestion(normalized)) {
            List<AiToolAnswer> answers = List.of(
                    readOnlyToolService.inventoryMaintenanceUsage(question),
                    readOnlyToolService.inventoryReservationUsage(question),
                    readOnlyToolService.inventoryPurchaseUsage(question)
            );
            return Optional.of(combine(
                    "inventory.whereUsed",
                    "Inventory where-used lookup",
                    "Maintenance, reservation and purchase usage were consulted for the requested item.",
                    "Busqué dónde aparece ese item dentro de mantenimientos, reservaciones y compras.",
                    answers
            ));
        }
        if (isInventoryUnusedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryUnusedItems());
        }
        if (isInventoryReservationUsageQuestion(normalized) && isInventoryFrequentlyUsedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryReservationUsage(""));
        }
        if (isInventoryMaintenanceUsageQuestion(normalized) && isInventoryFrequentlyUsedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryMaintenanceUsage(""));
        }
        if (isInventoryPurchaseUsageQuestion(normalized) && isInventoryFrequentlyUsedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryPurchaseUsage(""));
        }
        if (isInventoryReservationUsageQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryReservationUsage(question));
        }
        if (isInventoryMaintenanceUsageQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryMaintenanceUsage(question));
        }
        if (isInventoryPurchaseUsageQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryPurchaseUsage(question));
        }
        if (isInventoryFrequentlyUsedQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryFrequentlyUsed());
        }
        return Optional.of(readOnlyToolService.inventorySearch(question));
    }

    private Optional<AiToolAnswer> tryHandleMaintenanceAnalyticsQuestion(String question, String normalized) {
        if (!isMaintenanceAnalyticsQuestion(normalized)) {
            return Optional.empty();
        }
        if (isMaintenanceImageQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceImagesSummary(containsAny(normalized, "sin imagen", "sin imagenes", "no tienen imagen", "no tiene imagen", "sin evidencia", "no tienen evidencia", "no tiene evidencia")));
        }
        if (isMaintenanceCostByPropertyQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceCostByProperty());
        }
        if (isMaintenanceCostByCategoryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceCostByCategory());
        }
        if (isMaintenanceCostByMonthQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceCostByMonth());
        }
        if (isMaintenanceCostQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceCostSummary(question));
        }
        if (isMaintenanceStatusQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceByStatus(question));
        }
        if (isMaintenancePropertyFilterQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceByProperty(question));
        }
        if (isMaintenanceCategoryOrTypeFilterQuestion(normalized)) {
            return Optional.of(readOnlyToolService.maintenanceByCategoryOrType(question));
        }
        if (isMaintenanceItemUsageQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inventoryMaintenanceUsage(question));
        }
        if (isMaintenanceRecentQuestion(normalized)) {
            return Optional.of(readOnlyToolService.recentMaintenance());
        }
        return Optional.of(readOnlyToolService.maintenanceSearch(question));
    }

    private AiToolAnswer combine(
            String toolName,
            String label,
            String summary,
            String intro,
            List<AiToolAnswer> answers
    ) {
        StringBuilder builder = new StringBuilder(intro).append("\n\n");
        List<AiToolEvidenceResponse> evidence = new ArrayList<>();
        for (AiToolAnswer answer : answers) {
            evidence.addAll(answer.evidence());
            builder.append("### ").append(resolveSectionTitle(answer)).append("\n")
                    .append(answer.answer())
                    .append("\n\n");
        }
        builder.append("No hice cambios en tus datos; esta respuesta solo consulta información existente en TAMIAS.");
        evidence.add(new AiToolEvidenceResponse(toolName, label, summary, List.of()));
        return new AiToolAnswer(builder.toString().trim(), true, evidence);
    }

    private String resolveSectionTitle(AiToolAnswer answer) {
        if (answer.evidence().isEmpty()) {
            return "Resultado consultado";
        }
        String toolName = answer.evidence().get(0).toolName();
        return switch (toolName) {
            case "dashboard.operationalSummary" -> "Resumen operativo";
            case "reservation.upcoming" -> "Reservaciones próximas";
            case "scheduledMaintenance.search" -> "Mantenimientos programados";
            case "scheduledMaintenance.upcoming" -> "Mantenimientos programados próximos";
            case "scheduledMaintenance.dueToday" -> "Mantenimientos programados de hoy";
            case "scheduledMaintenance.dueThisWeek" -> "Mantenimientos programados de esta semana";
            case "scheduledMaintenance.byProperty" -> "Mantenimientos programados por propiedad";
            case "scheduledMaintenance.byType" -> "Mantenimientos programados por tipo";
            case "scheduledMaintenance.byStatus" -> "Mantenimientos programados por estado";
            case "scheduledMaintenance.nextDue" -> "Próximo mantenimiento programado";
            case "scheduledMaintenance.frequencySummary" -> "Frecuencias de mantenimiento programado";
            case "scheduledMaintenance.history" -> "Historial de mantenimiento programado";
            case "scheduledMaintenance.complianceSummary" -> "Cumplimiento de mantenimiento programado";
            case "reservation.search" -> "Reservaciones";
            case "reservation.current" -> "Reservaciones actuales";
            case "reservation.today" -> "Reservaciones de hoy";
            case "reservation.thisWeek" -> "Reservaciones de esta semana";
            case "reservation.thisMonth" -> "Reservaciones de este mes";
            case "reservation.byProperty" -> "Reservaciones por propiedad";
            case "reservation.byGuest" -> "Reservaciones por huésped";
            case "reservation.byStatus" -> "Reservaciones por estado";
            case "reservation.byPlatform" -> "Reservaciones por plataforma";
            case "reservation.occupancySummary" -> "Ocupación de reservaciones";
            case "reservation.revenueSummary" -> "Ingresos de reservaciones";
            case "reservation.nightsSummary" -> "Noches reservadas";
            case "reservation.guestCountSummary" -> "Conteo de huéspedes";
            case "reservation.calendarEvents" -> "Calendario de reservaciones";
            case "reservation.nextCheckIn" -> "Próxima llegada";
            case "reservation.nextCheckOut" -> "Próxima salida";
            case "reservation.gapsBetweenReservations" -> "Espacios entre reservaciones";
            case "guest.search" -> "Huéspedes";
            case "guest.byReservation" -> "Huéspedes por reservación";
            case "guest.recent" -> "Huéspedes recientes";
            case "guest.returningGuests" -> "Huéspedes recurrentes";
            case "guest.upcomingGuests" -> "Próximos huéspedes";
            case "guest.countByDateRange" -> "Conteo de huéspedes";
            case "scheduledMaintenance.overdue" -> "Mantenimientos programados vencidos";
            case "taskList.pending" -> "Tareas pendientes";
            case "document.searchMetadata" -> "Documentos cargados";
            case "rag.documentIndexStatus" -> "Índice RAG";
            case "property.search" -> "Propiedades";
            case "property.getSummary" -> "Resumen de propiedad";
            case "property.getOperationalOverview" -> "Panorama por propiedad";
            case "property.getImagesSummary" -> "Imágenes por propiedad";
            case "property.getActiveProperties" -> "Propiedades activas";
            case "property.getInactiveProperties" -> "Propiedades inactivas";
            case "catalog.maintenanceCategories" -> "Categorías de mantenimiento";
            case "catalog.maintenanceTypes" -> "Tipos de mantenimiento";
            case "catalog.reservationPlatforms" -> "Plataformas de reservación";
            case "catalog.taskCategories" -> "Plantillas/categorías de tareas";
            case "catalog.purchaseCategories" -> "Categorías de compras";
            case "catalog.inventoryItemTypes" -> "Tipos de items de inventario";
            case "catalog.maintenanceOverview" -> "Catálogos para mantenimiento";
            case "catalog.search" -> "Catálogos";
            case "maintenance.lastPerformed" -> "Último mantenimiento";
            case "inventory.search" -> "Inventario";
            case "inventory.getFrequentlyUsed" -> "Items más usados";
            case "inventory.getUnusedItems" -> "Items sin uso";
            case "inventory.getItemsUsedInReservations" -> "Items usados en reservaciones";
            case "inventory.getItemsUsedInPurchases" -> "Items usados en compras";
            case "inventory.getItemsUsedInMaintenance" -> "Items usados en mantenimientos";
            case "maintenance.search" -> "Mantenimientos";
            case "maintenance.recent" -> "Mantenimientos recientes";
            case "maintenance.byStatus" -> "Mantenimientos por estado";
            case "maintenance.byProperty" -> "Mantenimientos por propiedad";
            case "maintenance.byCategoryOrType" -> "Mantenimientos por categoría/tipo";
            case "maintenance.costSummary" -> "Costos de mantenimiento";
            case "maintenance.costByProperty" -> "Costos por propiedad";
            case "maintenance.costByCategory" -> "Costos por categoría";
            case "maintenance.costByMonth" -> "Costos por mes";
            case "maintenance.withImages" -> "Mantenimientos con imágenes";
            case "maintenance.withoutImages" -> "Mantenimientos sin imágenes";
            case "purchaseItem.lastPurchased" -> "Última compra";
            case "inventory.whereUsed" -> "Dónde se ha usado";
            case "file.searchMetadata" -> "Archivos";
            case "file.byProperty" -> "Archivos por propiedad";
            case "file.byMaintenance" -> "Archivos de mantenimiento";
            case "file.byDocument" -> "Archivos de documentos";
            case "file.storageSummary" -> "Almacenamiento de archivos";
            case "file.orphanFileCandidates" -> "Candidatos de archivos huérfanos";
            case "image.propertyImagesSummary" -> "Imágenes de propiedades";
            case "image.maintenanceImagesSummary" -> "Imágenes de mantenimientos";
            case "dashboard.reservationSummary" -> "Dashboard de reservaciones";
            case "dashboard.maintenanceSummary" -> "Dashboard de mantenimiento";
            case "dashboard.purchaseSummary" -> "Dashboard de compras";
            case "dashboard.taskSummary" -> "Dashboard de tareas";
            case "dashboard.documentSummary" -> "Dashboard de documentos";
            case "dashboard.calendarEvents" -> "Eventos del calendario";
            case "dashboard.alertSummary" -> "Alertas operativas";
            case "dashboard.executiveSummary" -> "Dashboard ejecutivo";
            case "aiChat.recentSessions" -> "Sesiones recientes del asistente IA";
            case "aiChat.searchHistory" -> "Búsqueda en historial IA";
            case "aiChat.recentMessages" -> "Mensajes recientes del asistente IA";
            case "aiChat.sessionsByProperty" -> "Sesiones IA por propiedad";
            case "aiChat.currentSessionSummary" -> "Resumen de la sesión IA actual";
            case "aiChat.usageSummary" -> "Uso del historial IA";
            default -> answer.evidence().get(0).label();
        };
    }

    private AiToolAnswer readOnlyGuard() {
        return AiToolAnswer.of(
                "Puedo ayudarte a revisar información que ya existe en TAMIAS, pero por seguridad todavía no creo, edito, elimino ni envío datos desde el asistente.\n"
                        + "Dime qué quieres revisar y te ayudo a encontrarlo para que tú hagas el cambio desde el módulo correspondiente.",
                "assistant.readOnlyGuard",
                "Read-only guard",
                "The user asked for an action that would modify data. The assistant refused autonomous writes.",
                List.of()
        );
    }


    private boolean isFileMetadataQuestion(String value) {
        return containsAny(value,
                "archivo", "archivos", "file", "files", "metadata de archivos", "metadatos de archivos", "almacenados", "almacenamiento", "bucket", "s3"
        ) && !isDocumentCountByTypeQuestion(value) && !isDocumentCountByPropertyQuestion(value);
    }

    private boolean isFileByPropertyQuestion(String value) {
        return isFileMetadataQuestion(value) && containsAny(value, "propiedad", "propiedades", "alojamiento", "casa", "bungalow", "asociados a esta propiedad", "para esta propiedad");
    }

    private boolean isFileByMaintenanceQuestion(String value) {
        return isFileMetadataQuestion(value) && containsAny(value, "mantenimiento", "mantenimientos", "evidencia", "fotos de mantenimiento", "imagenes de mantenimiento");
    }

    private boolean isFileByDocumentQuestion(String value) {
        return isFileMetadataQuestion(value) && containsAny(value, "documento", "documentos", "pdf", "manual", "plano", "regla");
    }

    private boolean isFileStorageSummaryQuestion(String value) {
        return isFileMetadataQuestion(value) && containsAny(value, "cuantos archivos", "cuántos archivos", "almacenados", "almacenamiento", "storage", "tamano", "tamaño", "peso", "espacio");
    }

    private boolean isFileOrphanCandidateQuestion(String value) {
        return isFileMetadataQuestion(value) && containsAny(value, "huerfano", "huérfano", "huerfanos", "huérfanos", "orphan", "sin asociar", "no asociados");
    }

    private boolean isImageMetadataQuestion(String value) {
        return containsAny(value, "imagen", "imagenes", "foto", "fotos", "evidencia fotografica", "evidencia fotográfica");
    }

    private boolean isPropertyImageMetadataQuestion(String value) {
        return isImageMetadataQuestion(value) && containsAny(value, "propiedad", "propiedades", "portada", "portadas", "casas", "bungalow");
    }

    private boolean isMaintenanceImageMetadataQuestion(String value) {
        return isImageMetadataQuestion(value) && containsAny(value, "mantenimiento", "mantenimientos", "evidencia");
    }

    private boolean isDashboardAnalyticsQuestion(String value) {
        return containsAny(value,
                "dashboard", "tablero", "resumen ejecutivo", "resumen operativo", "estado general", "alertas", "atencion hoy", "atención hoy",
                "calendario operativo", "eventos operativos", "summary", "analytics", "analitica", "analítica"
        );
    }

    private boolean isDashboardReservationSummaryQuestion(String value) {
        return isDashboardAnalyticsQuestion(value) && containsAny(value, "reservacion", "reservaciones", "reserva", "reservas", "check in", "check-in", "ocupacion", "ocupación");
    }

    private boolean isDashboardMaintenanceSummaryQuestion(String value) {
        return isDashboardAnalyticsQuestion(value) && containsAny(value, "mantenimiento", "mantenimientos", "reparacion", "reparaciones");
    }

    private boolean isDashboardPurchaseSummaryQuestion(String value) {
        return isDashboardAnalyticsQuestion(value) && containsAny(value, "compra", "compras", "gasto", "gastos", "supplies");
    }

    private boolean isDashboardTaskSummaryQuestion(String value) {
        return isDashboardAnalyticsQuestion(value) && containsAny(value, "tarea", "tareas", "task", "tasks", "pendiente", "pendientes");
    }

    private boolean isDashboardDocumentSummaryQuestion(String value) {
        return isDashboardAnalyticsQuestion(value) && containsAny(value, "documento", "documentos", "rag", "indice", "índice", "indexacion", "indexación");
    }

    private boolean isDashboardCalendarQuestion(String value) {
        return isDashboardAnalyticsQuestion(value) && containsAny(value, "calendario", "calendar", "eventos", "agenda", "proximos eventos", "próximos eventos");
    }

    private boolean isDashboardAttentionTodayQuestion(String value) {
        return isDashboardAnalyticsQuestion(value)
                && containsAny(value, "necesita atencion hoy", "necesita atención hoy", "que necesita atencion", "qué necesita atención", "atencion hoy", "atención hoy");
    }

    private boolean isDashboardAlertQuestion(String value) {
        return isDashboardAnalyticsQuestion(value) && containsAny(value, "alerta", "alertas", "vencido", "vencidos", "fallido", "fallidos", "riesgo", "riesgos");
    }



    private boolean isUserAdminToolQuestion(String value) {
        boolean asksAboutUsers = containsAny(
                value,
                "usuario", "usuarios", "user", "users", "miembro", "miembros", "equipo",
                "administrador", "administradores"
        );

        return (asksAboutUsers || isUserAccessIntent(value))
                && !isCurrentUserProfileQuestion(value)
                && !isOrganizationUserCountQuestion(value)
                && !isRolePermissionSummaryQuestion(value);
    }



    private boolean isUserAccessIntent(String value) {
        boolean accessWords = containsAny(
                value,
                "acceso", "accesos", "access summary", "resumen de acceso", "resumen de accesos"
        );
        boolean permissionWords = containsAny(value, "permiso", "permisos");
        boolean userScope = containsAny(
                value,
                "usuario", "usuarios", "este usuario", "mi usuario", "mi cuenta",
                "tengo", "mis permisos", "mis accesos", "accesos tengo", "acceso tengo"
        );

        return accessWords || (permissionWords && userScope);
    }


    private boolean isActiveUsersQuestion(String value) {
        return isUserAdminToolQuestion(value)
                && containsAny(value, "activos", "activas", "activo", "active")
                && !containsAny(value, "inactivos", "inactivas", "no activos", "inactive");
    }

    private boolean isInactiveUsersQuestion(String value) {
        return isUserAdminToolQuestion(value)
                && containsAny(value, "inactivos", "inactivas", "no activos", "inactive", "bloqueados", "locked", "invited", "invitados");
    }


    private boolean isUsersByRoleQuestion(String value) {
        boolean asksAboutUsers = containsAny(value, "usuario", "usuarios", "user", "users", "miembro", "miembros", "equipo");
        boolean mentionsRole = containsAny(
                value,
                "rol", "roles", "administrador", "administradores", "administrator",
                "property manager", "maintenance staff", "read only", "solo lectura"
        );

        return asksAboutUsers
                && mentionsRole
                && !isCurrentUserProfileQuestion(value)
                && !isOrganizationUserCountQuestion(value);
    }


    private boolean isUserAccessSummaryQuestion(String value) {
        return isUserAdminToolQuestion(value) && isUserAccessIntent(value);
    }


    private boolean isRoleAdminToolQuestion(String value) {
        boolean asksForRoles = containsAny(
                value,
                "roles", "roles existentes", "lista de roles", "rol", "role"
        );
        boolean asksForPermissions = containsAny(value, "permiso", "permisos", "permission", "permissions", "que puede", "qué puede");
        boolean asksAboutUsers = containsAny(value, "usuario", "usuarios", "user", "users", "miembro", "miembros", "equipo");

        return asksForRoles
                && !asksForPermissions
                && !asksAboutUsers
                && !isCurrentUserProfileQuestion(value);
    }



    private boolean isRolePermissionSummaryQuestion(String value) {
        boolean asksForPermissions = containsAny(
                value,
                "permiso", "permisos", "permission", "permissions", "que puede", "qué puede"
        );
        boolean mentionsRoleContext = containsAny(
                value,
                "rol", "roles", "role", "administrator", "administrador", "maintenance staff",
                "mantenimiento", "property manager", "read only", "solo lectura"
        );
        boolean asksAboutUsers = containsAny(value, "usuario", "usuarios", "user", "users", "miembro", "miembros", "equipo");

        return asksForPermissions
                && mentionsRoleContext
                && !asksAboutUsers
                && !isCurrentUserProfileQuestion(value);
    }


    private boolean isOrganizationAdminToolQuestion(String value) {
        return containsAny(value, "organizacion", "organización", "empresa", "modulo", "modulos", "módulo", "módulos", "module", "modules")
                && containsAny(value, "usuarios", "usuario", "modulos", "módulos", "uso", "usando", "usamos", "module usage", "cuantos usuarios", "cuántos usuarios");
    }

    private boolean isOrganizationUserCountQuestion(String value) {
        return isOrganizationAdminToolQuestion(value)
                && containsAny(value, "usuarios", "cuantos usuarios", "cuántos usuarios", "user count", "cantidad de usuarios");
    }

    private boolean isOrganizationModuleUsageQuestion(String value) {
        return isOrganizationAdminToolQuestion(value)
                && containsAny(value, "modulo", "modulos", "módulo", "módulos", "uso", "usando", "usamos", "module", "modules", "module usage", "usando mas", "usamos mas");
    }



    private boolean isAiChatHistoryQuestion(String value) {
        return containsAny(
                value,
                "chat ia", "chats ia", "chats", "mis chats", "chat del asistente", "chats del asistente",
                "historial ia", "historial del asistente", "historial de chat", "historial de chats",
                "conversacion ia", "conversaciones ia", "conversacion con la ia", "conversaciones con la ia",
                "conversaciones anteriores", "conversacion anterior", "mis conversaciones", "esta conversacion", "conversacion actual",
                "sesion ia", "sesiones ia", "sesion de chat", "sesiones de chat", "sesiones del asistente", "esta sesion", "sesion actual",
                "mensajes del asistente", "preguntas al asistente", "preguntas le hice", "le hice al asistente", "que preguntas", "que he preguntado", "que hemos hablado",
                "hemos hablado antes", "hablamos", "pregunte", "preguntado", "resume esta", "resumeme esta", "resumen de esta"
        );
    }

    private boolean isAiChatUsageSummaryQuestion(String value) {
        return isAiChatHistoryQuestion(value)
                && containsAny(value, "cuantas", "cuantos", "conteo", "cantidad", "uso", "resumen de uso", "estadistica", "estadisticas");
    }

    private boolean isAiChatCurrentSessionQuestion(String value) {
        return isAiChatHistoryQuestion(value)
                && containsAny(value, "esta conversacion", "esta sesion", "sesion actual", "conversacion actual", "resumen de esta", "resume esta", "resumeme esta");
    }

    private boolean isAiChatByPropertyQuestion(String value) {
        return isAiChatHistoryQuestion(value)
                && containsAny(value, "propiedad", "propiedades", "alojamiento", "alojamientos", "casa", "casas", "bungalow", "bungalows");
    }

    private boolean isAiChatRecentMessagesQuestion(String value) {
        return isAiChatHistoryQuestion(value)
                && containsAny(value, "mensajes", "preguntas", "respuestas", "que he preguntado", "que pregunte", "preguntas le hice", "le hice al asistente", "que preguntas", "ultimos mensajes", "ultimas preguntas");
    }

    private boolean isAiChatSearchHistoryQuestion(String value) {
        return isAiChatHistoryQuestion(value)
                && containsAny(value, "busca", "buscar", "sobre", "relacionado", "relacionados", "mencione", "mencionamos", "hablamos de", "pregunte sobre");
    }

    private boolean isCapabilitiesQuestion(String value) {
        return containsAny(value,
                "que puedes hacer", "que sabes hacer", "como me ayudas", "en que me ayudas",
                "que tipo de asistente eres", "capacidades", "funciones", "herramientas", "que puedes consultar"
        );
    }

    private boolean isCurrentUserProfileQuestion(String value) {
        return containsAny(value,
                "como me llamo", "cual es mi nombre", "mi nombre", "cual es mi correo", "mi correo", "mi email",
                "que usuario estoy usando", "usuario estoy usando", "mi usuario", "cual es mi rol", "mi rol",
                "mi perfil", "perfil actual", "numero de telefono", "mi telefono", "mi celular", "recuerdame mi numero"
        );
    }

    private boolean isOrganizationQuestion(String value) {
        return containsAny(value,
                "mi organizacion", "organizacion actual", "empresa actual", "resumen de organizacion", "resumen de la organizacion"
        );
    }

    private boolean isPropertyQuestion(String value) {
        return containsAny(value,
                "propiedad", "propiedades", "alojamiento", "alojamientos", "bungalow", "bungalows", "casa", "casas", "cabin", "cabins", "cabana", "cabanas"
        );
    }

    private boolean isActivePropertyQuestion(String value) {
        return isPropertyQuestion(value) && containsAny(value, "activas", "activa", "activos", "activo")
                && !containsAny(value, "inactivas", "inactiva", "inactivos", "inactivo");
    }

    private boolean isInactivePropertyQuestion(String value) {
        return isPropertyQuestion(value) && containsAny(value, "inactivas", "inactiva", "inactivos", "inactivo");
    }

    private boolean isPropertyImagesQuestion(String value) {
        return isPropertyQuestion(value) && containsAny(value, "imagen", "imagenes", "foto", "fotos", "portada", "sin imagen", "sin imagenes");
    }

    private boolean isPropertyOperationalOverviewQuestion(String value) {
        return isPropertyQuestion(value) && containsAny(value,
                "panorama operativo", "resumen operativo", "operacion", "operativo", "mas mantenimientos", "mas mantenimiento", "pendientes por propiedad", "estado general"
        );
    }

    private boolean isPropertySummaryQuestion(String value) {
        return isPropertyQuestion(value) && containsAny(value,
                "resumen", "resume", "detalle", "descripcion", "dame un resumen", "informacion de"
        );
    }

    private boolean isCatalogQuestion(String value) {
        return containsAny(value,
                "catalogo", "catalogos", "categoria", "categorias", "tipo de mantenimiento", "tipos de mantenimiento",
                "plataforma", "plataformas", "task template", "plantilla", "plantillas", "inventory item type", "tipos de item", "tipos de inventario"
        );
    }

    private boolean isMaintenanceCatalogOverviewQuestion(String value) {
        return containsAny(value, "catalogos puedo usar", "catalogo puedo usar", "catalogos para mantenimiento", "catalogo para mantenimiento", "catalogos de mantenimiento", "catalogo de mantenimiento")
                || (containsAny(value, "catalogo", "catalogos")
                && containsAny(value, "usar", "puedo usar", "disponible", "disponibles")
                && containsAny(value, "mantenimiento", "mantenimientos"));
    }

    private boolean isMaintenanceCategoryQuestion(String value) {
        return containsAny(value, "categorias de mantenimiento", "categoria de mantenimiento", "maintenance categories", "maintenance category")
                || (containsAny(value, "categoria", "categorias") && containsAny(value, "mantenimiento", "mantenimientos"));
    }

    private boolean isMaintenanceTypeQuestion(String value) {
        return containsAny(value, "tipos de mantenimiento", "tipo de mantenimiento", "maintenance types", "maintenance type")
                || (containsAny(value, "tipo", "tipos") && containsAny(value, "mantenimiento", "mantenimientos"));
    }

    private boolean isReservationPlatformQuestion(String value) {
        return containsAny(value, "plataformas", "plataforma", "reservation platforms", "airbnb", "booking")
                && containsAny(value, "reservacion", "reservaciones", "reserva", "reservas", "plataforma", "plataformas");
    }

    private boolean isTaskCategoryQuestion(String value) {
        return containsAny(value, "categorias de tareas", "categoria de tareas", "plantillas de tareas", "plantilla de tareas", "task categories", "task templates")
                || (containsAny(value, "categoria", "categorias", "plantilla", "plantillas") && containsAny(value, "tarea", "tareas"));
    }

    private boolean isPurchaseCategoryQuestion(String value) {
        return containsAny(value, "categorias de compras", "categoria de compras", "catalogos de compras", "catalogo de compras", "purchase categories")
                || (containsAny(value, "categoria", "categorias", "catalogo", "catalogos") && containsAny(value, "compra", "compras", "supply", "supplies"));
    }

    private boolean isInventoryItemTypeQuestion(String value) {
        return containsAny(value, "tipos de item", "tipos de inventario", "inventory item types", "item types", "tipos de supplies");
    }


    private boolean isInventoryQuestion(String value) {
        return containsAny(value,
                "inventario", "inventory", "inventory item", "inventory items", "item de inventario", "items de inventario",
                "item registrado", "items registrados", "supplies", "supply", "suministro", "suministros", "repuesto", "repuestos", "material", "materiales"
        ) || (containsAny(value, "item", "items") && containsAny(value, "registrado", "registrados", "usado", "usados", "usaron", "usan", "uso", "nunca", "reservacion", "reservaciones", "mantenimiento", "mantenimientos", "compra", "compras", "frecuente", "frecuentes", "mas"));
    }

    private boolean isInventoryWhereUsedQuestion(String value) {
        return containsAny(value, "donde se ha usado", "donde se uso", "donde use", "donde he usado", "en donde se uso", "en que se uso")
                || (containsAny(value, "donde") && containsAny(value, "usado", "uso", "use"));
    }

    private boolean isInventoryUnusedQuestion(String value) {
        return isInventoryQuestion(value) && containsAny(value, "nunca", "sin uso", "no usados", "no he usado", "nunca he usado", "unused");
    }

    private boolean isInventoryFrequentlyUsedQuestion(String value) {
        return isInventoryQuestion(value) && containsAny(value, "mas usados", "usan mas", "uso mas", "frecuentes", "frecuentemente", "frequently", "top", "mas se usan");
    }

    private boolean isInventoryReservationUsageQuestion(String value) {
        return isInventoryQuestion(value) && containsAny(value, "reservacion", "reservaciones", "reserva", "reservas", "huesped", "huespedes");
    }

    private boolean isInventoryPurchaseUsageQuestion(String value) {
        return isInventoryQuestion(value) && containsAny(value, "compra", "compras", "comprado", "compre", "precio", "costo");
    }

    private boolean isInventoryMaintenanceUsageQuestion(String value) {
        return isInventoryQuestion(value) && containsAny(value, "mantenimiento", "mantenimientos", "reparacion", "reparaciones");
    }

    private boolean isMaintenanceAnalyticsQuestion(String value) {
        return containsAny(value, "mantenimiento", "mantenimientos", "reparacion", "reparaciones")
                && !isLastMaintenanceQuestion(value)
                && !isOverdueScheduledMaintenanceQuestion(value)
                && !containsAny(value, "programado vencido", "programados vencidos", "proximo mantenimiento programado");
    }

    private boolean isMaintenanceRecentQuestion(String value) {
        return isMaintenanceAnalyticsQuestion(value) && containsAny(value, "recientes", "reciente", "ultimos", "ultimas", "lista", "listar", "buscar", "busca");
    }

    private boolean isMaintenanceCostQuestion(String value) {
        return isMaintenanceAnalyticsQuestion(value) && containsAny(value, "costo", "costos", "gaste", "gasto", "gastado", "cuanto", "total", "precio", "caros", "caras", "dinero");
    }

    private boolean isMaintenanceCostByPropertyQuestion(String value) {
        return isMaintenanceCostQuestion(value) && containsAny(value, "por propiedad", "propiedad genero", "propiedad tiene", "propiedades");
    }

    private boolean isMaintenanceCostByCategoryQuestion(String value) {
        return isMaintenanceCostQuestion(value) && containsAny(value, "por categoria", "categoria", "categorias");
    }

    private boolean isMaintenanceCostByMonthQuestion(String value) {
        return isMaintenanceCostQuestion(value) && containsAny(value, "por mes", "mensual", "mes a mes", "tendencia", "meses");
    }

    private boolean isMaintenanceImageQuestion(String value) {
        return isMaintenanceAnalyticsQuestion(value) && containsAny(value, "imagen", "imagenes", "foto", "fotos", "evidencia", "fotografica", "fotografica", "sin imagen", "sin evidencia");
    }

    private boolean isMaintenanceStatusQuestion(String value) {
        return isMaintenanceAnalyticsQuestion(value) && containsAny(value, "estado", "completado", "completados", "pendiente", "pendientes", "cancelado", "cancelados", "progreso");
    }

    private boolean isMaintenancePropertyFilterQuestion(String value) {
        return isMaintenanceAnalyticsQuestion(value) && containsAny(value, "propiedad", "casa", "bungalow", "alojamiento") && containsAny(value, "de", "del", "para", "en");
    }

    private boolean isMaintenanceCategoryOrTypeFilterQuestion(String value) {
        return isMaintenanceAnalyticsQuestion(value) && containsAny(value, "categoria", "categorias", "tipo", "tipos", "filtro", "cisterna", "bomba", "pozo");
    }

    private boolean isMaintenanceItemUsageQuestion(String value) {
        return isMaintenanceAnalyticsQuestion(value) && containsAny(value, "item", "items", "repuesto", "repuestos", "material", "materiales", "supply", "supplies", "usaron", "usado", "uso");
    }

    private boolean isScheduledMaintenanceToolQuestion(String value) {
        return containsAny(value, "mantenimiento programado", "mantenimientos programados", "scheduled maintenance", "programado", "programados")
                || (containsAny(value, "mantenimiento", "mantenimientos")
                && containsAny(value, "toca", "vencido", "vencidos", "vence", "vencen", "proximo", "proxima", "proximos", "proximas", "frecuencia", "historial", "cumplimiento"));
    }

    private boolean isScheduledMaintenanceUpcomingQuestion(String value) {
        return isScheduledMaintenanceToolQuestion(value) && containsAny(value, "proximo", "proximos", "proxima", "proximas", "upcoming", "siguiente", "siguientes");
    }

    private boolean isScheduledMaintenanceDueTodayQuestion(String value) {
        return isScheduledMaintenanceToolQuestion(value) && containsAny(value, "hoy", "today");
    }

    private boolean isScheduledMaintenanceDueThisWeekQuestion(String value) {
        return isScheduledMaintenanceToolQuestion(value) && containsAny(value, "semana", "week", "esta semana");
    }

    private boolean isScheduledMaintenanceNextDueQuestion(String value) {
        return isScheduledMaintenanceToolQuestion(value) && containsAny(value, "cual es el proximo", "proximo mantenimiento", "toca", "vence", "siguiente");
    }

    private boolean isScheduledMaintenanceByPropertyQuestion(String value) {
        return isScheduledMaintenanceToolQuestion(value) && containsAny(value, "propiedad", "casa", "bungalow", "alojamiento");
    }

    private boolean isScheduledMaintenanceByTypeQuestion(String value) {
        return isScheduledMaintenanceToolQuestion(value) && containsAny(value, "tipo", "categoria", "pozo", "cisterna", "filtro", "bomba");
    }

    private boolean isScheduledMaintenanceByStatusQuestion(String value) {
        return isScheduledMaintenanceToolQuestion(value) && containsAny(value, "estado", "activo", "activos", "pausado", "pausados", "completado", "completados", "cancelado", "cancelados");
    }

    private boolean isScheduledMaintenanceFrequencyQuestion(String value) {
        return isScheduledMaintenanceToolQuestion(value) && containsAny(value, "frecuencia", "frecuencias", "cada cuanto", "periodicidad");
    }

    private boolean isScheduledMaintenanceHistoryQuestion(String value) {
        return isScheduledMaintenanceToolQuestion(value) && containsAny(value, "historial", "historia", "historico", "registro", "registros");
    }

    private boolean isScheduledMaintenanceComplianceQuestion(String value) {
        return isScheduledMaintenanceToolQuestion(value) && containsAny(value, "cumplimiento", "compliance", "estado general", "resumen", "salud", "situacion");
    }

    private boolean isReservationToolQuestion(String value) {
        return containsAny(value, "reservacion", "reservaciones", "reserva", "reservas", "check in", "check-in", "check out", "check-out", "llegada", "llegan", "llega", "salida", "salen", "sale", "ocupacion", "noches reservadas", "dias libres");
    }

    private boolean isReservationTodayQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "hoy", "today");
    }

    private boolean isReservationThisWeekQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "semana", "esta semana", "week");
    }

    private boolean isReservationThisMonthQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "mes", "este mes", "month");
    }

    private boolean isCurrentReservationToolQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "actual", "actuales", "en curso", "hoy hospedados", "ocupado actualmente");
    }

    private boolean isReservationByPropertyQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "propiedad", "casa", "bungalow", "alojamiento");
    }

    private boolean isReservationByGuestQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "huesped", "huespedes", "cliente", "clientes", "guest", "guests");
    }

    private boolean isReservationByStatusQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "estado", "activas", "canceladas", "cancelados", "cancelada", "cancelado");
    }

    private boolean isReservationByPlatformQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "plataforma", "platform", "airbnb", "booking");
    }

    private boolean isReservationOccupancyQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "ocupacion", "ocupada", "ocupadas", "mas ocupacion", "reserved nights", "noches por propiedad");
    }

    private boolean isReservationRevenueQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "ingreso", "ingresos", "revenue", "valor", "dinero", "monto", "total", "cuanto", "gane", "ganado");
    }

    private boolean isReservationNightsQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "noches", "nights", "noches reservadas");
    }

    private boolean isReservationGuestCountQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "cuantos huespedes", "cantidad de huespedes", "huespedes tendre", "guest count");
    }

    private boolean isReservationCalendarQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "calendario", "calendar", "eventos");
    }

    private boolean isNextCheckInQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "proximo check in", "proximo check-in", "proxima llegada", "quien llega", "llega manana", "llega mañana", "siguiente llegada");
    }

    private boolean isNextCheckOutQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "proximo check out", "proximo check-out", "proxima salida", "quien sale", "sale manana", "sale mañana", "siguiente salida");
    }

    private boolean isReservationGapQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "dias libres", "espacios libres", "huecos", "gaps", "entre reservas", "entre reservaciones");
    }

    private boolean isGuestToolQuestion(String value) {
        return containsAny(value, "huesped", "huespedes", "guest", "guests", "cliente", "clientes")
                && !containsAny(value, "supplies", "supply", "suministro", "suministros");
    }

    private boolean isGuestByReservationQuestion(String value) {
        return isGuestToolQuestion(value) && containsAny(value, "reservacion", "reservaciones", "reserva", "reservas", "asociado", "asociados");
    }

    private boolean isRecentGuestQuestion(String value) {
        return isGuestToolQuestion(value) && containsAny(value, "recientes", "reciente", "ultimos", "ultimas");
    }

    private boolean isReturningGuestQuestion(String value) {
        return isGuestToolQuestion(value) && containsAny(value, "recurrente", "recurrentes", "regreso", "volvio", "ya se habia", "returning");
    }

    private boolean isUpcomingGuestQuestion(String value) {
        return isGuestToolQuestion(value) && containsAny(value, "llegan", "llega", "proximos", "proximas", "esta semana", "upcoming");
    }

    private boolean isGuestCountQuestion(String value) {
        return isGuestToolQuestion(value) && containsAny(value, "cuantos", "cantidad", "conteo", "count", "tendre", "tengo este mes");
    }


    private boolean isReservationSupplyToolQuestion(String value) {
        return containsAny(value, "supply", "supplies", "insumo", "insumos", "suministro", "suministros")
                && containsAny(value, "reservacion", "reservaciones", "reserva", "reservas", "check in", "check-in", "huesped", "huespedes", "proxima", "proximas", "ultima", "ultimos", "ultimas", "usaron", "usado", "usan", "usa", "uso", "mas", "más", "asignado", "asignados", "faltan", "faltantes");
    }

    private boolean isReservationSupplyByReservationQuestion(String value) {
        return isReservationSupplyToolQuestion(value) && containsAny(value, "reservacion", "reservaciones", "reserva", "reservas", "codigo", "check in", "check-in");
    }

    private boolean isReservationSupplyByPropertyQuestion(String value) {
        return isReservationSupplyToolQuestion(value) && containsAny(value, "propiedad", "propiedades", "casa", "bungalow", "alojamiento");
    }

    private boolean isReservationSupplyUpcomingQuestion(String value) {
        return isReservationSupplyToolQuestion(value) && containsAny(value, "proxima", "proximas", "proximo", "proximos", "siguiente", "siguientes", "upcoming", "check in", "check-in", "necesito", "necesarios", "necesarias", "preparar");
    }

    private boolean isReservationSupplySummaryQuestion(String value) {
        return isReservationSupplyToolQuestion(value) && containsAny(value, "resumen", "summary", "total", "totales", "por item", "por producto");
    }

    private boolean isReservationSupplySummaryByDateQuestion(String value) {
        return isReservationSupplyToolQuestion(value) && containsAny(value, "fecha", "fechas", "rango", "semana", "mes", "hoy");
    }

    private boolean isReservationSupplyLastUsedQuestion(String value) {
        return isReservationSupplyToolQuestion(value) && containsAny(value, "ultimo", "ultima", "ultima vez", "last used", "se usaron", "se uso");
    }

    private boolean isReservationSupplyLatestReservationQuestion(String value) {
        return isReservationSupplyToolQuestion(value)
                && containsAny(value, "ultima reserva", "ultima reservacion", "ultima reservación", "ultima vez en reserva", "se usaron en la ultima", "se usaron en ultima");
    }

    private boolean isReservationSupplyMostUsedQuestion(String value) {
        return isReservationSupplyToolQuestion(value) && containsAny(value, "mas usados", "más usados", "mas usado", "más usado", "usan mas", "usan más", "usa mas", "usa más", "se usan mas", "se usan más", "most used", "frecuentes");
    }

    private boolean isReservationSupplyMissingQuestion(String value) {
        return isReservationSupplyToolQuestion(value) && containsAny(value, "no tienen", "no tiene", "sin supplies", "sin supply", "sin insumos", "sin asignar", "no asignados", "no asignadas", "no tienen supplies", "faltan", "faltantes", "missing");
    }

    private boolean isTaskListToolQuestion(String value) {
        return containsAny(value, "tarea", "tareas", "task list", "task lists", "lista de tareas", "listas de tareas", "checklist")
                && !containsAny(value, "mantenimiento programado", "supplies", "supply", "insumo", "insumos");
    }

    private boolean isTaskListByPropertyQuestion(String value) {
        return isTaskListToolQuestion(value) && containsAny(value, "propiedad", "propiedades", "casa", "bungalow", "alojamiento");
    }

    private boolean isTaskListByReservationQuestion(String value) {
        return isTaskListToolQuestion(value) && containsAny(value, "reservacion", "reservaciones", "reserva", "reservas", "check in", "check-in");
    }

    private boolean isTaskListForNextReservationQuestion(String value) {
        return isTaskListToolQuestion(value)
                && containsAny(value, "proxima reservacion", "proxima reservación", "proxima reserva", "siguiente reservacion", "siguiente reserva");
    }

    private boolean isTaskListActiveQuestion(String value) {
        return isTaskListToolQuestion(value) && containsAny(value, "activas", "activa", "abiertas", "abierta", "pendientes", "pendiente", "en progreso");
    }

    private boolean isTaskListCompletedQuestion(String value) {
        return isTaskListToolQuestion(value) && containsAny(value, "completadas", "completada", "completados", "completado", "terminadas", "cerradas");
    }

    private boolean isTaskListOverdueQuestion(String value) {
        return isTaskListToolQuestion(value) && containsAny(value, "vencidas", "vencida", "atrasadas", "atrasada", "overdue");
    }

    private boolean isTaskListDueTodayQuestion(String value) {
        return isTaskListToolQuestion(value) && containsAny(value, "hoy", "today");
    }

    private boolean isTaskListDueThisWeekQuestion(String value) {
        return isTaskListToolQuestion(value) && containsAny(value, "esta semana", "semana", "week");
    }

    private boolean isTaskListProgressQuestion(String value) {
        return isTaskListToolQuestion(value) && containsAny(value, "avance", "progreso", "porcentaje", "progress");
    }

    private boolean isTaskListCompletionQuestion(String value) {
        return isTaskListToolQuestion(value) && containsAny(value, "completitud", "completion", "estado", "distribucion");
    }

    private boolean isTaskItemToolQuestion(String value) {
        return isTaskListToolQuestion(value) && containsAny(value, "especifica", "especificas", "item", "items", "faltan", "faltantes", "responsable", "responsables", "prioridad", "priority", "completada", "completadas", "completado", "completados", "completaron", "ya se completaron", "pendiente", "pendientes", "atrasada", "atrasadas", "vencida", "vencidas");
    }

    private boolean isTaskItemByTaskListQuestion(String value) {
        return isTaskItemToolQuestion(value) && containsAny(value, "lista", "checklist", "task list");
    }

    private boolean isTaskItemPendingQuestion(String value) {
        return isTaskItemToolQuestion(value) && containsAny(value, "pendientes", "pendiente", "faltan", "faltantes", "no completadas");
    }

    private boolean isTaskItemCompletedQuestion(String value) {
        return isTaskItemToolQuestion(value) && containsAny(value, "completadas", "completada", "completados", "completado", "ya se completaron");
    }

    private boolean isTaskItemOverdueQuestion(String value) {
        return isTaskItemToolQuestion(value) && containsAny(value, "atrasadas", "atrasada", "vencidas", "vencida", "overdue");
    }

    private boolean isTaskItemAssignedSummaryQuestion(String value) {
        return isTaskItemToolQuestion(value) && containsAny(value, "responsable", "responsables", "asignado", "asignadas", "assigned");
    }

    private boolean isTaskItemPrioritySummaryQuestion(String value) {
        return isTaskItemToolQuestion(value) && containsAny(value, "prioridad", "prioridades", "priority");
    }


    private boolean isPurchaseAnalyticsQuestion(String value) {
        return containsAny(value,
                "compra", "compras", "compre", "comprado", "comprados", "producto", "productos", "proveedor", "proveedores", "precio", "precios", "costo", "costos", "cuesta", "normalmente", "gasto", "gastos", "gaste", "gastado", "supplies", "supply", "suministro", "suministros", "papel higienico"
        )
                || containsAny(value, "cuanto cuesta", "cuánto cuesta", "cuesta normalmente", "costo promedio", "precio promedio")
                || (containsAny(value, "item", "items") && containsAny(value, "compro", "compras", "compre", "comprado", "precio", "costo", "gasto"));
    }

    private boolean isPurchaseListPendingQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "lista", "listas", "compra", "compras")
                && containsAny(value, "pendiente", "pendientes", "abierta", "abiertas", "open", "partially");
    }

    private boolean isPurchaseListCompletedQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "lista", "listas", "compra", "compras")
                && containsAny(value, "completada", "completadas", "completado", "completados", "finalizada", "finalizadas");
    }

    private boolean isPurchaseListRecentQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value) && containsAny(value, "reciente", "recientes", "ultimas compras", "ultimas listas");
    }

    private boolean isPurchaseListByPropertyQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "propiedad", "propiedades", "casa", "bungalow", "alojamiento")
                && !containsAny(value, "gasto", "gastos", "costo", "costos", "genero mas", "mas compras");
    }

    private boolean isPurchaseCostSummaryQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "cuanto", "gaste", "gasté", "gasto", "gastos", "costo", "costos", "monto", "total")
                && !isPurchaseItemAverageUnitCostQuestion(value)
                && !isPurchaseItemPriceHistoryQuestion(value);
    }

    private boolean isPurchaseCostByPropertyQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "propiedad", "propiedades", "genero mas", "generó mas", "mas compras", "más compras")
                && containsAny(value, "gasto", "gastos", "costo", "costos", "compra", "compras", "genero", "generó");
    }

    private boolean isPurchaseCostByCategoryQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "categoria", "categorias", "tipo", "tipos")
                && containsAny(value, "gasto", "gastos", "costo", "costos", "compra", "compras", "supplies", "suministros");
    }

    private boolean isPurchaseCostByMonthQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "por mes", "mensual", "mes a mes", "meses", "tendencia mensual");
    }

    private boolean isPurchaseItemListQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "item", "items", "producto", "productos")
                && !isPurchaseItemMostPurchasedQuestion(value)
                && !isPurchaseItemLeastPurchasedQuestion(value)
                && !isPurchaseItemAverageUnitCostQuestion(value)
                && !isPurchaseItemPriceHistoryQuestion(value)
                && !isPurchaseItemCostTrendQuestion(value);
    }

    private boolean isPurchaseItemPriceHistoryQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "historial de precio", "historial de precios", "precios", "precio")
                && containsAny(value, "historial", "ultimos", "ultimas", "evolucion", "evolución");
    }

    private boolean isPurchaseItemAverageUnitCostQuestion(String value) {
        return containsAny(value, "cuanto cuesta", "cuánto cuesta", "cuesta normalmente", "normalmente", "promedio", "costo promedio", "precio promedio", "unitario");
    }

    private boolean isPurchaseItemQuantitySummaryQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "cantidad", "cantidades", "cuanto compre", "cuánto compré", "cuantos compre", "cuántos compré");
    }

    private boolean isPurchaseItemMostPurchasedQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "compro mas", "compro más", "mas seguido", "más seguido", "mas comprado", "más comprado", "compro con mas frecuencia", "top compras", "item compro mas");
    }

    private boolean isPurchaseItemLeastPurchasedQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "menos comprado", "menos compro", "menor frecuencia", "compro menos", "menos seguido");
    }

    private boolean isPurchaseItemCostTrendQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "ha subido", "subio", "subió", "bajado", "bajo", "bajó", "tendencia", "variacion", "variación", "cambio de precio");
    }

    private boolean isOperationalSummaryQuestion(String value) {
        return containsAny(value,
                "resumen operativo", "dashboard", "panel operativo", "estado operativo", "resumen del sistema", "metricas operativas"
        );
    }

    private boolean isUpcomingReservationQuestion(String value) {
        return containsAny(value,
                "reservaciones proximas", "reservas proximas", "reservaciones activas", "reservas activas", "proximas reservaciones", "proximas reservas",
                "check in proximos", "check-in proximos", "entradas proximas"
        ) || (containsAny(value, "reservacion", "reservaciones", "reserva", "reservas")
                && containsAny(value, "proxima", "proximas", "siguiente", "siguientes", "activas", "check in", "check-in"));
    }

    private boolean isLastMaintenanceQuestion(String value) {
        return containsAny(value,
                "ultimo mantenimiento", "ultima reparacion", "mantenimiento mas reciente", "ultimo trabajo realizado", "mantenimiento realizado", "mantenimiento completado"
        ) || (containsAny(value, "mantenimiento", "mantenimientos", "reparacion", "reparaciones")
                && containsAny(value, "ultimo", "ultima", "reciente", "realizado", "completado"));
    }

    private boolean isOverdueScheduledMaintenanceQuestion(String value) {
        return containsAny(value,
                "mantenimientos programados vencidos", "mantenimiento programado vencido", "programados vencidos", "vencidos", "atrasados", "caducados"
        ) && containsAny(value, "mantenimiento", "mantenimientos", "programado", "programados");
    }

    private boolean isLastPurchaseQuestion(String value) {
        return containsAny(value,
                "ultima compra", "ultimo item comprado", "ultimo producto comprado", "compre por ultima vez", "compraste por ultima vez", "cuando compre", "cuando se compro", "comprado por ultima vez"
        ) || (containsAny(value, "compra", "compras", "compre", "comprado", "compraste")
                && containsAny(value, "ultima", "ultimo", "vez", "cuando", "reciente"));
    }

    private boolean isPendingTaskQuestion(String value) {
        return containsAny(value,
                "tareas pendientes", "listas de tareas pendientes", "task lists pendientes", "pendientes tengo", "tareas abiertas", "listas abiertas", "tareas en progreso", "cosas pendientes", "algo pendiente"
        ) || (containsAny(value, "tarea", "tareas", "task", "tasks", "pendiente", "pendientes")
                && containsAny(value, "pendiente", "pendientes", "abierta", "abiertas", "progreso", "hacer"));
    }


    private boolean isDocumentToolQuestion(String value) {
        return containsAny(value,
                "documento", "documentos", "archivo", "archivos", "pdf", "manual", "manuales", "plano", "planos", "regla", "reglas", "house rules", "document metadata"
        );
    }

    private boolean isDocumentByPropertyQuestion(String value) {
        return isDocumentToolQuestion(value) && containsAny(value, "propiedad", "alojamiento", "casa", "bungalow", "para esta propiedad", "de esta propiedad");
    }

    private boolean isDocumentByTypeQuestion(String value) {
        return isDocumentToolQuestion(value) && containsAny(value, "tipo", "plano", "planos", "manual", "manuales", "regla", "reglas", "blueprint");
    }

    private boolean isDocumentByStatusQuestion(String value) {
        return isDocumentToolQuestion(value) && containsAny(value, "estado", "status", "procesado", "procesados", "procesar", "fallaron", "fallo", "indexado", "indexados");
    }

    private boolean isDocumentRecentQuestion(String value) {
        return isDocumentToolQuestion(value) && containsAny(value, "reciente", "recientes", "ultimos", "ultimas", "subidos recientemente", "cargados recientemente");
    }

    private boolean isDocumentUnprocessedQuestion(String value) {
        return isDocumentToolQuestion(value) && containsAny(value, "no han sido procesados", "no procesados", "sin procesar", "pendientes de procesar", "pendiente de procesar", "unprocessed");
    }

    private boolean isDocumentFailedQuestion(String value) {
        return isDocumentToolQuestion(value) && containsAny(value, "fallaron", "fallo", "failed", "error al procesar", "procesamiento fallido");
    }

    private boolean isDocumentProcessedQuestion(String value) {
        return isDocumentToolQuestion(value) && containsAny(value, "procesados", "procesado", "ya procesados", "processed") && !isDocumentUnprocessedQuestion(value);
    }

    private boolean isDocumentIndexedQuestion(String value) {
        return isDocumentToolQuestion(value)
                && containsAny(value, "indexados", "indexado", "listos para ia", "listos para ai", "listos para inteligencia artificial")
                && !isDocumentNotIndexedQuestion(value);
    }

    private boolean isDocumentProcessedNotIndexedQuestion(String value) {
        return isDocumentToolQuestion(value)
                && containsAny(value, "procesados", "procesado", "processed")
                && containsAny(value,
                "no indexados", "sin indexar", "no estan indexados", "no están indexados", "no esten indexados", "no estén indexados", "not indexed"
        );
    }

    private boolean isDocumentNotIndexedQuestion(String value) {
        return isDocumentToolQuestion(value) && containsAny(value,
                "no indexados", "sin indexar", "no estan indexados", "no están indexados", "no esten indexados", "no estén indexados", "not indexed"
        );
    }

    private boolean isDocumentCountByTypeQuestion(String value) {
        return isDocumentToolQuestion(value)
                && containsAny(value, "tipo", "por tipo", "by type")
                && containsAny(value,
                "cuantos", "cuántos", "cantidad", "conteo", "count", "agrupados", "agrupar", "grupo"
        );
    }

    private boolean isDocumentCountByPropertyQuestion(String value) {
        return isDocumentToolQuestion(value)
                && containsAny(value, "propiedad", "por propiedad", "by property")
                && containsAny(value,
                "cuantos", "cuántos", "cantidad", "conteo", "count", "agrupados", "agrupar", "grupo"
        );
    }

    private boolean isDocumentBlueprintQuestion(String value) {
        return containsAny(value, "plano", "planos", "blueprint", "electrico", "eléctrico", "plomeria", "plomería", "drenaje") && isDocumentToolQuestion(value);
    }

    private boolean isDocumentHouseRulesQuestion(String value) {
        return containsAny(value, "reglas de casa", "house rules", "reglas", "senales", "señales") && isDocumentToolQuestion(value);
    }

    private boolean isDocumentManualQuestion(String value) {
        return containsAny(value, "manual", "manuales") && isDocumentToolQuestion(value);
    }

    private boolean isRagChunkSummaryQuestion(String value) {
        return containsAny(value, "resumen de chunks", "chunk summary", "chunks por documento", "cuantos chunks", "cuántos chunks");
    }

    private boolean isRagMissingChunksQuestion(String value) {
        return containsAny(value, "sin chunks", "no tienen chunks", "missing chunks", "documentos sin chunks");
    }

    private boolean isRagMissingVectorIdsQuestion(String value) {
        return containsAny(value, "sin vector_store_id", "sin vector store id", "missing vector", "chunks pero no vector", "pendientes de vector");
    }

    private boolean isRagCoverageSummaryQuestion(String value) {
        return containsAny(value, "cobertura del indice", "coverage", "index coverage", "cobertura rag", "coverage summary");
    }

    private boolean isDocumentMetadataQuestion(String value) {
        return containsAny(value,
                "documentos cargados", "documentos subidos", "documentos tengo", "documentos registrados", "documentos procesados", "que documentos", "mis documentos", "document metadata", "archivos cargados", "archivos subidos"
        );
    }

    private boolean isRagHealthQuestion(String value) {
        return containsAny(value,
                "indice rag", "índice rag", "index rag", "rag de mis documentos", "estado rag", "estado del rag", "salud del rag", "estado del indice", "estado del índice", "indexacion de documentos", "estado de indexacion", "chunks indexados", "vector store", "vector_store", "chroma"
        );
    }

    private boolean isPreparationQuestion(String value) {
        return containsAny(value,
                "preparar la casa", "preparar propiedad", "preparar alojamiento", "antes de la proxima reserva", "antes de la proxima reservacion", "antes del proximo check in", "antes del check in", "para la proxima reserva", "para la proxima reservacion"
        ) || (containsAny(value, "preparar", "pendiente", "pendientes", "hacer", "falta")
                && containsAny(value, "reserva", "reservacion", "check in", "huesped", "alojamiento", "casa", "propiedad"));
    }

    private boolean isOperationalPlanningQuestion(String value) {
        return containsAny(value,
                "que debo atender", "que debo revisar", "que tengo pendiente hoy", "prioridades operativas", "plan operativo", "que necesita atencion", "como va la operacion", "estado general de la operacion"
        );
    }

    private boolean isDocumentInventoryQuestion(String value) {
        return isDocumentMetadataQuestion(value) && isRagHealthQuestion(value);
    }

    private boolean isPropertyOperationsQuestion(String value) {
        return isPropertyQuestion(value) && containsAny(value, "operacion", "operativo", "reservacion", "reserva", "mantenimiento", "tarea", "pendiente", "estado");
    }

    private boolean isUnsupportedWriteAction(String value) {
        return startsWithAny(value,
                "crea ", "crear ", "agrega ", "agregar ", "anade ", "anadir ", "registra ", "registrar ",
                "actualiza ", "actualizar ", "edita ", "editar ", "modifica ", "modificar ", "elimina ",
                "eliminar ", "borra ", "borrar ", "cancela ", "cancelar ", "envia ", "enviar ", "manda ", "mandar ", "programa ", "programar "
        );
    }

    private boolean containsAny(String value, String... candidates) {
        return AiToolTextNormalizer.containsAnyForRouting(value, candidates);
    }

    private boolean startsWithAny(String value, String... prefixes) {
        return AiToolTextNormalizer.startsWithAnyForRouting(value, prefixes);
    }

    private String normalize(String value) {
        return AiToolTextNormalizer.normalizeForRouting(value);
    }

    private String collapseWhitespace(String value) {
        return AiToolTextNormalizer.collapseWhitespace(value);
    }

}
