package com.tamias.ai.tool.handler;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.context.AiToolRequestContext;
import com.tamias.ai.tool.service.ProductBoxReadOnlyToolService;
import com.tamias.ai.tool.support.AiToolRoutingSupport;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(70)
public class ProductBoxToolHandler extends AiToolRoutingSupport implements AiToolHandler {

    private final ProductBoxReadOnlyToolService productBoxReadOnlyToolService;

    public ProductBoxToolHandler(ProductBoxReadOnlyToolService productBoxReadOnlyToolService) {
        this.productBoxReadOnlyToolService = productBoxReadOnlyToolService;
    }

    @Override
    public Optional<AiToolAnswer> tryHandle(AiToolRequestContext context) {
        return tryHandleProductBoxQuestion(context.question(), context.normalizedQuestion());
    }

    private Optional<AiToolAnswer> tryHandleProductBoxQuestion(String question, String normalized) {
        if (!isProductBoxQuestion(normalized)) {
            return Optional.empty();
        }

        if (isWriteRequest(normalized)) {
            return Optional.of(readOnlyGuard());
        }

        if (isIncompleteQuestion(normalized)) {
            return Optional.of(productBoxReadOnlyToolService.productBoxIncompleteModels());
        }

        if (isInventoryWithoutProductBoxQuestion(normalized)) {
            return Optional.of(productBoxReadOnlyToolService.inventoryItemsWithoutProductBox(question));
        }

        if (isInventoryLinkedQuestion(normalized)) {
            return Optional.of(productBoxReadOnlyToolService.productBoxInventoryLinks(question));
        }

        if (isPurchaseLinkedQuestion(normalized)) {
            return Optional.of(productBoxReadOnlyToolService.productBoxPurchaseLinks(question));
        }

        if (isTextureStatusQuestion(normalized)) {
            return Optional.of(productBoxReadOnlyToolService.productBoxTextureStatus(question));
        }

        if (isSummaryQuestion(normalized)) {
            return Optional.of(productBoxReadOnlyToolService.productBoxSummary());
        }

        return Optional.of(productBoxReadOnlyToolService.productBoxSearch(question));
    }

    private boolean isProductBoxQuestion(String normalized) {
        return hasAnyToken(
                normalized,
                "product box",
                "productbox",
                "productboxmodel",
                "productboxmodels",
                "product box model",
                "product box models",
                "box model",
                "box models",
                "3d box",
                "caja 3d",
                "cajas 3d",
                "modelo 3d",
                "modelos 3d",
                "modelo de caja",
                "modelos de caja",
                "modelo product box",
                "modelos product box",
                "textura 3d",
                "texturas 3d"
        ) || (hasAnyToken(normalized, "textura", "texturas", "texture", "textures", "cara", "caras", "face", "faces")
                && hasAnyToken(normalized, "caja", "cajas", "modelo", "modelos", "product", "box"))
                || (hasAnyToken(normalized, "caja", "cajas")
                && hasAnyToken(
                normalized,
                "incompleto",
                "incompletos",
                "faltan",
                "faltante",
                "faltantes",
                "necesitan atencion",
                "necesitan atención"
        ));
    }

    private boolean isWriteRequest(String normalized) {
        return hasAnyToken(
                normalized,
                "crea",
                "crear",
                "agrega",
                "agregar",
                "añade",
                "anade",
                "elimina",
                "eliminar",
                "borra",
                "borrar",
                "actualiza",
                "actualizar",
                "edita",
                "editar",
                "modifica",
                "modificar",
                "sube",
                "subir",
                "carga",
                "cargar",
                "procesa",
                "procesar",
                "genera",
                "generar"
        );
    }

    private boolean isIncompleteQuestion(String normalized) {
        return hasAnyToken(
                normalized,
                "incompleto",
                "incompletos",
                "incompleta",
                "incompletas",
                "faltan",
                "faltante",
                "faltantes",
                "sin cara",
                "sin caras",
                "caras faltantes",
                "missing face",
                "missing faces",
                "needs attention",
                "necesitan atencion",
                "necesitan atención"
        );
    }

    private boolean isInventoryWithoutProductBoxQuestion(String normalized) {
        return isInventoryLinkedQuestion(normalized)
                && hasAnyToken(normalized, "sin", "no tienen", "no tiene", "faltan", "faltante", "faltantes", "without");
    }

    private boolean isInventoryLinkedQuestion(String normalized) {
        return hasAnyToken(
                normalized,
                "inventario",
                "inventory",
                "inventory item",
                "inventory items",
                "item de inventario",
                "items de inventario",
                "producto",
                "productos"
        );
    }

    private boolean isPurchaseLinkedQuestion(String normalized) {
        return hasAnyToken(
                normalized,
                "compra",
                "compras",
                "purchase",
                "purchases",
                "purchase item",
                "purchase items",
                "item de compra",
                "items de compra"
        );
    }

    private boolean isTextureStatusQuestion(String normalized) {
        return hasAnyToken(
                normalized,
                "textura",
                "texturas",
                "texture",
                "textures",
                "original",
                "procesada",
                "procesadas",
                "processed",
                "ai-enhanced",
                "ai enhanced",
                "mejorada",
                "mejoradas",
                "enhanced",
                "ia"
        );
    }

    private boolean isSummaryQuestion(String normalized) {
        return hasAnyToken(
                normalized,
                "resumen",
                "summary",
                "estado",
                "status",
                "panorama",
                "overview",
                "cuantos",
                "cuántos",
                "cantidad",
                "total"
        );
    }

    private boolean hasAnyToken(String normalized, String... tokens) {
        for (String token : tokens) {
            if (normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
