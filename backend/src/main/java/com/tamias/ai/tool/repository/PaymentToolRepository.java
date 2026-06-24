package com.tamias.ai.tool.repository;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.support.AiReadOnlyToolSupport;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class PaymentToolRepository extends AiReadOnlyToolSupport {

    public PaymentToolRepository(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer paymentSummary(String userQuestion) {
        PaymentDateRange range = paymentDateRange(userQuestion);
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(p.id) AS payment_count,
                       COALESCE(SUM(p.amount), 0) AS total_amount,
                       COALESCE(AVG(p.amount), 0) AS average_amount,
                       MIN(p.pay_date) AS first_pay_date,
                       MAX(p.pay_date) AS last_pay_date,
                       COUNT(DISTINCT p.category_id) AS category_count,
                       COUNT(DISTINCT p.property_id) AS property_count
                FROM payments p
                WHERE p.organization_id = :organizationId
                  AND p.deleted_at IS NULL
                  AND p.status = 'ACTIVE'
                """);
        if (range != null) {
            sql.append(" AND p.pay_date >= :fromDate\n");
            sql.append(" AND p.pay_date <= :toDate\n");
        }

        List<Map<String, Object>> rows = query(sql.toString(), q -> {
            q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
            if (range != null) {
                q.setParameter("fromDate", range.fromDate());
                q.setParameter("toDate", range.toDate());
            }
        }, "paymentCount", "totalAmount", "averageAmount", "firstPayDate", "lastPayDate", "categoryCount", "propertyCount");

        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);
        String label = range == null ? "registrados" : "para " + range.label();
        String answer = "Resumen de pagos " + label + ":\n"
                + "- Pagos: " + blankToDash(value(row.get("paymentCount"))) + "\n"
                + "- Total pagado: " + formatMoney(row.get("totalAmount")) + "\n"
                + "- Promedio por pago: " + formatMoney(row.get("averageAmount")) + "\n"
                + "- Primer pago: " + blankToDash(value(row.get("firstPayDate"))) + "\n"
                + "- Último pago: " + blankToDash(value(row.get("lastPayDate"))) + "\n"
                + "- Categorías usadas: " + blankToDash(value(row.get("categoryCount"))) + "\n"
                + "- Propiedades asociadas: " + blankToDash(value(row.get("propertyCount")));

        return AiToolAnswer.of(answer, "payment.summary", "Payment summary", "Payment summary was calculated.", rows);
    }

    public AiToolAnswer paymentSearch(String userQuestion) {
        PaymentDateRange range = paymentDateRange(userQuestion);
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "pago", "pagos", "pague", "pagué", "pagado", "pagados", "gasto", "gastos",
                "busca", "buscar", "muestra", "listame", "lístame", "recibo", "recibos", "comprobante", "comprobantes",
                "mes", "semana", "ano", "año", "actual", "este", "esta", "pasado", "pasada"
        ));
        List<Map<String, Object>> rows = paymentRows(search, null, null, null, range, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré pagos registrados que coincidan con tu pregunta."
                            : "No encontré pagos relacionados con “" + search + "”.",
                    "payment.search",
                    "Payment search",
                    "No payment rows found.",
                    List.of()
            );
        }

        StringBuilder answer = new StringBuilder(search == null
                ? "Estos son los pagos que encontré:"
                : "Estos pagos coinciden con “" + search + "”:");
        appendPaymentRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), "payment.search", "Payment search", "%d payment rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer recentPayments() {
        List<Map<String, Object>> rows = paymentRows(null, null, null, null, null, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré pagos recientes.", "payment.recent", "Recent payments", "No recent payments found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Estos son tus pagos más recientes:");
        appendPaymentRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), "payment.recent", "Recent payments", "%d recent payment rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer paymentsByCategory(String userQuestion) {
        PaymentDateRange range = paymentDateRange(userQuestion);
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "pago", "pagos", "pague", "pagué", "pagado", "pagados", "gasto", "gastos",
                "categoria", "categoría", "categorias", "categorías", "por", "de", "del",
                "cuanto", "cuánto", "total", "mes", "semana", "ano", "año", "actual", "este", "esta", "pasado", "pasada"
        ));
        List<Map<String, Object>> rows = paymentAggregateRows("category", search, null, range, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré pagos agrupados por categoría."
                            : "No encontré pagos relacionados con la categoría “" + search + "”.",
                    "payment.byCategory",
                    "Payments by category",
                    "No payment category rows found.",
                    List.of()
            );
        }
        StringBuilder answer = new StringBuilder(search == null
                ? "Pagos por categoría:"
                : "Pagos relacionados con la categoría “" + search + "”:");
        appendPaymentAggregateRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), "payment.byCategory", "Payments by category", "%d payment category rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer paymentsByMethod(String userQuestion) {
        PaymentDateRange range = paymentDateRange(userQuestion);
        String method = resolvePaymentMethod(userQuestion);
        List<Map<String, Object>> rows = paymentAggregateRows("method", null, method, range, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    method == null
                            ? "No encontré pagos agrupados por método."
                            : "No encontré pagos con método " + method + ".",
                    "payment.byMethod",
                    "Payments by method",
                    "No payment method rows found.",
                    List.of()
            );
        }
        StringBuilder answer = new StringBuilder(method == null ? "Pagos por método:" : "Pagos con método " + method + ":");
        appendPaymentAggregateRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), "payment.byMethod", "Payments by method", "%d payment method rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer paymentsByProperty(String userQuestion) {
        PaymentDateRange range = paymentDateRange(userQuestion);
        String search = nullableSearch(extractSearchText(
                userQuestion,
                "pago", "pagos", "pague", "pagué", "pagado", "pagados", "gasto", "gastos",
                "propiedad", "propiedades", "casa", "bungalow", "alojamiento", "por", "de", "del",
                "cuanto", "cuánto", "total", "mes", "semana", "ano", "año", "actual", "este", "esta", "pasado", "pasada"
        ));
        List<Map<String, Object>> rows = paymentAggregateRows("property", search, null, range, DEFAULT_LIMIT);
        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    search == null
                            ? "No encontré pagos agrupados por propiedad."
                            : "No encontré pagos relacionados con la propiedad “" + search + "”.",
                    "payment.byProperty",
                    "Payments by property",
                    "No payment property rows found.",
                    List.of()
            );
        }
        StringBuilder answer = new StringBuilder(search == null
                ? "Pagos por propiedad:"
                : "Pagos relacionados con la propiedad “" + search + "”:");
        appendPaymentAggregateRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), "payment.byProperty", "Payments by property", "%d payment property rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer paymentMonthlyTotals() {
        List<Map<String, Object>> rows = query("""
                SELECT TO_CHAR(DATE_TRUNC('month', p.pay_date), 'YYYY-MM') AS payment_month,
                       COUNT(p.id) AS payment_count,
                       COALESCE(SUM(p.amount), 0) AS total_amount,
                       COALESCE(AVG(p.amount), 0) AS average_amount
                FROM payments p
                WHERE p.organization_id = :organizationId
                  AND p.deleted_at IS NULL
                  AND p.status = 'ACTIVE'
                GROUP BY DATE_TRUNC('month', p.pay_date)
                ORDER BY DATE_TRUNC('month', p.pay_date) DESC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
                    q.setParameter("limit", 12);
                }, "paymentMonth", "paymentCount", "totalAmount", "averageAmount");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré pagos para calcular totales mensuales.", "payment.monthlyTotals", "Payment monthly totals", "No monthly payment totals found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Totales de pagos por mes:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("paymentMonth"))))
                    .append(" | total: ").append(formatMoney(row.get("totalAmount")))
                    .append(" | pagos: ").append(blankToDash(value(row.get("paymentCount"))))
                    .append(" | promedio: ").append(formatMoney(row.get("averageAmount")));
        }
        return AiToolAnswer.of(answer.toString(), "payment.monthlyTotals", "Payment monthly totals", "%d monthly payment totals found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer highestPayments() {
        List<Map<String, Object>> rows = query(basePaymentRowsSql("", "p.amount DESC, p.pay_date DESC", DEFAULT_LIMIT), q -> {
            q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
            q.setParameter("limit", DEFAULT_LIMIT);
        }, paymentRowColumns());
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré pagos para identificar los montos más altos.", "payment.highestPayments", "Highest payments", "No highest payment rows found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Estos son los pagos con montos más altos:");
        appendPaymentRows(answer, rows);
        return AiToolAnswer.of(answer.toString(), "payment.highestPayments", "Highest payments", "%d highest payment rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer paymentImagesSummary() {
        List<Map<String, Object>> rows = query("""
                SELECT p.id,
                       p.name,
                       p.pay_date,
                       p.amount,
                       pc.name AS category_name,
                       prop.name AS property_name,
                       COUNT(pi.id) AS image_count,
                       COALESCE(SUM(pi.size_bytes), 0) AS total_size_bytes
                FROM payments p
                JOIN payment_categories pc ON pc.id = p.category_id
                LEFT JOIN properties prop ON prop.id = p.property_id AND prop.organization_id = p.organization_id
                LEFT JOIN payment_images pi ON pi.payment_id = p.id
                    AND pi.organization_id = p.organization_id
                    AND pi.status = 'ACTIVE'
                WHERE p.organization_id = :organizationId
                  AND p.deleted_at IS NULL
                  AND p.status = 'ACTIVE'
                GROUP BY p.id, p.name, p.pay_date, p.amount, pc.name, prop.name
                HAVING COUNT(pi.id) > 0
                ORDER BY image_count DESC, p.pay_date DESC, p.name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "name", "payDate", "amount", "categoryName", "propertyName", "imageCount", "totalSizeBytes");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré pagos con imágenes registradas.", "payment.imagesSummary", "Payment images summary", "No payment image rows found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Estos pagos tienen imágenes o comprobantes asociados:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("name"))))
                    .append(" | fecha: ").append(blankToDash(value(row.get("payDate"))))
                    .append(" | monto: ").append(formatMoney(row.get("amount")))
                    .append(" | categoría: ").append(blankToDash(value(row.get("categoryName"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | imágenes: ").append(blankToDash(value(row.get("imageCount"))))
                    .append(" | tamaño total: ").append(formatBytes(toLong(row.get("totalSizeBytes"))));
        }
        return AiToolAnswer.of(answer.toString(), "payment.imagesSummary", "Payment images summary", "%d payment image rows found.".formatted(rows.size()), rows);
    }

    public AiToolAnswer paymentsWithoutCategory() {
        List<Map<String, Object>> rows = query("""
                SELECT COUNT(p.id) AS payment_count
                FROM payments p
                LEFT JOIN payment_categories pc ON pc.id = p.category_id
                WHERE p.organization_id = :organizationId
                  AND p.deleted_at IS NULL
                  AND p.status = 'ACTIVE'
                  AND (p.category_id IS NULL OR pc.id IS NULL)
                """, q -> q.setParameter("organizationId", currentUserService.getCurrentOrganizationId()), "paymentCount");
        String count = rows.isEmpty() ? "0" : value(rows.get(0).get("paymentCount"));
        String answer = "Pagos sin categoría encontrados: " + blankToDash(count) + ".\n"
                + "Según el diseño actual, los pagos deben tener categoría obligatoria; si aparece un valor mayor a 0, habría que revisar integridad de datos.";
        return AiToolAnswer.of(answer, "payment.withoutCategory", "Payments without category", "Payments without category were counted.", rows);
    }

    public AiToolAnswer paymentCategories() {
        List<Map<String, Object>> rows = query("""
                SELECT pc.id,
                       pc.name,
                       pc.description,
                       pc.status,
                       COUNT(p.id) AS payment_count,
                       COALESCE(SUM(p.amount), 0) AS total_amount
                FROM payment_categories pc
                LEFT JOIN payments p ON p.category_id = pc.id
                    AND p.organization_id = pc.organization_id
                    AND p.deleted_at IS NULL
                    AND p.status = 'ACTIVE'
                WHERE pc.organization_id = :organizationId
                  AND pc.deleted_at IS NULL
                GROUP BY pc.id, pc.name, pc.description, pc.status
                ORDER BY pc.status ASC, pc.name ASC
                LIMIT :limit
                """, q -> {
                    q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
                    q.setParameter("limit", DEFAULT_LIMIT);
                }, "id", "name", "description", "status", "paymentCount", "totalAmount");
        if (rows.isEmpty()) {
            return AiToolAnswer.of("No encontré categorías de pago registradas.", "payment.categories", "Payment categories", "No payment categories found.", List.of());
        }
        StringBuilder answer = new StringBuilder("Estas son tus categorías de pago:");
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("name"))))
                    .append(" | estado: ").append(blankToDash(value(row.get("status"))))
                    .append(" | pagos: ").append(blankToDash(value(row.get("paymentCount"))))
                    .append(" | total: ").append(formatMoney(row.get("totalAmount")));
        }
        return AiToolAnswer.of(answer.toString(), "payment.categories", "Payment categories", "%d payment categories found.".formatted(rows.size()), rows);
    }

    private List<Map<String, Object>> paymentRows(String search, String method, String categorySearch, String propertySearch, PaymentDateRange range, int limit) {
        StringBuilder extraSql = new StringBuilder();
        if (search != null) {
            extraSql.append("""
                    AND NOT EXISTS (
                        SELECT 1
                        FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                        WHERE token.value <> ''
                          AND translate(LOWER(CONCAT_WS(' ', p.name, p.description, p.responsible, p.method, pc.name, prop.name)), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                    )
                    """);
        }
        if (method != null) {
            extraSql.append(" AND p.method = :method\n");
        }
        if (categorySearch != null) {
            extraSql.append("""
                    AND NOT EXISTS (
                        SELECT 1
                        FROM unnest(string_to_array(CAST(:categorySearch AS TEXT), ' ')) AS token(value)
                        WHERE token.value <> ''
                          AND translate(LOWER(pc.name), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                    )
                    """);
        }
        if (propertySearch != null) {
            extraSql.append("""
                    AND NOT EXISTS (
                        SELECT 1
                        FROM unnest(string_to_array(CAST(:propertySearch AS TEXT), ' ')) AS token(value)
                        WHERE token.value <> ''
                          AND translate(LOWER(COALESCE(prop.name, 'Sin propiedad')), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%', token.value, '%')
                    )
                    """);
        }
        if (range != null) {
            extraSql.append(" AND p.pay_date >= :fromDate\n");
            extraSql.append(" AND p.pay_date <= :toDate\n");
        }
        return query(basePaymentRowsSql(extraSql.toString(), "p.pay_date DESC, p.created_at DESC, p.name ASC", limit), q -> {
            q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
            if (search != null) q.setParameter("search", search);
            if (method != null) q.setParameter("method", method);
            if (categorySearch != null) q.setParameter("categorySearch", categorySearch);
            if (propertySearch != null) q.setParameter("propertySearch", propertySearch);
            if (range != null) {
                q.setParameter("fromDate", range.fromDate());
                q.setParameter("toDate", range.toDate());
            }
            q.setParameter("limit", limit);
        }, paymentRowColumns());
    }

    private String basePaymentRowsSql(String extraWhereSql, String orderBy, int limit) {
        return """
                SELECT p.id,
                       p.name,
                       p.description,
                       p.method,
                       p.amount,
                       p.responsible,
                       p.pay_date,
                       pc.name AS category_name,
                       COALESCE(prop.name, 'Sin propiedad') AS property_name,
                       COUNT(pi.id) AS image_count
                FROM payments p
                JOIN payment_categories pc ON pc.id = p.category_id
                LEFT JOIN properties prop ON prop.id = p.property_id AND prop.organization_id = p.organization_id
                LEFT JOIN payment_images pi ON pi.payment_id = p.id
                    AND pi.organization_id = p.organization_id
                    AND pi.status = 'ACTIVE'
                WHERE p.organization_id = :organizationId
                  AND p.deleted_at IS NULL
                  AND p.status = 'ACTIVE'
                """
                + extraWhereSql
                + """
                GROUP BY p.id, p.name, p.description, p.method, p.amount, p.responsible, p.pay_date, pc.name, prop.name, p.created_at
                ORDER BY %s
                LIMIT :limit
                """.formatted(orderBy);
    }

    private String[] paymentRowColumns() {
        return new String[]{"id", "name", "description", "method", "amount", "responsible", "payDate", "categoryName", "propertyName", "imageCount"};
    }

    private List<Map<String, Object>> paymentAggregateRows(String groupType, String search, String method, PaymentDateRange range, int limit) {
        String selectLabel = switch (groupType) {
            case "category" -> "pc.name";
            case "property" -> "COALESCE(prop.name, 'Sin propiedad')";
            case "method" -> "p.method";
            default -> "pc.name";
        };
        StringBuilder extraSql = new StringBuilder();
        if (search != null) {
            String source = "property".equals(groupType) ? "COALESCE(prop.name, 'Sin propiedad')" : "pc.name";
            extraSql.append("""
                    AND NOT EXISTS (
                        SELECT 1
                        FROM unnest(string_to_array(CAST(:search AS TEXT), ' ')) AS token(value)
                        WHERE token.value <> ''
                          AND translate(LOWER(%s), 'áéíóúüñ', 'aeiouun') NOT LIKE CONCAT('%%', token.value, '%%')
                    )
                    """.formatted(source));
        }
        if (method != null) {
            extraSql.append(" AND p.method = :method\n");
        }
        if (range != null) {
            extraSql.append(" AND p.pay_date >= :fromDate\n");
            extraSql.append(" AND p.pay_date <= :toDate\n");
        }

        String sql = """
                SELECT %s AS group_name,
                       COUNT(p.id) AS payment_count,
                       COALESCE(SUM(p.amount), 0) AS total_amount,
                       COALESCE(AVG(p.amount), 0) AS average_amount,
                       MIN(p.pay_date) AS first_pay_date,
                       MAX(p.pay_date) AS last_pay_date,
                       COUNT(pi.id) AS image_count
                FROM payments p
                JOIN payment_categories pc ON pc.id = p.category_id
                LEFT JOIN properties prop ON prop.id = p.property_id AND prop.organization_id = p.organization_id
                LEFT JOIN payment_images pi ON pi.payment_id = p.id
                    AND pi.organization_id = p.organization_id
                    AND pi.status = 'ACTIVE'
                WHERE p.organization_id = :organizationId
                  AND p.deleted_at IS NULL
                  AND p.status = 'ACTIVE'
                %s
                GROUP BY %s
                ORDER BY total_amount DESC, payment_count DESC, group_name ASC
                LIMIT :limit
                """.formatted(selectLabel, extraSql, selectLabel);

        return query(sql, q -> {
            q.setParameter("organizationId", currentUserService.getCurrentOrganizationId());
            if (search != null) q.setParameter("search", search);
            if (method != null) q.setParameter("method", method);
            if (range != null) {
                q.setParameter("fromDate", range.fromDate());
                q.setParameter("toDate", range.toDate());
            }
            q.setParameter("limit", limit);
        }, "groupName", "paymentCount", "totalAmount", "averageAmount", "firstPayDate", "lastPayDate", "imageCount");
    }

    private void appendPaymentRows(StringBuilder answer, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("payDate"))))
                    .append(" | ").append(blankToDash(value(row.get("name"))))
                    .append(" | monto: ").append(formatMoney(row.get("amount")))
                    .append(" | método: ").append(blankToDash(value(row.get("method"))))
                    .append(" | categoría: ").append(blankToDash(value(row.get("categoryName"))))
                    .append(" | propiedad: ").append(blankToDash(value(row.get("propertyName"))))
                    .append(" | responsable: ").append(blankToDash(value(row.get("responsible"))))
                    .append(" | imágenes: ").append(blankToDash(value(row.get("imageCount"))));
        }
    }

    private void appendPaymentAggregateRows(StringBuilder answer, List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            answer.append(System.lineSeparator())
                    .append("- ").append(blankToDash(value(row.get("groupName"))))
                    .append(" | total: ").append(formatMoney(row.get("totalAmount")))
                    .append(" | pagos: ").append(blankToDash(value(row.get("paymentCount"))))
                    .append(" | promedio: ").append(formatMoney(row.get("averageAmount")))
                    .append(" | primer pago: ").append(blankToDash(value(row.get("firstPayDate"))))
                    .append(" | último pago: ").append(blankToDash(value(row.get("lastPayDate"))))
                    .append(" | imágenes: ").append(blankToDash(value(row.get("imageCount"))));
        }
    }

    private String resolvePaymentMethod(String userQuestion) {
        String normalized = normalize(userQuestion);
        if (containsAny(normalized, "transferencia", "transfer", "bank transfer", "banco", "bancaria", "bancario")) {
            return "BANK_TRANSFER";
        }
        if (containsAny(normalized, "efectivo", "cash")) {
            return "CASH";
        }
        if (containsAny(normalized, "debito", "débito", "debit")) {
            return "DEBIT";
        }
        if (containsAny(normalized, "credito", "crédito", "credit")) {
            return "CREDIT";
        }
        return null;
    }

    private PaymentDateRange paymentDateRange(String userQuestion) {
        String normalized = normalize(userQuestion);
        LocalDate today = LocalDate.now();
        if (containsAny(normalized, "hoy", "today")) {
            return new PaymentDateRange(today, today, "hoy");
        }
        if (containsAny(normalized, "ayer", "yesterday")) {
            LocalDate yesterday = today.minusDays(1);
            return new PaymentDateRange(yesterday, yesterday, "ayer");
        }
        if (containsAny(normalized, "mes pasado", "last month")) {
            LocalDate first = today.minusMonths(1).withDayOfMonth(1);
            LocalDate last = first.withDayOfMonth(first.lengthOfMonth());
            return new PaymentDateRange(first, last, "el mes pasado");
        }
        if (containsAny(normalized, "este mes", "mes actual", "this month")) {
            LocalDate first = today.withDayOfMonth(1);
            return new PaymentDateRange(first, today, "este mes");
        }
        if (containsAny(normalized, "semana pasada", "last week")) {
            LocalDate first = today.minusWeeks(1).with(DayOfWeek.MONDAY);
            LocalDate last = first.plusDays(6);
            return new PaymentDateRange(first, last, "la semana pasada");
        }
        if (containsAny(normalized, "esta semana", "semana actual", "this week")) {
            LocalDate first = today.with(DayOfWeek.MONDAY);
            return new PaymentDateRange(first, today, "esta semana");
        }
        if (containsAny(normalized, "ano pasado", "año pasado", "last year")) {
            LocalDate first = LocalDate.of(today.getYear() - 1, 1, 1);
            LocalDate last = LocalDate.of(today.getYear() - 1, 12, 31);
            return new PaymentDateRange(first, last, "el año pasado");
        }
        if (containsAny(normalized, "este ano", "este año", "ano actual", "año actual", "this year")) {
            LocalDate first = LocalDate.of(today.getYear(), 1, 1);
            return new PaymentDateRange(first, today, "este año");
        }
        return null;
    }


    private record PaymentDateRange(LocalDate fromDate, LocalDate toDate, String label) {
    }
}
