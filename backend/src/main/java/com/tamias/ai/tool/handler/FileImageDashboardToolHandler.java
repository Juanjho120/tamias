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

        if (isCrossModuleImageOrFileDashboardQuestion(normalized)) {
            if (isLargestFileDashboardQuestion(normalized)) {
                return Optional.of(readOnlyToolService.largestFiles());
            }
            if (isRecentUploadDashboardQuestion(normalized)) {
                return Optional.of(readOnlyToolService.recentUploads());
            }
            if (isEntitiesWithoutImagesDashboardQuestion(normalized)) {
                return Optional.of(readOnlyToolService.entitiesWithoutImages());
            }
            if (isModuleImageDashboardSummaryQuestion(normalized)) {
                return Optional.of(readOnlyToolService.imageDashboardSummary());
            }
            if (isEntitiesWithMostImagesDashboardQuestion(normalized)) {
                return Optional.of(readOnlyToolService.entitiesWithMostImages());
            }
            return Optional.of(readOnlyToolService.imageDashboardSummary());
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
            if (isLargestFileDashboardQuestion(normalized)) {
                return Optional.of(readOnlyToolService.largestFiles());
            }
            if (isRecentUploadDashboardQuestion(normalized)) {
                return Optional.of(readOnlyToolService.recentUploads());
            }
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

    private boolean isCrossModuleImageOrFileDashboardQuestion(String normalized) {
        boolean imageOrFile = containsAny(
                normalized,
                "imagen", "imagenes", "image", "images", "foto", "fotos",
                "archivo", "archivos", "file", "files", "upload", "uploads", "subido", "subidos",
                "almacenamiento", "storage", "espacio", "tamano", "tamaño", "peso", "pesados"
        );
        if (!imageOrFile) {
            return false;
        }

        boolean crossModuleScope = containsAny(
                normalized,
                "tamias", "modulo", "modulos", "módulo", "módulos", "por modulo", "por módulo",
                "entidad", "entidades", "general", "global", "todos", "todas", "total", "totales",
                "dashboard", "resumen", "cuantas", "cuántas", "cuantos", "cuántos"
        );
        return crossModuleScope
                || isLargestFileDashboardQuestion(normalized)
                || isRecentUploadDashboardQuestion(normalized)
                || isEntitiesWithoutImagesDashboardQuestion(normalized)
                || isEntitiesWithMostImagesDashboardQuestion(normalized);
    }

    private boolean isModuleImageDashboardSummaryQuestion(String normalized) {
        return containsAny(normalized, "modulo", "modulos", "módulo", "módulos", "por modulo", "por módulo")
                && containsAny(normalized, "imagen", "imagenes", "image", "images", "foto", "fotos", "cantidad", "cuantas", "cuántas", "cuantos", "cuántos", "mas", "más");
    }

    private boolean isRecentUploadDashboardQuestion(String normalized) {
        return containsAny(
                normalized,
                "reciente", "recientes", "ultimos", "últimos", "ultimas", "últimas",
                "subido", "subidos", "subida", "subidas", "cargado", "cargados", "cargada", "cargadas",
                "uploaded", "recent uploads"
        );
    }

    private boolean isLargestFileDashboardQuestion(String normalized) {
        return containsAny(
                normalized,
                "mas grande", "más grande", "mas grandes", "más grandes", "mayor tamano", "mayor tamaño",
                "pesado", "pesados", "peso", "ocupan mas", "ocupan más", "espacio", "largest", "biggest"
        );
    }

    private boolean isEntitiesWithoutImagesDashboardQuestion(String normalized) {
        return containsAny(
                normalized,
                "sin imagen", "sin imagenes", "sin imágenes", "sin foto", "sin fotos",
                "no tienen imagen", "no tienen imagenes", "no tienen imágenes", "no tienen foto", "no tienen fotos",
                "missing images", "without images"
        ) && containsAny(normalized, "entidad", "entidades", "modulo", "modulos", "módulo", "módulos", "tamias", "general", "global", "todos", "todas");
    }

    private boolean isEntitiesWithMostImagesDashboardQuestion(String normalized) {
        return containsAny(
                normalized,
                "mas imagen", "más imagen", "mas imagenes", "más imágenes", "mas fotos", "más fotos",
                "tienen mas", "tienen más", "con mas", "con más", "top", "ranking", "most images"
        ) && containsAny(normalized, "entidad", "entidades", "propiedad", "propiedades", "item", "items", "reservacion", "reservaciones", "compra", "compras", "mantenimiento", "mantenimientos", "tamias");
    }
}
