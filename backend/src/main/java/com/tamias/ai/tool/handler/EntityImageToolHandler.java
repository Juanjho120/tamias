package com.tamias.ai.tool.handler;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.context.AiToolRequestContext;
import com.tamias.ai.tool.service.EntityImageReadOnlyToolService;
import com.tamias.ai.tool.support.AiToolRoutingSupport;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(45)
public class EntityImageToolHandler extends AiToolRoutingSupport implements AiToolHandler {

    private final EntityImageReadOnlyToolService imageReadOnlyToolService;

    public EntityImageToolHandler(EntityImageReadOnlyToolService imageReadOnlyToolService) {
        this.imageReadOnlyToolService = imageReadOnlyToolService;
    }

    @Override
    public Optional<AiToolAnswer> tryHandle(AiToolRequestContext context) {
        String normalized = context.normalizedQuestion();

        if (!isImageQuestion(normalized)) {
            return Optional.empty();
        }

        boolean withoutImages = isWithoutImagesQuestion(normalized);

        if (isReservationImageQuestion(normalized)) {
            return Optional.of(imageReadOnlyToolService.reservationImages(context.question(), withoutImages));
        }

        if (isPurchaseImageQuestion(normalized)) {
            return Optional.of(imageReadOnlyToolService.purchaseImages(context.question(), withoutImages));
        }

        if (isInventoryItemImageQuestion(normalized)) {
            return Optional.of(imageReadOnlyToolService.inventoryItemImages(context.question(), withoutImages));
        }

        return Optional.empty();
    }

    private boolean isImageQuestion(String normalized) {
        return containsAny(normalized, "imagen", "imagenes", "image", "images", "foto", "fotos", "fotografia", "fotografias");
    }

    private boolean isWithoutImagesQuestion(String normalized) {
        return containsAny(
            normalized,
            "sin imagen", "sin imagenes", "sin foto", "sin fotos", "no tienen imagen", "no tiene imagen", "no tienen foto", "no tiene foto",
            "without image", "without images", "missing image", "missing images", "no images"
        );
    }

    private boolean isReservationImageQuestion(String normalized) {
        return containsAny(normalized, "reservacion", "reservaciones", "reserva", "reservas", "reservation", "reservations");
    }

    private boolean isPurchaseImageQuestion(String normalized) {
        return containsAny(
            normalized,
            "compra", "compras", "lista de compra", "listas de compra", "listado de compra", "listados de compra", "purchase", "purchases", "purchase list", "purchase lists"
        );
    }

    private boolean isInventoryItemImageQuestion(String normalized) {
        return containsAny(
            normalized,
            "item", "items", "inventario", "inventory", "producto", "productos", "catalogo", "catalogos", "catalog", "catalogs", "repuesto", "repuestos", "material", "materiales"
        );
    }
}
