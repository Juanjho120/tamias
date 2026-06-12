# Fase 9G — Property and Catalog read-only tools

## Objetivo

Expandir el catálogo de Tool Calling con herramientas read-only enfocadas en propiedades y catálogos configurados por organización.

Esta fase mantiene las reglas aprobadas:

- Read-only first.
- No free SQL generado por el modelo.
- No autonomous writes.
- `organization_id` resuelto por backend desde el usuario autenticado.
- Respuestas con `toolEvidence`.
- Sin exponer URLs privadas ni secretos.

## Tools agregadas o ampliadas

### Property Tools

- `property.getSummary`
- `property.getOperationalOverview`
- `property.getImagesSummary`
- `property.getActiveProperties`
- `property.getInactiveProperties`

`property.search` ya existía desde 9B y se conserva.

Preguntas cubiertas:

- ¿Qué propiedades tengo activas?
- ¿Qué propiedades tengo inactivas?
- Dame un resumen de Lake View Bungalow.
- ¿Qué propiedades no tienen imágenes?
- ¿Qué propiedad tiene más mantenimientos?
- Dame un panorama operativo por propiedad.

### Catalog Tools

- `catalog.maintenanceCategories`
- `catalog.maintenanceTypes`
- `catalog.reservationPlatforms`
- `catalog.taskCategories`
- `catalog.purchaseCategories`
- `catalog.inventoryItemTypes`
- `catalog.search`

Preguntas cubiertas:

- ¿Qué tipos de mantenimiento tengo configurados?
- ¿Qué categorías de mantenimiento existen?
- ¿Qué plataformas de reservación tengo configuradas?
- ¿Qué categorías de compras existen?
- ¿Qué catálogos puedo usar para mantenimiento?
- ¿Qué tipos de items de inventario existen?

## Nota sobre `catalog.purchaseCategories`

En el schema actual no existe una tabla dedicada `purchase_categories`.

Para no inventar estructura, la tool responde usando metadata real de `inventory_items`, filtrando items activos con `available_for_purchases = TRUE` y agrupando por `item_type`.

Esto mantiene el comportamiento read-only y evita queries contra tablas inexistentes.

## Archivos modificados

- `backend/src/main/java/com/tamias/ai/tool/AiReadOnlyToolService.java`
- `backend/src/main/java/com/tamias/ai/tool/AiToolCallingService.java`

## Smoke tests recomendados

```text
¿Qué propiedades tengo activas?
¿Qué propiedades tengo inactivas?
Dame un resumen de Lake View Bungalow.
¿Qué propiedades no tienen imágenes?
Dame un panorama operativo por propiedad.
¿Qué tipos de mantenimiento tengo configurados?
¿Qué categorías de mantenimiento existen?
¿Qué plataformas de reservación tengo configuradas?
¿Qué categorías de compras existen?
¿Qué tipos de items de inventario existen?
¿Qué catálogos puedo usar para mantenimiento?
```

## Commit sugerido

```bash
git commit -m "feat: add property and catalog AI read-only tools"
```
