package com.tamias.ai.tool.support;

import com.tamias.ai.dto.AiToolEvidenceResponse;
import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.AiToolResult;
import java.util.List;

public final class AiToolFallbackPolicy {

    private AiToolFallbackPolicy() {
    }

    public static AiToolResult classify(AiToolAnswer answer, String normalizedQuestion) {
        String toolName = primaryToolName(answer);
        String normalizedAnswer = AiToolTextNormalizer.normalizeForRouting(answer != null ? answer.answer() : "");

        if ("assistant.readOnlyGuard".equals(toolName)) {
            return AiToolResult.guardrail(answer);
        }

        if (isAdminOnlyDenied(answer, normalizedAnswer)) {
            return AiToolResult.denied(answer);
        }

        if (isEmptyToolAnswer(answer, normalizedAnswer)) {
            return AiToolResult.empty(answer, allowRagFallback(toolName, normalizedQuestion));
        }

        return AiToolResult.hit(answer);
    }

    public static boolean isFallbackAllowed(AiToolResult result) {
        return result != null && result.allowRagFallback() && result.status().allowsRagFallback();
    }

    public static String primaryToolName(AiToolAnswer answer) {
        if (answer == null || answer.evidence().isEmpty()) {
            return "";
        }
        String toolName = answer.evidence().get(0).toolName();
        return toolName == null ? "" : toolName;
    }

    private static boolean isAdminOnlyDenied(AiToolAnswer answer, String normalizedAnswer) {
        if (answer == null) {
            return false;
        }

        boolean deniedBySummary = answer.evidence().stream()
                .map(AiToolEvidenceResponse::summary)
                .filter(summary -> summary != null)
                .map(AiToolTextNormalizer::normalizeForRouting)
                .anyMatch(summary -> summary.contains("admin-only ai tool blocked"));

        return deniedBySummary
                || normalizedAnswer.contains("solo esta disponible para usuarios con rol administrator")
                || normalizedAnswer.contains("no puedo listar usuarios roles ni accesos");
    }

    private static boolean isEmptyToolAnswer(AiToolAnswer answer, String normalizedAnswer) {
        if (answer == null) {
            return true;
        }

        String toolName = primaryToolName(answer);
        if (isAlwaysTerminalTool(toolName)) {
            return false;
        }

        boolean hasEvidenceItems = answer.evidence().stream()
                .map(AiToolEvidenceResponse::items)
                .anyMatch(items -> items != null && !items.isEmpty());
        if (hasEvidenceItems) {
            return false;
        }

        boolean emptyByEvidenceSummary = answer.evidence().stream()
                .map(AiToolEvidenceResponse::summary)
                .filter(summary -> summary != null)
                .map(AiToolTextNormalizer::normalizeForRouting)
                .anyMatch(AiToolFallbackPolicy::looksLikeEmptySummary);

        return emptyByEvidenceSummary || looksLikeEmptyAnswer(normalizedAnswer);
    }

    private static boolean looksLikeEmptySummary(String normalizedSummary) {
        return normalizedSummary.startsWith("no ")
                || normalizedSummary.contains(" no ")
                || normalizedSummary.contains("not found")
                || normalizedSummary.contains("no matching")
                || normalizedSummary.contains("no rows")
                || normalizedSummary.contains("no data")
                || normalizedSummary.contains("no document")
                || normalizedSummary.contains("no users")
                || normalizedSummary.contains("no role")
                || normalizedSummary.contains("no properties")
                || normalizedSummary.contains("no matching");
    }

    private static boolean looksLikeEmptyAnswer(String normalizedAnswer) {
        if (normalizedAnswer == null || normalizedAnswer.isBlank()) {
            return true;
        }

        return normalizedAnswer.startsWith("no encontre")
                || normalizedAnswer.contains(" no encontre")
                || normalizedAnswer.startsWith("no veo")
                || normalizedAnswer.contains(" no veo")
                || normalizedAnswer.startsWith("no hay")
                || normalizedAnswer.contains(" no hay")
                || normalizedAnswer.startsWith("no tengo")
                || normalizedAnswer.contains(" no tengo")
                || normalizedAnswer.startsWith("todas las propiedades consultadas tienen")
                || normalizedAnswer.startsWith("todas las propiedades tienen");
    }

    private static boolean isAlwaysTerminalTool(String toolName) {
        return List.of(
                "assistant.capabilities",
                "assistant.readOnlyGuard",
                "user.currentProfile",
                "organization.currentSummary",
                "role.list",
                "role.permissionSummary",
                "organization.userCount",
                "organization.moduleUsageSummary",
                "aiChat.usageSummary",
                "aiChat.currentSessionSummary",
                "scheduledMaintenance.nextDue",
                "image.maintenanceImagesSummary",
                "maintenance.withImages",
                "maintenance.withoutImages",
                "document.processedNotIndexed",
                "document.failedProcessing",
                "files.getImageDashboardSummary",
                "files.getImageCountByModule",
                "files.getTopImageModule",
                "files.getImageStorageSummary",
                "files.getFileNameList",
                "productBox.summary",
                "productBox.search",
                "productBox.incompleteModels",
                "productBox.inventoryLinks",
                "productBox.inventoryItemsWithoutModel",
                "productBox.purchaseLinks",
                "productBox.textureStatus"
        ).contains(toolName);
    }

    private static boolean allowRagFallback(String toolName, String normalizedQuestion) {
        if (toolName == null || toolName.isBlank()) {
            return true;
        }

        if (toolName.startsWith("user.")
                || toolName.startsWith("role.")
                || toolName.startsWith("organization.")
                || toolName.startsWith("aiChat.")
                || toolName.startsWith("dashboard.")
                || toolName.startsWith("productBox.")) {
            return false;
        }

        if (toolName.startsWith("rag.")) {
            return false;
        }

        if (questionLooksStrictlyOperational(normalizedQuestion)) {
            return false;
        }

        return toolName.startsWith("document.")
                || toolName.startsWith("file.")
                || toolName.startsWith("image.")
                || toolName.startsWith("property.")
                || toolName.startsWith("catalog.")
                || toolName.startsWith("maintenance.")
                || toolName.startsWith("scheduledMaintenance.")
                || toolName.startsWith("reservation.")
                || toolName.startsWith("guest.")
                || toolName.startsWith("purchase")
                || toolName.startsWith("inventory.")
                || toolName.startsWith("task")
                || toolName.startsWith("assistant.");
    }

    private static boolean questionLooksStrictlyOperational(String normalizedQuestion) {
        if (normalizedQuestion == null || normalizedQuestion.isBlank()) {
            return false;
        }

        boolean hasDocumentSignals = AiToolTextNormalizer.containsAnyForRouting(
                normalizedQuestion,
                "documento",
                "documentos",
                "archivo",
                "archivos",
                "pdf",
                "manual",
                "plano",
                "regla",
                "reglas",
                "rag",
                "indexado",
                "indexacion",
                "contenido",
                "texto",
                "dice",
                "menciona",
                "habla de",
                "segun el documento"
        );
        if (hasDocumentSignals) {
            return false;
        }

        return AiToolTextNormalizer.containsAnyForRouting(
                normalizedQuestion,
                "cuantos",
                "cuantas",
                "cuanto",
                "cuanta",
                "total",
                "conteo",
                "activos",
                "inactivos",
                "vencidos",
                "pendientes",
                "completadas",
                "completados",
                "por estado",
                "por propiedad",
                "por categoria",
                "por mes",
                "ultimas conversaciones",
                "historial",
                "usuarios",
                "roles",
                "modulos",
                "dashboard",
                "alertas operativas",
                "product box",
                "productbox",
                "productboxmodel",
                "productboxmodels",
                "product box model",
                "product box models",
                "box model",
                "box models",
                "modelo product box",
                "modelos product box",
                "modelo de caja",
                "modelos de caja",
                "modelo 3d",
                "modelos 3d",
                "caja 3d",
                "cajas 3d"
        );
    }
}
