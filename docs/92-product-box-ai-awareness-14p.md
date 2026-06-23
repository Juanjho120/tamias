# 14P — AI Awareness for Product Box Models

## Status

Implemented and refined.

## Goal

Add read-only AI tool awareness for Product Box Models so TAMI can answer operational questions about 3D box models, linked inventory items, linked purchase items, missing faces, and texture readiness.

This phase keeps the assistant read-only. It does not create, edit, delete, upload, process, or generate Product Box assets.

## Scope

### Backend

AI tool classes were added under the existing AI tool architecture:

```text
backend/src/main/java/com/tamias/ai/tool/handler/ProductBoxToolHandler.java
backend/src/main/java/com/tamias/ai/tool/service/ProductBoxReadOnlyToolService.java
backend/src/main/java/com/tamias/ai/tool/repository/ProductBoxToolRepository.java
```

The implementation follows the existing handler → read-only service → repository structure used by the other AI tools.

### Orchestration hardening

Product Box questions are structured operational questions, not document/RAG questions. The orchestration layer must treat `productBox.*` tools as deterministic backend tools:

- `AiPlanningService` must respect Product Box tool hits before asking the LLM planner to choose a path.
- `AiToolFallbackPolicy` must not send Product Box empty answers to RAG fallback.
- `AiAnswerCompositionService` must return Product Box backend answers as-is, without LLM rewriting.

This prevents prompts such as:

```text
Dame un resumen de Product Box Models.
¿Qué items de inventario tienen Product Box?
Qué modelos Product Box están asociados a compras?
```

from falling through to the generic RAG message:

```text
No encontré información relacionada con lo que preguntaste en los documentos indexados/RAG.
```

### Database

No Flyway migration is required for this phase.

The current Product Box schema already contains the fields needed for AI awareness:

- `product_box_models.inventory_item_id`
- `product_box_models.purchase_item_id`
- `product_box_model_faces.face_name`
- `product_box_model_faces.s3_key`
- `product_box_model_faces.original_s3_key`
- `product_box_model_faces.processed_s3_key`
- `product_box_model_faces.ai_enhanced_s3_key`
- `product_box_model_faces.texture_status`
- `product_box_model_faces.ai_enhancement_status`
- `product_box_model_faces.active_texture_source`

Inventory and purchase link answers also reuse the existing inventory brand relationship:

- `inventory_items.brand_id`
- `brands.name`

## Supported questions

TAMI can answer Product Box questions such as:

```text
Qué productos tienen modelo 3D?
Qué items de inventario tienen Product Box?
Qué items de inventario no tienen Product Box?
Qué modelos de caja están incompletos?
Qué caras le faltan a los modelos Product Box?
Qué modelos tienen textura original, procesada o AI-enhanced?
Qué modelos Product Box están asociados a compras?
Dame un resumen de Product Box Models.
```

## Tools / evidence labels

The Product Box AI tools return grounded evidence with these tool names:

```text
productBox.summary
productBox.search
productBox.incompleteModels
productBox.inventoryLinks
productBox.inventoryItemsWithoutModel
productBox.purchaseLinks
productBox.textureStatus
```

## Behavior

### Summary

Returns aggregate Product Box counts:

- total models
- models linked to inventory
- models linked to purchases
- complete models
- incomplete models
- active texture faces
- original texture faces
- processed texture faces
- AI-enhanced texture faces

The summary prompt must route to `productBox.summary`, not to RAG.

### Incomplete models

A Product Box Model is considered complete when it has texture data for all six expected faces:

```text
front
back
left
right
top
bottom
```

A face is considered to have texture data when at least one of the following fields exists:

```text
s3_key
original_s3_key
processed_s3_key
ai_enhanced_s3_key
```

### Inventory awareness

TAMI can list Product Box Models linked to inventory items and can also list active inventory items that do not yet have a Product Box Model.

Inventory link questions intentionally use a concise answer format:

```text
Los modelos Product Box están asociados a los siguientes items de inventario:
- <Product Box Model> | inventario: <Brand> <Inventory Item>
```

Texture counts, face counts and AI statuses are not included in this answer because they belong to texture/status questions.

If the inventory item name already contains the brand, the brand is not duplicated.

### Purchase awareness

TAMI can list Product Box Models linked to purchase items using `purchase_items.item_name_snapshot` for the purchase item label.

When the purchase item is linked to an inventory item with a brand, the answer prepends that brand to the purchase item label and to the inventory label when shown:

```text
Los modelos Product Box están asociados a los siguientes items de compra:
- <Product Box Model> | compra: <Brand> <Purchase Item> | inventario: <Brand> <Inventory Item>
```

Generic wording such as `están asociados a compras` must not be treated as a search term. It should list all Product Box Models with `product_box_models.purchase_item_id` populated for the current organization.

If the item name already contains the brand, the brand is not duplicated.

### Texture awareness

TAMI can report the amount of original, processed, and AI-enhanced texture faces per model, along with texture and AI enhancement statuses.

## Read-only guard

If the user asks TAMI to create, edit, delete, upload, process, or generate Product Box data, the handler returns the existing read-only guard response instead of attempting to mutate data.

## Manual smoke tests

Ask TAMI:

```text
Dame un resumen de Product Box Models.
Qué modelos de caja están incompletos?
Qué items de inventario tienen Product Box?
Qué items de inventario no tienen Product Box?
Qué modelos Product Box están asociados a compras?
Qué modelos tienen textura AI-enhanced?
Crea un Product Box para el café.
```

Expected behavior:

- `Dame un resumen de Product Box Models.` returns a grounded answer with `productBox.summary` evidence.
- `Qué items de inventario tienen Product Box?` returns a concise inventory-link answer with `productBox.inventoryLinks` evidence and brand + item labels.
- `Qué modelos Product Box están asociados a compras?` returns Product Box Models with populated `purchase_item_id`, `productBox.purchaseLinks` evidence and brand + item labels when the purchase item has an inventory brand.
- Product Box operational prompts do not fall back to the generic RAG no-information answer.
- Texture/status prompts still return detailed texture metrics.
- The creation prompt returns the read-only guard response.

## Verification checklist

- Backend compiles.
- Existing AI tools still route normally.
- Product Box questions are handled before generic inventory/purchase handlers.
- Questions involving `inventario` or `compras` plus Product Box route to `ProductBoxToolHandler`.
- `productBox.*` tools are respected by `AiPlanningService` as structured operational tools.
- Product Box empty responses do not attempt RAG fallback.
- Product Box backend answers are returned as-is by answer composition.
- Generic relation words such as `asociado`, `asociados`, `vinculado`, `relacionado` are not treated as search terms.
- Inventory and purchase link answers concatenate brand + item name without duplicating the brand.
- No Product Box write operation is executed from the assistant.
- No Flyway migration is needed.
