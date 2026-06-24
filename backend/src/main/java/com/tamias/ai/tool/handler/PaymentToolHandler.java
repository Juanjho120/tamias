package com.tamias.ai.tool.handler;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.context.AiToolRequestContext;
import com.tamias.ai.tool.service.PaymentReadOnlyToolService;
import com.tamias.ai.tool.support.AiToolRoutingSupport;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(75)
public class PaymentToolHandler extends AiToolRoutingSupport implements AiToolHandler {

    private final PaymentReadOnlyToolService paymentReadOnlyToolService;

    public PaymentToolHandler(PaymentReadOnlyToolService paymentReadOnlyToolService) {
        this.paymentReadOnlyToolService = paymentReadOnlyToolService;
    }

    @Override
    public Optional<AiToolAnswer> tryHandle(AiToolRequestContext context) {
        return tryHandlePaymentQuestion(context.question(), context.normalizedQuestion());
    }

    private Optional<AiToolAnswer> tryHandlePaymentQuestion(String question, String normalized) {
        if (!isPaymentQuestion(normalized)) {
            return Optional.empty();
        }
        if (isWriteRequest(normalized)) {
            return Optional.of(readOnlyGuard());
        }
        if (isPaymentWithoutCategoryQuestion(normalized)) {
            return Optional.of(paymentReadOnlyToolService.paymentsWithoutCategory());
        }
        if (isPaymentCategoryListQuestion(normalized)) {
            return Optional.of(paymentReadOnlyToolService.paymentCategories());
        }
        if (isPaymentImagesQuestion(normalized)) {
            return Optional.of(paymentReadOnlyToolService.paymentImagesSummary());
        }
        if (isHighestPaymentQuestion(normalized)) {
            return Optional.of(paymentReadOnlyToolService.highestPayments());
        }
        if (isMonthlyTotalsQuestion(normalized)) {
            return Optional.of(paymentReadOnlyToolService.paymentMonthlyTotals());
        }
        if (isPaymentMethodQuestion(normalized)) {
            return Optional.of(paymentReadOnlyToolService.paymentsByMethod(question));
        }
        if (isPaymentPropertyQuestion(normalized)) {
            return Optional.of(paymentReadOnlyToolService.paymentsByProperty(question));
        }
        if (isPaymentCategoryQuestion(normalized)) {
            return Optional.of(paymentReadOnlyToolService.paymentsByCategory(question));
        }
        if (isRecentPaymentQuestion(normalized)) {
            return Optional.of(paymentReadOnlyToolService.recentPayments());
        }
        if (isPaymentSummaryQuestion(normalized)) {
            return Optional.of(paymentReadOnlyToolService.paymentSummary(question));
        }
        return Optional.of(paymentReadOnlyToolService.paymentSearch(question));
    }

    private boolean isPaymentQuestion(String normalized) {
        boolean paymentTerm = containsAny(
                normalized,
                "pago", "pagos", "pague", "pagué", "pagado", "pagados", "pagada", "pagadas",
                "payment", "payments", "paid", "expense", "expenses",
                "gasto", "gastos", "recibo", "recibos", "comprobante", "comprobantes"
        );
        if (!paymentTerm) {
            return false;
        }

        boolean purchaseScope = containsAny(normalized, "compra", "compras", "purchase", "purchases", "lista de compra", "listas de compra");
        boolean explicitPaymentScope = containsAny(normalized, "pago", "pagos", "pague", "pagué", "pagado", "payment", "payments", "recibo", "comprobante");
        return !purchaseScope || explicitPaymentScope;
    }

    private boolean isWriteRequest(String normalized) {
        return containsAny(
                normalized,
                "crea", "crear", "agrega", "agregar", "añade", "anade",
                "elimina", "eliminar", "borra", "borrar", "actualiza", "actualizar",
                "edita", "editar", "modifica", "modificar", "sube", "subir", "carga", "cargar"
        );
    }

    private boolean isPaymentWithoutCategoryQuestion(String normalized) {
        return containsAny(normalized, "sin categoria", "sin categoría", "sin categorizar", "without category");
    }

    private boolean isPaymentCategoryListQuestion(String normalized) {
        return containsAny(normalized, "categorias de pago", "categorías de pago", "payment categories", "catalogo de pagos", "catálogo de pagos")
                && containsAny(normalized, "que", "cuales", "cuáles", "lista", "listar", "muestra", "ver", "tengo");
    }

    private boolean isPaymentImagesQuestion(String normalized) {
        return containsAny(normalized, "imagen", "imagenes", "imágenes", "foto", "fotos", "recibo", "recibos", "comprobante", "comprobantes")
                && containsAny(normalized, "pago", "pagos", "payment", "payments", "tienen", "con", "sin");
    }

    private boolean isHighestPaymentQuestion(String normalized) {
        return containsAny(normalized, "mas alto", "más alto", "mayor", "mayores", "monto alto", "monto mas", "monto más", "highest", "largest", "top")
                && containsAny(normalized, "pago", "pagos", "gasto", "gastos", "payment", "payments");
    }

    private boolean isMonthlyTotalsQuestion(String normalized) {
        return containsAny(normalized, "por mes", "mensual", "mensuales", "meses", "monthly", "month totals")
                && containsAny(normalized, "total", "totales", "pago", "pagos", "gasto", "gastos", "payment", "payments");
    }

    private boolean isPaymentMethodQuestion(String normalized) {
        return containsAny(normalized, "efectivo", "cash", "credito", "crédito", "credit", "debito", "débito", "debit", "transferencia", "transfer", "banco", "bancaria", "bancario", "metodo", "método", "method");
    }

    private boolean isPaymentPropertyQuestion(String normalized) {
        return containsAny(normalized, "propiedad", "propiedades", "casa", "bungalow", "alojamiento", "property", "properties");
    }

    private boolean isPaymentCategoryQuestion(String normalized) {
        return containsAny(normalized, "categoria", "categoría", "categorias", "categorías", "electricidad", "agua", "internet", "limpieza", "reparacion", "reparación", "jardineria", "jardinería", "impuesto", "impuestos", "comision", "comisión", "comisiones")
                || (containsAny(normalized, "por", "de", "del") && containsAny(normalized, "pague", "pagué", "pagado", "pagos", "gasto", "gastos"));
    }

    private boolean isRecentPaymentQuestion(String normalized) {
        return containsAny(normalized, "reciente", "recientes", "ultimos", "últimos", "ultimas", "últimas", "last", "recent");
    }

    private boolean isPaymentSummaryQuestion(String normalized) {
        return containsAny(normalized, "cuanto", "cuánto", "total", "totales", "resumen", "summary", "este mes", "esta semana", "este año", "este ano", "mes pasado", "semana pasada", "ano pasado", "año pasado");
    }
}
