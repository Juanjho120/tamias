package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AiReadOnlyToolService {

    private final AssistantProfileReadOnlyToolService assistantProfileReadOnlyToolService;
    private final UserRoleOrganizationReadOnlyToolService userRoleOrganizationReadOnlyToolService;
    private final AiChatHistoryReadOnlyToolService aiChatHistoryReadOnlyToolService;
    private final PropertyCatalogReadOnlyToolService propertyCatalogReadOnlyToolService;
    private final ScheduledReservationGuestReadOnlyToolService scheduledReservationGuestReadOnlyToolService;
    private final MaintenanceReadOnlyToolService maintenanceReadOnlyToolService;
    private final PurchaseReadOnlyToolService purchaseReadOnlyToolService;
    private final ReservationSupplyTaskReadOnlyToolService reservationSupplyTaskReadOnlyToolService;
    private final DocumentRagReadOnlyToolService documentRagReadOnlyToolService;
    private final InventoryReadOnlyToolService inventoryReadOnlyToolService;
    private final FileImageReadOnlyToolService fileImageReadOnlyToolService;
    private final DashboardReadOnlyToolService dashboardReadOnlyToolService;

    public AiReadOnlyToolService(
            AssistantProfileReadOnlyToolService assistantProfileReadOnlyToolService,
            UserRoleOrganizationReadOnlyToolService userRoleOrganizationReadOnlyToolService,
            AiChatHistoryReadOnlyToolService aiChatHistoryReadOnlyToolService,
            PropertyCatalogReadOnlyToolService propertyCatalogReadOnlyToolService,
            ScheduledReservationGuestReadOnlyToolService scheduledReservationGuestReadOnlyToolService,
            MaintenanceReadOnlyToolService maintenanceReadOnlyToolService,
            PurchaseReadOnlyToolService purchaseReadOnlyToolService,
            ReservationSupplyTaskReadOnlyToolService reservationSupplyTaskReadOnlyToolService,
            DocumentRagReadOnlyToolService documentRagReadOnlyToolService,
            InventoryReadOnlyToolService inventoryReadOnlyToolService,
            FileImageReadOnlyToolService fileImageReadOnlyToolService,
            DashboardReadOnlyToolService dashboardReadOnlyToolService
    ) {
        this.assistantProfileReadOnlyToolService = assistantProfileReadOnlyToolService;
        this.userRoleOrganizationReadOnlyToolService = userRoleOrganizationReadOnlyToolService;
        this.aiChatHistoryReadOnlyToolService = aiChatHistoryReadOnlyToolService;
        this.propertyCatalogReadOnlyToolService = propertyCatalogReadOnlyToolService;
        this.scheduledReservationGuestReadOnlyToolService = scheduledReservationGuestReadOnlyToolService;
        this.maintenanceReadOnlyToolService = maintenanceReadOnlyToolService;
        this.purchaseReadOnlyToolService = purchaseReadOnlyToolService;
        this.reservationSupplyTaskReadOnlyToolService = reservationSupplyTaskReadOnlyToolService;
        this.documentRagReadOnlyToolService = documentRagReadOnlyToolService;
        this.inventoryReadOnlyToolService = inventoryReadOnlyToolService;
        this.fileImageReadOnlyToolService = fileImageReadOnlyToolService;
        this.dashboardReadOnlyToolService = dashboardReadOnlyToolService;
    }

    public AiToolAnswer capabilities() {
        return assistantProfileReadOnlyToolService.capabilities();
    }

    public AiToolAnswer currentUserProfile(String userQuestion) {
        return assistantProfileReadOnlyToolService.currentUserProfile(userQuestion);
    }

    public AiToolAnswer currentOrganizationSummary() {
        return assistantProfileReadOnlyToolService.currentOrganizationSummary();
    }

    public AiToolAnswer activeUsers() {
        return userRoleOrganizationReadOnlyToolService.activeUsers();
    }

    public AiToolAnswer inactiveUsers() {
        return userRoleOrganizationReadOnlyToolService.inactiveUsers();
    }

    public AiToolAnswer searchUsers(String userQuestion) {
        return userRoleOrganizationReadOnlyToolService.searchUsers(userQuestion);
    }

    public AiToolAnswer usersByRole(String userQuestion) {
        return userRoleOrganizationReadOnlyToolService.usersByRole(userQuestion);
    }

    public AiToolAnswer userAccessSummary(String userQuestion) {
        return userRoleOrganizationReadOnlyToolService.userAccessSummary(userQuestion);
    }

    public AiToolAnswer roleList() {
        return userRoleOrganizationReadOnlyToolService.roleList();
    }

    public AiToolAnswer rolePermissionSummary(String userQuestion) {
        return userRoleOrganizationReadOnlyToolService.rolePermissionSummary(userQuestion);
    }

    public AiToolAnswer organizationUserCount() {
        return userRoleOrganizationReadOnlyToolService.organizationUserCount();
    }

    public AiToolAnswer organizationModuleUsageSummary() {
        return userRoleOrganizationReadOnlyToolService.organizationModuleUsageSummary();
    }

    public AiToolAnswer aiChatRecentSessions(UUID excludedSessionId) {
        return aiChatHistoryReadOnlyToolService.aiChatRecentSessions(excludedSessionId);
    }

    public AiToolAnswer aiChatSearchHistory(String userQuestion, UUID excludedSessionId) {
        return aiChatHistoryReadOnlyToolService.aiChatSearchHistory(userQuestion, excludedSessionId);
    }

    public AiToolAnswer aiChatRecentMessages(UUID excludedSessionId) {
        return aiChatHistoryReadOnlyToolService.aiChatRecentMessages(excludedSessionId);
    }

    public AiToolAnswer aiChatRecentUserQuestions(UUID excludedSessionId) {
        return aiChatHistoryReadOnlyToolService.aiChatRecentUserQuestions(excludedSessionId);
    }

    public AiToolAnswer aiChatSessionsByProperty(String userQuestion, UUID excludedSessionId) {
        return aiChatHistoryReadOnlyToolService.aiChatSessionsByProperty(userQuestion, excludedSessionId);
    }

    public AiToolAnswer aiChatCurrentSessionSummary(UUID chatSessionId) {
        return aiChatHistoryReadOnlyToolService.aiChatCurrentSessionSummary(chatSessionId);
    }

    public AiToolAnswer aiChatUsageSummary() {
        return aiChatHistoryReadOnlyToolService.aiChatUsageSummary();
    }

    public AiToolAnswer searchProperties(String userQuestion) {
        return propertyCatalogReadOnlyToolService.searchProperties(userQuestion);
    }

    public AiToolAnswer activeProperties() {
        return propertyCatalogReadOnlyToolService.activeProperties();
    }

    public AiToolAnswer inactiveProperties() {
        return propertyCatalogReadOnlyToolService.inactiveProperties();
    }

    public AiToolAnswer propertySummary(String userQuestion) {
        return propertyCatalogReadOnlyToolService.propertySummary(userQuestion);
    }

    public AiToolAnswer propertyOperationalOverview() {
        return propertyCatalogReadOnlyToolService.propertyOperationalOverview();
    }

    public AiToolAnswer propertyImagesSummary(String userQuestion) {
        return propertyCatalogReadOnlyToolService.propertyImagesSummary(userQuestion);
    }

    public AiToolAnswer maintenanceCategories() {
        return propertyCatalogReadOnlyToolService.maintenanceCategories();
    }

    public AiToolAnswer maintenanceTypes() {
        return propertyCatalogReadOnlyToolService.maintenanceTypes();
    }

    public AiToolAnswer maintenanceCatalogOverview() {
        return propertyCatalogReadOnlyToolService.maintenanceCatalogOverview();
    }

    public AiToolAnswer reservationPlatforms() {
        return propertyCatalogReadOnlyToolService.reservationPlatforms();
    }

    public AiToolAnswer taskCategories() {
        return propertyCatalogReadOnlyToolService.taskCategories();
    }

    public AiToolAnswer purchaseCategories() {
        return propertyCatalogReadOnlyToolService.purchaseCategories();
    }

    public AiToolAnswer inventoryItemTypes() {
        return propertyCatalogReadOnlyToolService.inventoryItemTypes();
    }

    public AiToolAnswer catalogSearch(String userQuestion) {
        return propertyCatalogReadOnlyToolService.catalogSearch(userQuestion);
    }

    public AiToolAnswer operationalSummary() {
        return dashboardReadOnlyToolService.operationalSummary();
    }

    public AiToolAnswer upcomingReservations() {
        return scheduledReservationGuestReadOnlyToolService.upcomingReservations();
    }

    public AiToolAnswer scheduledMaintenanceSearch(String userQuestion) {
        return scheduledReservationGuestReadOnlyToolService.scheduledMaintenanceSearch(userQuestion);
    }

    public AiToolAnswer upcomingScheduledMaintenance() {
        return scheduledReservationGuestReadOnlyToolService.upcomingScheduledMaintenance();
    }

    public AiToolAnswer dueTodayScheduledMaintenance() {
        return scheduledReservationGuestReadOnlyToolService.dueTodayScheduledMaintenance();
    }

    public AiToolAnswer dueThisWeekScheduledMaintenance() {
        return scheduledReservationGuestReadOnlyToolService.dueThisWeekScheduledMaintenance();
    }

    public AiToolAnswer scheduledMaintenanceByProperty(String userQuestion) {
        return scheduledReservationGuestReadOnlyToolService.scheduledMaintenanceByProperty(userQuestion);
    }

    public AiToolAnswer scheduledMaintenanceByType(String userQuestion) {
        return scheduledReservationGuestReadOnlyToolService.scheduledMaintenanceByType(userQuestion);
    }

    public AiToolAnswer scheduledMaintenanceByStatus(String userQuestion) {
        return scheduledReservationGuestReadOnlyToolService.scheduledMaintenanceByStatus(userQuestion);
    }

    public AiToolAnswer nextDueScheduledMaintenance(String userQuestion) {
        return scheduledReservationGuestReadOnlyToolService.nextDueScheduledMaintenance(userQuestion);
    }

    public AiToolAnswer scheduledMaintenanceFrequencySummary() {
        return scheduledReservationGuestReadOnlyToolService.scheduledMaintenanceFrequencySummary();
    }

    public AiToolAnswer scheduledMaintenanceHistory(String userQuestion) {
        return scheduledReservationGuestReadOnlyToolService.scheduledMaintenanceHistory(userQuestion);
    }

    public AiToolAnswer scheduledMaintenanceComplianceSummary() {
        return scheduledReservationGuestReadOnlyToolService.scheduledMaintenanceComplianceSummary();
    }

    public AiToolAnswer reservationsToday() {
        return scheduledReservationGuestReadOnlyToolService.reservationsToday();
    }

    public AiToolAnswer currentReservations() {
        return scheduledReservationGuestReadOnlyToolService.currentReservations();
    }

    public AiToolAnswer reservationsThisWeek() {
        return scheduledReservationGuestReadOnlyToolService.reservationsThisWeek();
    }

    public AiToolAnswer reservationsThisMonth() {
        return scheduledReservationGuestReadOnlyToolService.reservationsThisMonth();
    }

    public AiToolAnswer reservationsByProperty(String userQuestion) {
        return scheduledReservationGuestReadOnlyToolService.reservationsByProperty(userQuestion);
    }

    public AiToolAnswer reservationsByGuest(String userQuestion) {
        return scheduledReservationGuestReadOnlyToolService.reservationsByGuest(userQuestion);
    }

    public AiToolAnswer reservationsByStatus(String userQuestion) {
        return scheduledReservationGuestReadOnlyToolService.reservationsByStatus(userQuestion);
    }

    public AiToolAnswer reservationsByPlatform(String userQuestion) {
        return scheduledReservationGuestReadOnlyToolService.reservationsByPlatform(userQuestion);
    }

    public AiToolAnswer reservationSearch(String userQuestion) {
        return scheduledReservationGuestReadOnlyToolService.reservationSearch(userQuestion);
    }

    public AiToolAnswer nextCheckIn() {
        return scheduledReservationGuestReadOnlyToolService.nextCheckIn();
    }

    public AiToolAnswer nextCheckOut() {
        return scheduledReservationGuestReadOnlyToolService.nextCheckOut();
    }

    public AiToolAnswer reservationCalendarEvents() {
        return scheduledReservationGuestReadOnlyToolService.reservationCalendarEvents();
    }

    public AiToolAnswer reservationRevenueSummary(String userQuestion) {
        return scheduledReservationGuestReadOnlyToolService.reservationRevenueSummary(userQuestion);
    }

    public AiToolAnswer reservationNightsSummary(String userQuestion) {
        return scheduledReservationGuestReadOnlyToolService.reservationNightsSummary(userQuestion);
    }

    public AiToolAnswer reservationGuestCountSummary(String userQuestion) {
        return scheduledReservationGuestReadOnlyToolService.reservationGuestCountSummary(userQuestion);
    }

    public AiToolAnswer reservationOccupancySummary(String userQuestion) {
        return scheduledReservationGuestReadOnlyToolService.reservationOccupancySummary(userQuestion);
    }

    public AiToolAnswer reservationGapsBetweenReservations() {
        return scheduledReservationGuestReadOnlyToolService.reservationGapsBetweenReservations();
    }

    public AiToolAnswer guestSearch(String userQuestion) {
        return scheduledReservationGuestReadOnlyToolService.guestSearch(userQuestion);
    }

    public AiToolAnswer guestsByReservation(String userQuestion) {
        return scheduledReservationGuestReadOnlyToolService.guestsByReservation(userQuestion);
    }

    public AiToolAnswer recentGuests() {
        return scheduledReservationGuestReadOnlyToolService.recentGuests();
    }

    public AiToolAnswer returningGuests() {
        return scheduledReservationGuestReadOnlyToolService.returningGuests();
    }

    public AiToolAnswer upcomingGuests() {
        return scheduledReservationGuestReadOnlyToolService.upcomingGuests();
    }

    public AiToolAnswer guestCountByDateRange(String userQuestion) {
        return scheduledReservationGuestReadOnlyToolService.guestCountByDateRange(userQuestion);
    }

    public AiToolAnswer lastPerformedMaintenance(String userQuestion) {
        return maintenanceReadOnlyToolService.lastPerformedMaintenance(userQuestion);
    }

    public AiToolAnswer overdueScheduledMaintenance() {
        return scheduledReservationGuestReadOnlyToolService.overdueScheduledMaintenance();
    }

    public AiToolAnswer lastPurchasedItem(String userQuestion) {
        return purchaseReadOnlyToolService.lastPurchasedItem(userQuestion);
    }

    public AiToolAnswer purchaseListSearch(String userQuestion) {
        return purchaseReadOnlyToolService.purchaseListSearch(userQuestion);
    }

    public AiToolAnswer purchaseListsByProperty(String userQuestion) {
        return purchaseReadOnlyToolService.purchaseListsByProperty(userQuestion);
    }

    public AiToolAnswer recentPurchaseLists() {
        return purchaseReadOnlyToolService.recentPurchaseLists();
    }

    public AiToolAnswer pendingPurchaseLists() {
        return purchaseReadOnlyToolService.pendingPurchaseLists();
    }

    public AiToolAnswer completedPurchaseLists() {
        return purchaseReadOnlyToolService.completedPurchaseLists();
    }

    public AiToolAnswer purchaseCostSummary(String userQuestion) {
        return purchaseReadOnlyToolService.purchaseCostSummary(userQuestion);
    }

    public AiToolAnswer purchaseCostByProperty() {
        return purchaseReadOnlyToolService.purchaseCostByProperty();
    }

    public AiToolAnswer purchaseCostByCategory() {
        return purchaseReadOnlyToolService.purchaseCostByCategory();
    }

    public AiToolAnswer purchaseCostByMonth() {
        return purchaseReadOnlyToolService.purchaseCostByMonth();
    }

    public AiToolAnswer purchaseItemSearch(String userQuestion) {
        return purchaseReadOnlyToolService.purchaseItemSearch(userQuestion);
    }

    public AiToolAnswer purchaseItemsByPurchaseList(String userQuestion) {
        return purchaseReadOnlyToolService.purchaseItemsByPurchaseList(userQuestion);
    }

    public AiToolAnswer purchaseItemsByInventoryItem(String userQuestion) {
        return purchaseReadOnlyToolService.purchaseItemsByInventoryItem(userQuestion);
    }

    public AiToolAnswer purchaseItemPriceHistory(String userQuestion) {
        return purchaseReadOnlyToolService.purchaseItemPriceHistory(userQuestion);
    }

    public AiToolAnswer purchaseItemAverageUnitCost(String userQuestion) {
        return purchaseReadOnlyToolService.purchaseItemAverageUnitCost(userQuestion);
    }

    public AiToolAnswer purchaseItemQuantitySummary(String userQuestion) {
        return purchaseReadOnlyToolService.purchaseItemQuantitySummary(userQuestion);
    }

    public AiToolAnswer purchaseItemMostPurchased() {
        return purchaseReadOnlyToolService.purchaseItemMostPurchased();
    }

    public AiToolAnswer purchaseItemLeastPurchased() {
        return purchaseReadOnlyToolService.purchaseItemLeastPurchased();
    }

    public AiToolAnswer purchaseItemCostTrend(String userQuestion) {
        return purchaseReadOnlyToolService.purchaseItemCostTrend(userQuestion);
    }

    public AiToolAnswer pendingTaskLists() {
        return reservationSupplyTaskReadOnlyToolService.pendingTaskLists();
    }

    public AiToolAnswer reservationSupplySearch(String userQuestion) {
        return reservationSupplyTaskReadOnlyToolService.reservationSupplySearch(userQuestion);
    }

    public AiToolAnswer reservationSuppliesByReservation(String userQuestion) {
        return reservationSupplyTaskReadOnlyToolService.reservationSuppliesByReservation(userQuestion);
    }

    public AiToolAnswer reservationSuppliesByProperty(String userQuestion) {
        return reservationSupplyTaskReadOnlyToolService.reservationSuppliesByProperty(userQuestion);
    }

    public AiToolAnswer reservationSuppliesForUpcomingReservations() {
        return reservationSupplyTaskReadOnlyToolService.reservationSuppliesForUpcomingReservations();
    }

    public AiToolAnswer reservationSuppliesForLatestPastReservation() {
        return reservationSupplyTaskReadOnlyToolService.reservationSuppliesForLatestPastReservation();
    }

    public AiToolAnswer reservationSupplySummaryByItem(String userQuestion) {
        return reservationSupplyTaskReadOnlyToolService.reservationSupplySummaryByItem(userQuestion);
    }

    public AiToolAnswer reservationSupplySummaryByDateRange(String userQuestion) {
        return reservationSupplyTaskReadOnlyToolService.reservationSupplySummaryByDateRange(userQuestion);
    }

    public AiToolAnswer reservationSupplyLastUsed(String userQuestion) {
        return reservationSupplyTaskReadOnlyToolService.reservationSupplyLastUsed(userQuestion);
    }

    public AiToolAnswer reservationSupplyMostUsed() {
        return reservationSupplyTaskReadOnlyToolService.reservationSupplyMostUsed();
    }

    public AiToolAnswer reservationSupplyMissingForUpcomingReservations() {
        return reservationSupplyTaskReadOnlyToolService.reservationSupplyMissingForUpcomingReservations();
    }

    public AiToolAnswer taskListSearch(String userQuestion) {
        return reservationSupplyTaskReadOnlyToolService.taskListSearch(userQuestion);
    }

    public AiToolAnswer taskListsByProperty(String userQuestion) {
        return reservationSupplyTaskReadOnlyToolService.taskListsByProperty(userQuestion);
    }

    public AiToolAnswer taskListsByReservation(String userQuestion) {
        return reservationSupplyTaskReadOnlyToolService.taskListsByReservation(userQuestion);
    }

    public AiToolAnswer taskListsForNextReservation() {
        return reservationSupplyTaskReadOnlyToolService.taskListsForNextReservation();
    }

    public AiToolAnswer activeTaskLists() {
        return reservationSupplyTaskReadOnlyToolService.activeTaskLists();
    }

    public AiToolAnswer completedTaskLists() {
        return reservationSupplyTaskReadOnlyToolService.completedTaskLists();
    }

    public AiToolAnswer overdueTaskLists() {
        return reservationSupplyTaskReadOnlyToolService.overdueTaskLists();
    }

    public AiToolAnswer dueTodayTaskLists() {
        return reservationSupplyTaskReadOnlyToolService.dueTodayTaskLists();
    }

    public AiToolAnswer dueThisWeekTaskLists() {
        return reservationSupplyTaskReadOnlyToolService.dueThisWeekTaskLists();
    }

    public AiToolAnswer taskListProgressSummary() {
        return reservationSupplyTaskReadOnlyToolService.taskListProgressSummary();
    }

    public AiToolAnswer taskListCompletionSummary() {
        return reservationSupplyTaskReadOnlyToolService.taskListCompletionSummary();
    }

    public AiToolAnswer taskItemSearch(String userQuestion) {
        return reservationSupplyTaskReadOnlyToolService.taskItemSearch(userQuestion);
    }

    public AiToolAnswer taskItemsByTaskList(String userQuestion) {
        return reservationSupplyTaskReadOnlyToolService.taskItemsByTaskList(userQuestion);
    }

    public AiToolAnswer pendingTaskItems() {
        return reservationSupplyTaskReadOnlyToolService.pendingTaskItems();
    }

    public AiToolAnswer completedTaskItems() {
        return reservationSupplyTaskReadOnlyToolService.completedTaskItems();
    }

    public AiToolAnswer overdueTaskItems() {
        return reservationSupplyTaskReadOnlyToolService.overdueTaskItems();
    }

    public AiToolAnswer taskItemAssignedSummary() {
        return reservationSupplyTaskReadOnlyToolService.taskItemAssignedSummary();
    }

    public AiToolAnswer taskItemPrioritySummary() {
        return reservationSupplyTaskReadOnlyToolService.taskItemPrioritySummary();
    }

    public AiToolAnswer documentMetadata(String userQuestion) {
        return documentRagReadOnlyToolService.documentMetadata(userQuestion);
    }

    public AiToolAnswer ragDocumentIndexStatus() {
        return documentRagReadOnlyToolService.ragDocumentIndexStatus();
    }

    public AiToolAnswer documentByProperty(String userQuestion) {
        return documentRagReadOnlyToolService.documentByProperty(userQuestion);
    }

    public AiToolAnswer documentByType(String userQuestion) {
        return documentRagReadOnlyToolService.documentByType(userQuestion);
    }

    public AiToolAnswer documentByStatus(String userQuestion) {
        return documentRagReadOnlyToolService.documentByStatus(userQuestion);
    }

    public AiToolAnswer recentDocuments() {
        return documentRagReadOnlyToolService.recentDocuments();
    }

    public AiToolAnswer unprocessedDocuments() {
        return documentRagReadOnlyToolService.unprocessedDocuments();
    }

    public AiToolAnswer failedDocuments() {
        return documentRagReadOnlyToolService.failedDocuments();
    }

    public AiToolAnswer processedDocuments() {
        return documentRagReadOnlyToolService.processedDocuments();
    }

    public AiToolAnswer indexedDocuments() {
        return documentRagReadOnlyToolService.indexedDocuments();
    }

    public AiToolAnswer notIndexedDocuments() {
        return documentRagReadOnlyToolService.notIndexedDocuments();
    }

    public AiToolAnswer processedNotIndexedDocuments() {
        return documentRagReadOnlyToolService.processedNotIndexedDocuments();
    }

    public AiToolAnswer documentCountByType() {
        return documentRagReadOnlyToolService.documentCountByType();
    }

    public AiToolAnswer documentCountByProperty() {
        return documentRagReadOnlyToolService.documentCountByProperty();
    }

    public AiToolAnswer findBlueprintDocuments() {
        return documentRagReadOnlyToolService.findBlueprintDocuments();
    }

    public AiToolAnswer findHouseRulesDocuments() {
        return documentRagReadOnlyToolService.findHouseRulesDocuments();
    }

    public AiToolAnswer findManualDocuments() {
        return documentRagReadOnlyToolService.findManualDocuments();
    }

    public AiToolAnswer ragChunkSummary() {
        return documentRagReadOnlyToolService.ragChunkSummary();
    }

    public AiToolAnswer documentsMissingChunks() {
        return documentRagReadOnlyToolService.documentsMissingChunks();
    }

    public AiToolAnswer documentsMissingVectorIds() {
        return documentRagReadOnlyToolService.documentsMissingVectorIds();
    }

    public AiToolAnswer ragIndexCoverageSummary() {
        return documentRagReadOnlyToolService.ragIndexCoverageSummary();
    }

    public AiToolAnswer inventorySearch(String userQuestion) {
        return inventoryReadOnlyToolService.inventorySearch(userQuestion);
    }

    public AiToolAnswer inventoryFrequentlyUsed() {
        return inventoryReadOnlyToolService.inventoryFrequentlyUsed();
    }

    public AiToolAnswer inventoryUnusedItems() {
        return inventoryReadOnlyToolService.inventoryUnusedItems();
    }

    public AiToolAnswer inventoryReservationUsage(String userQuestion) {
        return inventoryReadOnlyToolService.inventoryReservationUsage(userQuestion);
    }

    public AiToolAnswer inventoryPurchaseUsage(String userQuestion) {
        return inventoryReadOnlyToolService.inventoryPurchaseUsage(userQuestion);
    }

    public AiToolAnswer inventoryMaintenanceUsage(String userQuestion) {
        return inventoryReadOnlyToolService.inventoryMaintenanceUsage(userQuestion);
    }

    public AiToolAnswer maintenanceSearch(String userQuestion) {
        return maintenanceReadOnlyToolService.maintenanceSearch(userQuestion);
    }

    public AiToolAnswer recentMaintenance() {
        return maintenanceReadOnlyToolService.recentMaintenance();
    }

    public AiToolAnswer maintenanceByStatus(String userQuestion) {
        return maintenanceReadOnlyToolService.maintenanceByStatus(userQuestion);
    }

    public AiToolAnswer maintenanceByProperty(String userQuestion) {
        return maintenanceReadOnlyToolService.maintenanceByProperty(userQuestion);
    }

    public AiToolAnswer maintenanceByCategoryOrType(String userQuestion) {
        return maintenanceReadOnlyToolService.maintenanceByCategoryOrType(userQuestion);
    }

    public AiToolAnswer maintenanceCostSummary(String userQuestion) {
        return maintenanceReadOnlyToolService.maintenanceCostSummary(userQuestion);
    }

    public AiToolAnswer maintenanceCostByProperty() {
        return maintenanceReadOnlyToolService.maintenanceCostByProperty();
    }

    public AiToolAnswer maintenanceCostByCategory() {
        return maintenanceReadOnlyToolService.maintenanceCostByCategory();
    }

    public AiToolAnswer maintenanceCostByMonth() {
        return maintenanceReadOnlyToolService.maintenanceCostByMonth();
    }

    public AiToolAnswer maintenanceImagesSummary(boolean withoutImages) {
        return maintenanceReadOnlyToolService.maintenanceImagesSummary(withoutImages);
    }

    public AiToolAnswer fileMetadata(String userQuestion) {
        return fileImageReadOnlyToolService.fileMetadata(userQuestion);
    }

    public AiToolAnswer filesByProperty(String userQuestion) {
        return fileImageReadOnlyToolService.filesByProperty(userQuestion);
    }

    public AiToolAnswer filesByMaintenance(String userQuestion) {
        return fileImageReadOnlyToolService.filesByMaintenance(userQuestion);
    }

    public AiToolAnswer filesByDocument(String userQuestion) {
        return fileImageReadOnlyToolService.filesByDocument(userQuestion);
    }

    public AiToolAnswer fileStorageSummary() {
        return fileImageReadOnlyToolService.fileStorageSummary();
    }

    public AiToolAnswer orphanFileCandidates() {
        return fileImageReadOnlyToolService.orphanFileCandidates();
    }

    public AiToolAnswer propertyImageMetadataSummary() {
        return fileImageReadOnlyToolService.propertyImageMetadataSummary();
    }

    public AiToolAnswer maintenanceImageMetadataSummary() {
        return fileImageReadOnlyToolService.maintenanceImageMetadataSummary();
    }

    public AiToolAnswer dashboardReservationSummary() {
        return dashboardReadOnlyToolService.dashboardReservationSummary();
    }

    public AiToolAnswer dashboardMaintenanceSummary() {
        return dashboardReadOnlyToolService.dashboardMaintenanceSummary();
    }

    public AiToolAnswer dashboardPurchaseSummary() {
        return dashboardReadOnlyToolService.dashboardPurchaseSummary();
    }

    public AiToolAnswer dashboardTaskSummary() {
        return dashboardReadOnlyToolService.dashboardTaskSummary();
    }

    public AiToolAnswer dashboardDocumentSummary() {
        return dashboardReadOnlyToolService.dashboardDocumentSummary();
    }

    public AiToolAnswer dashboardCalendarEvents() {
        return dashboardReadOnlyToolService.dashboardCalendarEvents();
    }

    public AiToolAnswer dashboardAlertSummary() {
        return dashboardReadOnlyToolService.dashboardAlertSummary();
    }

    public AiToolAnswer dashboardAttentionToday() {
        return dashboardReadOnlyToolService.dashboardAttentionToday();
    }

}
