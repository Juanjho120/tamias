package com.tamias.ai.tool;

import java.util.List;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(50)
public class FileImageDashboardToolHandler extends AiToolRoutingSupport implements AiToolHandler {

    private final AiReadOnlyToolService readOnlyToolService;

    public FileImageDashboardToolHandler(AiReadOnlyToolService readOnlyToolService) {
        this.readOnlyToolService = readOnlyToolService;
    }

    @Override
    public Optional<AiToolAnswer> tryHandle(AiToolRequestContext context) {
        return tryHandleFileImageDashboardQuestion(context.question(), context.normalizedQuestion());
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
}
