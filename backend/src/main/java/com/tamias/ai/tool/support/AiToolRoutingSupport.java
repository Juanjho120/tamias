package com.tamias.ai.tool.support;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.dto.AiToolEvidenceResponse;
import java.util.ArrayList;
import java.util.List;

public abstract class AiToolRoutingSupport {

    protected AiToolAnswer combine(
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

    protected String resolveSectionTitle(AiToolAnswer answer) {
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

    protected AiToolAnswer readOnlyGuard() {
        return AiToolAnswer.of(
                "Puedo ayudarte a revisar información que ya existe en TAMIAS, pero por seguridad todavía no creo, edito, elimino ni envío datos desde el asistente.\n"
                        + "Dime qué quieres revisar y te ayudo a encontrarlo para que tú hagas el cambio desde el módulo correspondiente.",
                "assistant.readOnlyGuard",
                "Read-only guard",
                "The user asked for an action that would modify data. The assistant refused autonomous writes.",
                List.of()
        );
    }


    protected boolean isFileMetadataQuestion(String value) {
        return containsAny(value,
                "archivo", "archivos", "file", "files", "metadata de archivos", "metadatos de archivos", "almacenados", "almacenamiento", "bucket", "s3"
        ) && !isDocumentCountByTypeQuestion(value) && !isDocumentCountByPropertyQuestion(value);
    }

    protected boolean isFileByPropertyQuestion(String value) {
        return isFileMetadataQuestion(value) && containsAny(value, "propiedad", "propiedades", "alojamiento", "casa", "bungalow", "asociados a esta propiedad", "para esta propiedad");
    }

    protected boolean isFileByMaintenanceQuestion(String value) {
        return isFileMetadataQuestion(value) && containsAny(value, "mantenimiento", "mantenimientos", "evidencia", "fotos de mantenimiento", "imagenes de mantenimiento");
    }

    protected boolean isFileByDocumentQuestion(String value) {
        return isFileMetadataQuestion(value) && containsAny(value, "documento", "documentos", "pdf", "manual", "plano", "regla");
    }

    protected boolean isFileStorageSummaryQuestion(String value) {
        return isFileMetadataQuestion(value) && containsAny(value, "cuantos archivos", "cuántos archivos", "almacenados", "almacenamiento", "storage", "tamano", "tamaño", "peso", "espacio");
    }

    protected boolean isFileOrphanCandidateQuestion(String value) {
        return isFileMetadataQuestion(value) && containsAny(value, "huerfano", "huérfano", "huerfanos", "huérfanos", "orphan", "sin asociar", "no asociados");
    }

    protected boolean isImageMetadataQuestion(String value) {
        return containsAny(value, "imagen", "imagenes", "foto", "fotos", "evidencia fotografica", "evidencia fotográfica");
    }

    protected boolean isPropertyImageMetadataQuestion(String value) {
        return isImageMetadataQuestion(value) && containsAny(value, "propiedad", "propiedades", "portada", "portadas", "casas", "bungalow");
    }

    protected boolean isMaintenanceImageMetadataQuestion(String value) {
        return isImageMetadataQuestion(value) && containsAny(value, "mantenimiento", "mantenimientos", "evidencia");
    }

    protected boolean isDashboardAnalyticsQuestion(String value) {
        return containsAny(value,
                "dashboard", "tablero", "resumen ejecutivo", "resumen operativo", "estado general", "alertas", "atencion hoy", "atención hoy",
                "calendario operativo", "eventos operativos", "summary", "analytics", "analitica", "analítica"
        );
    }

    protected boolean isDashboardReservationSummaryQuestion(String value) {
        return isDashboardAnalyticsQuestion(value) && containsAny(value, "reservacion", "reservaciones", "reserva", "reservas", "check in", "check-in", "ocupacion", "ocupación");
    }

    protected boolean isDashboardMaintenanceSummaryQuestion(String value) {
        return isDashboardAnalyticsQuestion(value) && containsAny(value, "mantenimiento", "mantenimientos", "reparacion", "reparaciones");
    }

    protected boolean isDashboardPurchaseSummaryQuestion(String value) {
        return isDashboardAnalyticsQuestion(value) && containsAny(value, "compra", "compras", "gasto", "gastos", "supplies");
    }

    protected boolean isDashboardTaskSummaryQuestion(String value) {
        return isDashboardAnalyticsQuestion(value) && containsAny(value, "tarea", "tareas", "task", "tasks", "pendiente", "pendientes");
    }

    protected boolean isDashboardDocumentSummaryQuestion(String value) {
        return isDashboardAnalyticsQuestion(value) && containsAny(value, "documento", "documentos", "rag", "indice", "índice", "indexacion", "indexación");
    }

    protected boolean isDashboardCalendarQuestion(String value) {
        return isDashboardAnalyticsQuestion(value) && containsAny(value, "calendario", "calendar", "eventos", "agenda", "proximos eventos", "próximos eventos");
    }

    protected boolean isDashboardAttentionTodayQuestion(String value) {
        return isDashboardAnalyticsQuestion(value)
                && containsAny(value, "necesita atencion hoy", "necesita atención hoy", "que necesita atencion", "qué necesita atención", "atencion hoy", "atención hoy");
    }

    protected boolean isDashboardAlertQuestion(String value) {
        return isDashboardAnalyticsQuestion(value) && containsAny(value, "alerta", "alertas", "vencido", "vencidos", "fallido", "fallidos", "riesgo", "riesgos");
    }



    protected boolean isUserAdminToolQuestion(String value) {
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



    protected boolean isUserAccessIntent(String value) {
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


    protected boolean isActiveUsersQuestion(String value) {
        return isUserAdminToolQuestion(value)
                && containsAny(value, "activos", "activas", "activo", "active")
                && !containsAny(value, "inactivos", "inactivas", "no activos", "inactive");
    }

    protected boolean isInactiveUsersQuestion(String value) {
        return isUserAdminToolQuestion(value)
                && containsAny(value, "inactivos", "inactivas", "no activos", "inactive", "bloqueados", "locked", "invited", "invitados");
    }


    protected boolean isUsersByRoleQuestion(String value) {
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


    protected boolean isUserAccessSummaryQuestion(String value) {
        return isUserAdminToolQuestion(value) && isUserAccessIntent(value);
    }


    protected boolean isRoleAdminToolQuestion(String value) {
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



    protected boolean isRolePermissionSummaryQuestion(String value) {
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


    protected boolean isOrganizationAdminToolQuestion(String value) {
        return containsAny(value, "organizacion", "organización", "empresa", "modulo", "modulos", "módulo", "módulos", "module", "modules")
                && containsAny(value, "usuarios", "usuario", "modulos", "módulos", "uso", "usando", "usamos", "module usage", "cuantos usuarios", "cuántos usuarios");
    }

    protected boolean isOrganizationUserCountQuestion(String value) {
        return isOrganizationAdminToolQuestion(value)
                && containsAny(value, "usuarios", "cuantos usuarios", "cuántos usuarios", "user count", "cantidad de usuarios");
    }

    protected boolean isOrganizationModuleUsageQuestion(String value) {
        return isOrganizationAdminToolQuestion(value)
                && containsAny(value, "modulo", "modulos", "módulo", "módulos", "uso", "usando", "usamos", "module", "modules", "module usage", "usando mas", "usamos mas");
    }



    protected boolean isAiChatHistoryQuestion(String value) {
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

    protected boolean isAiChatLastPreviousSessionQuestion(String value) {
        return isAiChatHistoryQuestion(value)
                && containsAny(value, "que hemos hablado antes", "hemos hablado antes", "que hablamos antes", "ultima conversacion", "última conversación")
                && !containsAny(value, "busca", "buscar", "sobre", "cuantas", "cuantos", "conteo", "cantidad");
    }

    protected boolean isAiChatUsageSummaryQuestion(String value) {
        return isAiChatHistoryQuestion(value)
                && containsAny(value, "cuantas", "cuantos", "conteo", "cantidad", "uso", "resumen de uso", "estadistica", "estadisticas");
    }

    protected boolean isAiChatCurrentSessionQuestion(String value) {
        return isAiChatHistoryQuestion(value)
                && containsAny(value, "esta conversacion", "esta sesion", "sesion actual", "conversacion actual", "resumen de esta", "resume esta", "resumeme esta");
    }

    protected boolean isAiChatByPropertyQuestion(String value) {
        return isAiChatHistoryQuestion(value)
                && containsAny(value, "propiedad", "propiedades", "alojamiento", "alojamientos", "casa", "casas", "bungalow", "bungalows");
    }

    protected boolean isAiChatRecentMessagesQuestion(String value) {
        return isAiChatHistoryQuestion(value)
                && containsAny(value, "mensajes", "preguntas", "respuestas", "que he preguntado", "que pregunte", "preguntas le hice", "le hice al asistente", "que preguntas", "ultimos mensajes", "ultimas preguntas");
    }

    protected boolean isAiChatSearchHistoryQuestion(String value) {
        return isAiChatHistoryQuestion(value)
                && containsAny(value, "busca", "buscar", "sobre", "relacionado", "relacionados", "mencione", "mencionamos", "hablamos de", "pregunte sobre");
    }

    protected boolean isCapabilitiesQuestion(String value) {
        return containsAny(value,
                "que puedes hacer", "que sabes hacer", "como me ayudas", "en que me ayudas",
                "que tipo de asistente eres", "capacidades", "funciones", "herramientas", "que puedes consultar"
        );
    }

    protected boolean isCurrentUserProfileQuestion(String value) {
        return containsAny(value,
                "como me llamo", "cual es mi nombre", "mi nombre", "cual es mi correo", "mi correo", "mi email",
                "que usuario estoy usando", "usuario estoy usando", "mi usuario", "cual es mi rol", "mi rol",
                "mi perfil", "perfil actual", "numero de telefono", "mi telefono", "mi celular", "recuerdame mi numero"
        );
    }

    protected boolean isOrganizationQuestion(String value) {
        return containsAny(value,
                "mi organizacion", "organizacion actual", "empresa actual", "resumen de organizacion", "resumen de la organizacion"
        );
    }

    protected boolean isPropertyQuestion(String value) {
        return containsAny(value,
                "propiedad", "propiedades", "alojamiento", "alojamientos", "bungalow", "bungalows", "casa", "casas", "cabin", "cabins", "cabana", "cabanas"
        );
    }

    protected boolean isActivePropertyQuestion(String value) {
        return isPropertyQuestion(value) && containsAny(value, "activas", "activa", "activos", "activo")
                && !containsAny(value, "inactivas", "inactiva", "inactivos", "inactivo");
    }

    protected boolean isInactivePropertyQuestion(String value) {
        return isPropertyQuestion(value) && containsAny(value, "inactivas", "inactiva", "inactivos", "inactivo");
    }

    protected boolean isPropertyImagesQuestion(String value) {
        return isPropertyQuestion(value) && containsAny(value, "imagen", "imagenes", "foto", "fotos", "portada", "sin imagen", "sin imagenes");
    }

    protected boolean isPropertyOperationalOverviewQuestion(String value) {
        return isPropertyQuestion(value) && containsAny(value,
                "panorama operativo", "resumen operativo", "operacion", "operativo", "mas mantenimientos", "mas mantenimiento", "pendientes por propiedad", "estado general"
        );
    }

    protected boolean isPropertySummaryQuestion(String value) {
        return isPropertyQuestion(value) && containsAny(value,
                "resumen", "resume", "detalle", "descripcion", "dame un resumen", "informacion de"
        );
    }

    protected boolean isCatalogQuestion(String value) {
        return containsAny(value,
                "catalogo", "catalogos", "categoria", "categorias", "tipo de mantenimiento", "tipos de mantenimiento",
                "plataforma", "plataformas", "task template", "plantilla", "plantillas", "inventory item type",
                "inventory item", "tipos de item", "tipos de inventario", "tipos de inventory item"
        );
    }

    protected boolean isMaintenanceCatalogOverviewQuestion(String value) {
        return containsAny(value, "catalogos puedo usar", "catalogo puedo usar", "catalogos para mantenimiento", "catalogo para mantenimiento", "catalogos de mantenimiento", "catalogo de mantenimiento")
                || (containsAny(value, "catalogo", "catalogos")
                && containsAny(value, "usar", "puedo usar", "disponible", "disponibles")
                && containsAny(value, "mantenimiento", "mantenimientos"));
    }

    protected boolean isMaintenanceCategoryQuestion(String value) {
        return containsAny(value, "categorias de mantenimiento", "categoria de mantenimiento", "maintenance categories", "maintenance category")
                || (containsAny(value, "categoria", "categorias") && containsAny(value, "mantenimiento", "mantenimientos"));
    }

    protected boolean isMaintenanceTypeQuestion(String value) {
        return containsAny(value, "tipos de mantenimiento", "tipo de mantenimiento", "maintenance types", "maintenance type")
                || (containsAny(value, "tipo", "tipos") && containsAny(value, "mantenimiento", "mantenimientos"));
    }

    protected boolean isReservationPlatformQuestion(String value) {
        return containsAny(value, "plataformas", "plataforma", "reservation platforms", "airbnb", "booking")
                && containsAny(value, "reservacion", "reservaciones", "reserva", "reservas", "plataforma", "plataformas");
    }

    protected boolean isTaskCategoryQuestion(String value) {
        return containsAny(value, "categorias de tareas", "categoria de tareas", "plantillas de tareas", "plantilla de tareas", "task categories", "task templates")
                || (containsAny(value, "categoria", "categorias", "plantilla", "plantillas") && containsAny(value, "tarea", "tareas"));
    }

    protected boolean isPurchaseCategoryQuestion(String value) {
        return containsAny(value, "categorias de compras", "categoria de compras", "catalogos de compras", "catalogo de compras", "purchase categories")
                || (containsAny(value, "categoria", "categorias", "catalogo", "catalogos") && containsAny(value, "compra", "compras", "supply", "supplies"));
    }

    protected boolean isInventoryItemTypeQuestion(String value) {
        return containsAny(value,
                "tipos de item", "tipos de inventario", "inventory item types", "item types", "tipos de supplies",
                "tipos de inventory item", "inventory item existen", "inventory items existen", "tipo de inventory item"
        );
    }


    protected boolean isInventoryQuestion(String value) {
        return containsAny(value,
                "inventario", "inventory", "inventory item", "inventory items", "item de inventario", "items de inventario",
                "item registrado", "items registrados", "supplies", "supply", "suministro", "suministros", "repuesto", "repuestos", "material", "materiales"
        ) || (containsAny(value, "item", "items") && containsAny(value, "registrado", "registrados", "usado", "usados", "usaron", "usan", "uso", "nunca", "reservacion", "reservaciones", "mantenimiento", "mantenimientos", "compra", "compras", "frecuente", "frecuentes", "mas"));
    }

    protected boolean isInventoryWhereUsedQuestion(String value) {
        return containsAny(value, "donde se ha usado", "donde se uso", "donde use", "donde he usado", "en donde se uso", "en que se uso")
                || (containsAny(value, "donde") && containsAny(value, "usado", "uso", "use"));
    }

    protected boolean isInventoryUnusedQuestion(String value) {
        return isInventoryQuestion(value) && containsAny(value, "nunca", "sin uso", "no usados", "no he usado", "nunca he usado", "unused");
    }

    protected boolean isInventoryFrequentlyUsedQuestion(String value) {
        return isInventoryQuestion(value) && containsAny(value, "mas usados", "usan mas", "uso mas", "frecuentes", "frecuentemente", "frequently", "top", "mas se usan");
    }

    protected boolean isInventoryReservationUsageQuestion(String value) {
        return isInventoryQuestion(value) && containsAny(value, "reservacion", "reservaciones", "reserva", "reservas", "huesped", "huespedes");
    }

    protected boolean isInventoryPurchaseUsageQuestion(String value) {
        return isInventoryQuestion(value) && containsAny(value, "compra", "compras", "comprado", "compre", "precio", "costo");
    }

    protected boolean isInventoryMaintenanceUsageQuestion(String value) {
        return isInventoryQuestion(value) && containsAny(value, "mantenimiento", "mantenimientos", "reparacion", "reparaciones");
    }

    protected boolean isMaintenanceAnalyticsQuestion(String value) {
        return containsAny(value, "mantenimiento", "mantenimientos", "reparacion", "reparaciones")
                && !isLastMaintenanceQuestion(value)
                && !isOverdueScheduledMaintenanceQuestion(value)
                && !containsAny(value, "programado vencido", "programados vencidos", "proximo mantenimiento programado");
    }

    protected boolean isMaintenanceRecentQuestion(String value) {
        return isMaintenanceAnalyticsQuestion(value) && containsAny(value, "recientes", "reciente", "ultimos", "ultimas", "lista", "listar", "buscar", "busca");
    }

    protected boolean isMaintenanceCostQuestion(String value) {
        return isMaintenanceAnalyticsQuestion(value) && containsAny(value, "costo", "costos", "gaste", "gasto", "gastado", "cuanto", "total", "precio", "caros", "caras", "dinero");
    }

    protected boolean isMaintenanceCostByPropertyQuestion(String value) {
        return isMaintenanceCostQuestion(value) && containsAny(value, "por propiedad", "propiedad genero", "propiedad tiene", "propiedades");
    }

    protected boolean isMaintenanceCostByCategoryQuestion(String value) {
        return isMaintenanceCostQuestion(value) && containsAny(value, "por categoria", "categoria", "categorias");
    }

    protected boolean isMaintenanceCostByMonthQuestion(String value) {
        return isMaintenanceCostQuestion(value) && containsAny(value, "por mes", "mensual", "mes a mes", "tendencia", "meses");
    }

    protected boolean isMaintenanceImageQuestion(String value) {
        return isMaintenanceAnalyticsQuestion(value) && containsAny(value, "imagen", "imagenes", "foto", "fotos", "evidencia", "fotografica", "fotografica", "sin imagen", "sin evidencia");
    }

    protected boolean isMaintenanceStatusQuestion(String value) {
        return isMaintenanceAnalyticsQuestion(value) && containsAny(value, "estado", "completado", "completados", "pendiente", "pendientes", "cancelado", "cancelados", "progreso");
    }

    protected boolean isMaintenancePropertyFilterQuestion(String value) {
        return isMaintenanceAnalyticsQuestion(value) && containsAny(value, "propiedad", "casa", "bungalow", "alojamiento") && containsAny(value, "de", "del", "para", "en");
    }

    protected boolean isMaintenanceCategoryOrTypeFilterQuestion(String value) {
        return isMaintenanceAnalyticsQuestion(value) && containsAny(value, "categoria", "categorias", "tipo", "tipos", "filtro", "cisterna", "bomba", "pozo");
    }

    protected boolean isMaintenanceItemUsageQuestion(String value) {
        return isMaintenanceAnalyticsQuestion(value) && containsAny(value, "item", "items", "repuesto", "repuestos", "material", "materiales", "supply", "supplies", "usaron", "usado", "uso");
    }

    protected boolean isScheduledMaintenanceToolQuestion(String value) {
        return containsAny(value, "mantenimiento programado", "mantenimientos programados", "scheduled maintenance", "programado", "programados")
                || (containsAny(value, "mantenimiento", "mantenimientos")
                && containsAny(value, "toca", "vencido", "vencidos", "vence", "vencen", "proximo", "proxima", "proximos", "proximas", "frecuencia", "historial", "cumplimiento"));
    }

    protected boolean isScheduledMaintenanceUpcomingQuestion(String value) {
        return isScheduledMaintenanceToolQuestion(value)
                && containsAny(value, "proximos", "proximas", "upcoming", "siguientes", "lista", "listar", "muestrame", "muéstrame");
    }

    protected boolean isScheduledMaintenanceDueTodayQuestion(String value) {
        return isScheduledMaintenanceToolQuestion(value) && containsAny(value, "hoy", "today");
    }

    protected boolean isScheduledMaintenanceDueThisWeekQuestion(String value) {
        return isScheduledMaintenanceToolQuestion(value) && containsAny(value, "semana", "week", "esta semana");
    }

    protected boolean isScheduledMaintenanceNextDueQuestion(String value) {
        return isScheduledMaintenanceToolQuestion(value)
                && (containsAny(value, "toca", "vence", "vencen", "proximo vencimiento", "siguiente vencimiento")
                || containsAny(value, "cual es el proximo mantenimiento programado", "cual es la proxima mantenimiento programado",
                "proximo mantenimiento programado", "proxima mantenimiento programado", "siguiente mantenimiento programado"));
    }

    protected boolean isScheduledMaintenanceByPropertyQuestion(String value) {
        return isScheduledMaintenanceToolQuestion(value) && containsAny(value, "propiedad", "casa", "bungalow", "alojamiento");
    }

    protected boolean isScheduledMaintenanceByTypeQuestion(String value) {
        return isScheduledMaintenanceToolQuestion(value) && containsAny(value, "tipo", "categoria", "pozo", "cisterna", "filtro", "bomba");
    }

    protected boolean isScheduledMaintenanceByStatusQuestion(String value) {
        return isScheduledMaintenanceToolQuestion(value) && containsAny(value, "estado", "activo", "activos", "pausado", "pausados", "completado", "completados", "cancelado", "cancelados");
    }

    protected boolean isScheduledMaintenanceFrequencyQuestion(String value) {
        return isScheduledMaintenanceToolQuestion(value) && containsAny(value, "frecuencia", "frecuencias", "cada cuanto", "periodicidad");
    }

    protected boolean isScheduledMaintenanceHistoryQuestion(String value) {
        return isScheduledMaintenanceToolQuestion(value) && containsAny(value, "historial", "historia", "historico", "registro", "registros");
    }

    protected boolean isScheduledMaintenanceComplianceQuestion(String value) {
        return isScheduledMaintenanceToolQuestion(value) && containsAny(value, "cumplimiento", "compliance", "estado general", "resumen", "salud", "situacion");
    }

    protected boolean isReservationToolQuestion(String value) {
        return containsAny(value, "reservacion", "reservaciones", "reserva", "reservas", "check in", "check-in", "check out", "check-out", "llegada", "llegan", "llega", "entrada", "entradas", "salida", "salen", "sale", "ocupacion", "noches reservadas", "dias libres");
    }

    protected boolean isReservationTodayQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "hoy", "today");
    }

    protected boolean isReservationThisWeekQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "semana", "esta semana", "week");
    }

    protected boolean isReservationThisMonthQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "mes", "este mes", "month");
    }

    protected boolean isCurrentReservationToolQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "actual", "actuales", "en curso", "hoy hospedados", "ocupado actualmente");
    }

    protected boolean isReservationByPropertyQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "propiedad", "casa", "bungalow", "alojamiento");
    }

    protected boolean isReservationByGuestQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "huesped", "huespedes", "cliente", "clientes", "guest", "guests");
    }

    protected boolean isReservationByStatusQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "estado", "activas", "canceladas", "cancelados", "cancelada", "cancelado");
    }

    protected boolean isReservationByPlatformQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "plataforma", "platform", "airbnb", "booking");
    }

    protected boolean isReservationOccupancyQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "ocupacion", "ocupada", "ocupadas", "mas ocupacion", "reserved nights", "noches por propiedad");
    }

    protected boolean isReservationRevenueQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "ingreso", "ingresos", "revenue", "valor", "dinero", "monto", "total", "cuanto", "gane", "ganado");
    }

    protected boolean isReservationNightsQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "noches", "nights", "noches reservadas");
    }

    protected boolean isReservationGuestCountQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "cuantos huespedes", "cantidad de huespedes", "huespedes tendre", "guest count");
    }

    protected boolean isReservationCalendarQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "calendario", "calendar", "eventos");
    }

    protected boolean isNextCheckInQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value,
                "proximo check in", "proximo check-in", "proxima llegada", "proxima entrada", "siguiente entrada",
                "cual es la proxima entrada", "quien llega", "llega manana", "llega mañana", "siguiente llegada"
        );
    }

    protected boolean isNextCheckOutQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "proximo check out", "proximo check-out", "proxima salida", "quien sale", "sale manana", "sale mañana", "siguiente salida");
    }

    protected boolean isReservationGapQuestion(String value) {
        return isReservationToolQuestion(value) && containsAny(value, "dias libres", "espacios libres", "huecos", "gaps", "entre reservas", "entre reservaciones");
    }

    protected boolean isGuestToolQuestion(String value) {
        return containsAny(value, "huesped", "huespedes", "guest", "guests", "cliente", "clientes")
                && !containsAny(value, "supplies", "supply", "suministro", "suministros");
    }

    protected boolean isGuestInformationLookupQuestion(String value) {
        return containsAny(value,
                "quien es", "quién es", "que sabes sobre", "qué sabes sobre", "dame informacion sobre", "dame información sobre", "informacion sobre", "información sobre"
        ) && !containsAny(value,
                "propiedad", "propiedades", "casa", "bungalow", "alojamiento", "documento", "documentos", "pdf", "manual", "plano",
                "reservacion", "reservaciones", "reserva", "reservas", "mantenimiento", "mantenimientos", "tarea", "tareas", "compra", "compras",
                "catalogo", "catalogos", "plataforma", "plataformas", "inventario", "inventory", "supply", "supplies"
        );
    }

    protected boolean isGuestByReservationQuestion(String value) {
        return isGuestToolQuestion(value) && containsAny(value, "reservacion", "reservaciones", "reserva", "reservas", "asociado", "asociados");
    }

    protected boolean isRecentGuestQuestion(String value) {
        return isGuestToolQuestion(value) && containsAny(value, "recientes", "reciente", "ultimos", "ultimas");
    }

    protected boolean isReturningGuestQuestion(String value) {
        return isGuestToolQuestion(value) && containsAny(value, "recurrente", "recurrentes", "regreso", "volvio", "ya se habia", "returning");
    }

    protected boolean isUpcomingGuestQuestion(String value) {
        return isGuestToolQuestion(value) && containsAny(value, "llegan", "llega", "proximos", "proximas", "esta semana", "upcoming");
    }

    protected boolean isGuestCountQuestion(String value) {
        return isGuestToolQuestion(value) && containsAny(value, "cuantos", "cantidad", "conteo", "count", "tendre", "tengo este mes");
    }


    protected boolean isReservationSupplyToolQuestion(String value) {
        return containsAny(value, "supply", "supplies", "insumo", "insumos", "suministro", "suministros")
                && containsAny(value, "reservacion", "reservaciones", "reserva", "reservas", "check in", "check-in", "huesped", "huespedes", "proxima", "proximas", "ultima", "ultimos", "ultimas", "usaron", "usado", "usan", "usa", "uso", "mas", "más", "asignado", "asignados", "faltan", "faltantes");
    }

    protected boolean isReservationSupplyByReservationQuestion(String value) {
        return isReservationSupplyToolQuestion(value) && containsAny(value, "reservacion", "reservaciones", "reserva", "reservas", "codigo", "check in", "check-in");
    }

    protected boolean isReservationSupplyByPropertyQuestion(String value) {
        return isReservationSupplyToolQuestion(value) && containsAny(value, "propiedad", "propiedades", "casa", "bungalow", "alojamiento");
    }

    protected boolean isReservationSupplyUpcomingQuestion(String value) {
        return isReservationSupplyToolQuestion(value) && containsAny(value, "proxima", "proximas", "proximo", "proximos", "siguiente", "siguientes", "upcoming", "check in", "check-in", "necesito", "necesarios", "necesarias", "preparar");
    }

    protected boolean isReservationSupplySummaryQuestion(String value) {
        return isReservationSupplyToolQuestion(value) && containsAny(value, "resumen", "summary", "total", "totales", "por item", "por producto");
    }

    protected boolean isReservationSupplySummaryByDateQuestion(String value) {
        return isReservationSupplyToolQuestion(value) && containsAny(value, "fecha", "fechas", "rango", "semana", "mes", "hoy");
    }

    protected boolean isReservationSupplyLastUsedQuestion(String value) {
        return isReservationSupplyToolQuestion(value) && containsAny(value, "ultimo", "ultima", "ultima vez", "last used", "se usaron", "se uso");
    }

    protected boolean isReservationSupplyLatestReservationQuestion(String value) {
        return isReservationSupplyToolQuestion(value)
                && containsAny(value, "ultima reserva", "ultima reservacion", "ultima reservación", "ultima vez en reserva", "se usaron en la ultima", "se usaron en ultima");
    }

    protected boolean isReservationSupplyMostUsedQuestion(String value) {
        return isReservationSupplyToolQuestion(value) && containsAny(value, "mas usados", "más usados", "mas usado", "más usado", "usan mas", "usan más", "usa mas", "usa más", "se usan mas", "se usan más", "most used", "frecuentes");
    }

    protected boolean isReservationSupplyMissingQuestion(String value) {
        return isReservationSupplyToolQuestion(value) && containsAny(value, "no tienen", "no tiene", "sin supplies", "sin supply", "sin insumos", "sin asignar", "no asignados", "no asignadas", "no tienen supplies", "faltan", "faltantes", "missing");
    }

    protected boolean isTaskListToolQuestion(String value) {
        return containsAny(value, "tarea", "tareas", "task list", "task lists", "lista de tareas", "listas de tareas", "checklist")
                && !containsAny(value, "mantenimiento programado", "supplies", "supply", "insumo", "insumos");
    }

    protected boolean isTaskListByPropertyQuestion(String value) {
        return isTaskListToolQuestion(value) && containsAny(value, "propiedad", "propiedades", "casa", "bungalow", "alojamiento");
    }

    protected boolean isTaskListByReservationQuestion(String value) {
        return isTaskListToolQuestion(value) && containsAny(value, "reservacion", "reservaciones", "reserva", "reservas", "check in", "check-in");
    }

    protected boolean isTaskListForNextReservationQuestion(String value) {
        return isTaskListToolQuestion(value)
                && containsAny(value, "proxima reservacion", "proxima reservación", "proxima reserva", "siguiente reservacion", "siguiente reserva");
    }

    protected boolean isTaskListActiveQuestion(String value) {
        return isTaskListToolQuestion(value) && containsAny(value, "activas", "activa", "abiertas", "abierta", "pendientes", "pendiente", "en progreso");
    }

    protected boolean isTaskListCompletedQuestion(String value) {
        return isTaskListToolQuestion(value) && containsAny(value, "completadas", "completada", "completados", "completado", "terminadas", "cerradas");
    }

    protected boolean isTaskListOverdueQuestion(String value) {
        return isTaskListToolQuestion(value) && containsAny(value, "vencidas", "vencida", "atrasadas", "atrasada", "overdue");
    }

    protected boolean isTaskListDueTodayQuestion(String value) {
        return isTaskListToolQuestion(value) && containsAny(value, "hoy", "today");
    }

    protected boolean isTaskListDueThisWeekQuestion(String value) {
        return isTaskListToolQuestion(value) && containsAny(value, "esta semana", "semana", "week");
    }

    protected boolean isTaskListProgressQuestion(String value) {
        return isTaskListToolQuestion(value) && containsAny(value, "avance", "progreso", "porcentaje", "progress");
    }

    protected boolean isTaskListCompletionQuestion(String value) {
        return isTaskListToolQuestion(value) && containsAny(value, "completitud", "completion", "estado", "distribucion");
    }

    protected boolean isTaskItemToolQuestion(String value) {
        return isTaskListToolQuestion(value) && containsAny(value, "especifica", "especificas", "item", "items", "faltan", "faltantes", "responsable", "responsables", "prioridad", "priority", "completada", "completadas", "completado", "completados", "completaron", "ya se completaron", "pendiente", "pendientes", "atrasada", "atrasadas", "vencida", "vencidas");
    }

    protected boolean isTaskItemByTaskListQuestion(String value) {
        return isTaskItemToolQuestion(value) && containsAny(value, "lista", "checklist", "task list");
    }

    protected boolean isTaskItemPendingQuestion(String value) {
        return isTaskItemToolQuestion(value) && containsAny(value, "pendientes", "pendiente", "faltan", "faltantes", "no completadas");
    }

    protected boolean isTaskItemCompletedQuestion(String value) {
        return isTaskItemToolQuestion(value) && containsAny(value, "completadas", "completada", "completados", "completado", "ya se completaron");
    }

    protected boolean isTaskItemOverdueQuestion(String value) {
        return isTaskItemToolQuestion(value) && containsAny(value, "atrasadas", "atrasada", "vencidas", "vencida", "overdue");
    }

    protected boolean isTaskItemAssignedSummaryQuestion(String value) {
        return isTaskItemToolQuestion(value) && containsAny(value, "responsable", "responsables", "asignado", "asignadas", "assigned");
    }

    protected boolean isTaskItemPrioritySummaryQuestion(String value) {
        return isTaskItemToolQuestion(value) && containsAny(value, "prioridad", "prioridades", "priority");
    }


    protected boolean isPurchaseAnalyticsQuestion(String value) {
        return containsAny(value,
                "compra", "compras", "compre", "comprado", "comprados", "producto", "productos", "proveedor", "proveedores", "precio", "precios", "costo", "costos", "cuesta", "normalmente", "gasto", "gastos", "gaste", "gastado", "supplies", "supply", "suministro", "suministros", "papel higienico"
        )
                || containsAny(value, "cuanto cuesta", "cuánto cuesta", "cuesta normalmente", "costo promedio", "precio promedio")
                || (containsAny(value, "item", "items") && containsAny(value, "compro", "compras", "compre", "comprado", "precio", "costo", "gasto"));
    }

    protected boolean isPurchaseListPendingQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "lista", "listas", "compra", "compras")
                && containsAny(value, "pendiente", "pendientes", "abierta", "abiertas", "open", "partially");
    }

    protected boolean isPurchaseListCompletedQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "lista", "listas", "compra", "compras")
                && containsAny(value, "completada", "completadas", "completado", "completados", "finalizada", "finalizadas");
    }

    protected boolean isPurchaseListRecentQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value) && containsAny(value, "reciente", "recientes", "ultimas compras", "ultimas listas");
    }

    protected boolean isPurchaseListByPropertyQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "propiedad", "propiedades", "casa", "bungalow", "alojamiento")
                && !containsAny(value, "gasto", "gastos", "costo", "costos", "genero mas", "mas compras");
    }

    protected boolean isPurchaseCostSummaryQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "cuanto", "gaste", "gasté", "gasto", "gastos", "costo", "costos", "monto", "total")
                && !isPurchaseItemAverageUnitCostQuestion(value)
                && !isPurchaseItemPriceHistoryQuestion(value);
    }

    protected boolean isPurchaseCostByPropertyQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "propiedad", "propiedades", "genero mas", "generó mas", "mas compras", "más compras")
                && containsAny(value, "gasto", "gastos", "costo", "costos", "compra", "compras", "genero", "generó");
    }

    protected boolean isPurchaseCostByCategoryQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "categoria", "categorias", "tipo", "tipos")
                && containsAny(value, "gasto", "gastos", "costo", "costos", "compra", "compras", "supplies", "suministros");
    }

    protected boolean isPurchaseCostByMonthQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "por mes", "mensual", "mes a mes", "meses", "tendencia mensual");
    }

    protected boolean isPurchaseItemListQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "item", "items", "producto", "productos")
                && !isPurchaseItemMostPurchasedQuestion(value)
                && !isPurchaseItemLeastPurchasedQuestion(value)
                && !isPurchaseItemAverageUnitCostQuestion(value)
                && !isPurchaseItemPriceHistoryQuestion(value)
                && !isPurchaseItemCostTrendQuestion(value);
    }

    protected boolean isPurchaseItemPriceHistoryQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "historial de precio", "historial de precios", "precios", "precio")
                && containsAny(value, "historial", "ultimos", "ultimas", "evolucion", "evolución");
    }

    protected boolean isPurchaseItemAverageUnitCostQuestion(String value) {
        return containsAny(value, "cuanto cuesta", "cuánto cuesta", "cuesta normalmente", "normalmente", "promedio", "costo promedio", "precio promedio", "unitario");
    }

    protected boolean isPurchaseItemQuantitySummaryQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "cantidad", "cantidades", "cuanto compre", "cuánto compré", "cuantos compre", "cuántos compré");
    }

    protected boolean isPurchaseItemMostPurchasedQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "compro mas", "compro más", "mas seguido", "más seguido", "mas comprado", "más comprado", "compro con mas frecuencia", "top compras", "item compro mas");
    }

    protected boolean isPurchaseItemLeastPurchasedQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "menos comprado", "menos compro", "menor frecuencia", "compro menos", "menos seguido");
    }

    protected boolean isPurchaseItemCostTrendQuestion(String value) {
        return isPurchaseAnalyticsQuestion(value)
                && containsAny(value, "ha subido", "subio", "subió", "bajado", "bajo", "bajó", "tendencia", "variacion", "variación", "cambio de precio");
    }

    protected boolean isOperationalSummaryQuestion(String value) {
        return containsAny(value,
                "resumen operativo", "dashboard", "panel operativo", "estado operativo", "resumen del sistema", "metricas operativas"
        );
    }

    protected boolean isUpcomingReservationQuestion(String value) {
        return containsAny(value,
                "reservaciones proximas", "reservas proximas", "reservaciones activas", "reservas activas", "proximas reservaciones", "proximas reservas",
                "check in proximos", "check-in proximos", "entradas proximas"
        ) || (containsAny(value, "reservacion", "reservaciones", "reserva", "reservas")
                && containsAny(value, "proxima", "proximas", "siguiente", "siguientes", "activas", "check in", "check-in"));
    }

    protected boolean isLastMaintenanceQuestion(String value) {
        return containsAny(value,
                "ultimo mantenimiento", "ultima reparacion", "mantenimiento mas reciente", "ultimo trabajo realizado", "mantenimiento realizado", "mantenimiento completado"
        ) || (containsAny(value, "mantenimiento", "mantenimientos", "reparacion", "reparaciones")
                && containsAny(value, "ultimo", "ultima", "reciente", "realizado", "completado"));
    }

    protected boolean isOverdueScheduledMaintenanceQuestion(String value) {
        return containsAny(value,
                "mantenimientos programados vencidos", "mantenimiento programado vencido", "programados vencidos", "vencidos", "atrasados", "caducados"
        ) && containsAny(value, "mantenimiento", "mantenimientos", "programado", "programados");
    }

    protected boolean isLastPurchaseQuestion(String value) {
        return containsAny(value,
                "ultima compra", "ultimo item comprado", "ultimo producto comprado", "compre por ultima vez", "compraste por ultima vez", "cuando compre", "cuando se compro", "comprado por ultima vez"
        ) || (containsAny(value, "compra", "compras", "compre", "comprado", "compraste")
                && containsAny(value, "ultima", "ultimo", "vez", "cuando", "reciente"));
    }

    protected boolean isPendingTaskQuestion(String value) {
        return containsAny(value,
                "tareas pendientes", "listas de tareas pendientes", "task lists pendientes", "pendientes tengo", "tareas abiertas", "listas abiertas", "tareas en progreso", "cosas pendientes", "algo pendiente"
        ) || (containsAny(value, "tarea", "tareas", "task", "tasks", "pendiente", "pendientes")
                && containsAny(value, "pendiente", "pendientes", "abierta", "abiertas", "progreso", "hacer"));
    }


    protected boolean isDocumentToolQuestion(String value) {
        return containsAny(value,
                "documento", "documentos", "archivo", "archivos", "pdf", "manual", "manuales", "plano", "planos", "regla", "reglas", "house rules", "document metadata"
        );
    }

    protected boolean isDocumentByPropertyQuestion(String value) {
        return isDocumentToolQuestion(value) && containsAny(value, "propiedad", "alojamiento", "casa", "bungalow", "para esta propiedad", "de esta propiedad");
    }

    protected boolean isDocumentByTypeQuestion(String value) {
        return isDocumentToolQuestion(value) && containsAny(value, "tipo", "plano", "planos", "manual", "manuales", "regla", "reglas", "blueprint");
    }

    protected boolean isDocumentByStatusQuestion(String value) {
        return isDocumentToolQuestion(value) && containsAny(value, "estado", "status", "procesado", "procesados", "procesar", "fallaron", "fallo", "indexado", "indexados");
    }

    protected boolean isDocumentRecentQuestion(String value) {
        return isDocumentToolQuestion(value) && containsAny(value, "reciente", "recientes", "ultimos", "ultimas", "subidos recientemente", "cargados recientemente");
    }

    protected boolean isDocumentUnprocessedQuestion(String value) {
        return isDocumentToolQuestion(value) && containsAny(value, "no han sido procesados", "no procesados", "sin procesar", "pendientes de procesar", "pendiente de procesar", "unprocessed");
    }

    protected boolean isDocumentFailedQuestion(String value) {
        return isDocumentToolQuestion(value) && containsAny(value, "fallaron", "fallo", "failed", "error al procesar", "procesamiento fallido");
    }

    protected boolean isDocumentProcessedQuestion(String value) {
        return isDocumentToolQuestion(value) && containsAny(value, "procesados", "procesado", "ya procesados", "processed") && !isDocumentUnprocessedQuestion(value);
    }

    protected boolean isDocumentIndexedQuestion(String value) {
        return isDocumentToolQuestion(value)
                && containsAny(value, "indexados", "indexado", "listos para ia", "listos para ai", "listos para inteligencia artificial")
                && !isDocumentNotIndexedQuestion(value);
    }

    protected boolean isDocumentProcessedNotIndexedQuestion(String value) {
        return isDocumentToolQuestion(value)
                && containsAny(value, "procesados", "procesado", "processed")
                && containsAny(value,
                "no indexados", "sin indexar", "no estan indexados", "no están indexados", "no esten indexados", "no estén indexados", "not indexed"
        );
    }

    protected boolean isDocumentNotIndexedQuestion(String value) {
        return isDocumentToolQuestion(value) && containsAny(value,
                "no indexados", "sin indexar", "no estan indexados", "no están indexados", "no esten indexados", "no estén indexados", "not indexed"
        );
    }

    protected boolean isDocumentCountByTypeQuestion(String value) {
        return isDocumentToolQuestion(value)
                && containsAny(value, "por tipo", "agrupados por tipo", "agrupados tipo", "tipo", "by type");
    }

    protected boolean isDocumentCountByPropertyQuestion(String value) {
        return isDocumentToolQuestion(value)
                && containsAny(value, "por propiedad", "agrupados por propiedad", "agrupados propiedad", "by property");
    }

    protected boolean isDocumentContentQuestion(String value) {
        boolean metadataQuestion = containsAny(value,
                "documentos tengo", "documentos cargados", "documentos por tipo", "documentos por propiedad",
                "que documentos", "qué documentos", "documentos estan", "documentos están", "documentos fallaron",
                "documentos listos", "documentos procesados", "indice rag", "índice rag", "chunks"
        );
        if (metadataQuestion) {
            return false;
        }
        return containsAny(value,
                "que dice", "qué dice", "que menciona", "qué menciona", "menciona", "habla de", "contenido",
                "que reglas hay", "qué reglas hay", "reglas hay", "reglas aplican", "que reglas aplican", "qué reglas aplican",
                "aplican a", "aplica a", "sobre basura", "sobre mascotas", "segun mis documentos", "según mis documentos"
        );
    }

    protected boolean isDocumentBlueprintQuestion(String value) {
        return containsAny(value, "plano", "planos", "blueprint", "electrico", "eléctrico", "plomeria", "plomería", "drenaje") && isDocumentToolQuestion(value);
    }

    protected boolean isDocumentHouseRulesQuestion(String value) {
        return containsAny(value, "reglas de casa", "house rules", "reglas", "senales", "señales") && isDocumentToolQuestion(value);
    }

    protected boolean isDocumentManualQuestion(String value) {
        return containsAny(value, "manual", "manuales") && isDocumentToolQuestion(value);
    }

    protected boolean isRagChunkSummaryQuestion(String value) {
        return containsAny(value, "resumen de chunks", "chunk summary", "chunks por documento", "cuantos chunks", "cuántos chunks");
    }

    protected boolean isRagMissingChunksQuestion(String value) {
        return containsAny(value, "sin chunks", "no tienen chunks", "missing chunks", "documentos sin chunks");
    }

    protected boolean isRagMissingVectorIdsQuestion(String value) {
        return containsAny(value, "sin vector_store_id", "sin vector store id", "missing vector", "chunks pero no vector", "pendientes de vector");
    }

    protected boolean isRagCoverageSummaryQuestion(String value) {
        return containsAny(value, "cobertura del indice", "coverage", "index coverage", "cobertura rag", "coverage summary");
    }

    protected boolean isDocumentMetadataQuestion(String value) {
        return containsAny(value,
                "documentos cargados", "documentos subidos", "documentos tengo", "documentos registrados", "documentos procesados", "que documentos", "mis documentos", "document metadata", "archivos cargados", "archivos subidos"
        );
    }

    protected boolean isRagHealthQuestion(String value) {
        return containsAny(value,
                "indice rag", "índice rag", "index rag", "rag de mis documentos", "estado rag", "estado del rag", "salud del rag", "estado del indice", "estado del índice", "indexacion de documentos", "estado de indexacion", "chunks indexados", "vector store", "vector_store", "chroma"
        );
    }

    protected boolean isPreparationQuestion(String value) {
        return containsAny(value,
                "preparar la casa", "preparar propiedad", "preparar alojamiento", "antes de la proxima reserva", "antes de la proxima reservacion", "antes del proximo check in", "antes del check in", "para la proxima reserva", "para la proxima reservacion"
        ) || (containsAny(value, "preparar", "pendiente", "pendientes", "hacer", "falta")
                && containsAny(value, "reserva", "reservacion", "check in", "huesped", "alojamiento", "casa", "propiedad"));
    }

    protected boolean isOperationalPlanningQuestion(String value) {
        return containsAny(value,
                "que debo atender", "que debo revisar", "que tengo pendiente hoy", "prioridades operativas", "plan operativo", "que necesita atencion", "como va la operacion", "estado general de la operacion"
        );
    }

    protected boolean isDocumentInventoryQuestion(String value) {
        return isDocumentMetadataQuestion(value) && isRagHealthQuestion(value);
    }

    protected boolean isPropertyOperationsQuestion(String value) {
        return isPropertyQuestion(value) && containsAny(value, "operacion", "operativo", "reservacion", "reserva", "mantenimiento", "tarea", "pendiente", "estado");
    }

    protected boolean isUnsupportedWriteAction(String value) {
        return startsWithAny(value,
                "crea ", "crear ", "agrega ", "agregar ", "anade ", "anadir ", "registra ", "registrar ",
                "actualiza ", "actualizar ", "edita ", "editar ", "modifica ", "modificar ", "elimina ",
                "eliminar ", "borra ", "borrar ", "cancela ", "cancelar ", "envia ", "enviar ", "manda ", "mandar ", "programa ", "programar "
        );
    }

    protected boolean containsAny(String value, String... candidates) {
        return AiToolTextNormalizer.containsAnyForRouting(value, candidates);
    }

    protected boolean startsWithAny(String value, String... prefixes) {
        return AiToolTextNormalizer.startsWithAnyForRouting(value, prefixes);
    }

    protected String normalize(String value) {
        return AiToolTextNormalizer.normalizeForRouting(value);
    }

    protected String collapseWhitespace(String value) {
        return AiToolTextNormalizer.collapseWhitespace(value);
    }
}
