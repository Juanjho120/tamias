# 12E — AI image and inventory brand tools

Status: Completed.

## Objetivo

Agregar nuevas tools read-only para que TAMI pueda responder preguntas sobre:

1. Imágenes asociadas a reservaciones.
2. Imágenes asociadas a inventory items.
3. Imágenes asociadas a purchase lists.
4. Items de inventario por marca.
5. Información de marca dentro de las respuestas existentes de inventory items.

Esta fase no agrega nuevas tablas ni endpoints REST. Se apoya en las tablas creadas en 12B, 12C y 12D.

## Alcance

### Incluido

- Consultas read-only sobre `reservation_images`.
- Consultas read-only sobre `inventory_item_images`.
- Consultas read-only sobre `purchase_images`.
- Consultas read-only para items por marca usando `inventory_items.brand_id`.
- Actualización de respuestas de inventory tools para incluir `brandName`.
- Routing AI para detectar preguntas sobre imágenes de reservaciones, compras e items.
- Routing AI para detectar preguntas de items por marca.
- Ajustes de routing para que preguntas con `productos` puedan caer en inventory cuando el contexto sea inventario/marca/uso general, sin robar preguntas claramente de compras.
- Limpieza de términos de control en extractores para evitar búsquedas incorrectas como `no`, `he`, `tienen`, `fotos` o `imagenes`.

### No incluido

- No se crean migraciones nuevas.
- No se modifica frontend.
- No se crean endpoints REST nuevos.
- No se crean operaciones write desde IA.
- No se generan, editan, eliminan ni descargan imágenes desde IA.
- No se exponen URLs firmadas desde estas tools.

## Decisión de arquitectura

El backend de IA ya está organizado por capas dentro de `com.tamias.ai.tool`:

- `handler`: ruteo de preguntas hacia tools.
- `service`: fachada read-only transaccional.
- `repository`: SQL read-only y armado de `AiToolAnswer`.
- `support`: helpers compartidos de ruteo y consulta.

Para mantener archivos pequeños y no inflar clases existentes:

- Las nuevas tools de imágenes se agregan en clases nuevas:
  - `EntityImageToolHandler`
  - `EntityImageReadOnlyToolService`
  - `EntityImageToolRepository`
- Las tools existentes de inventario se extienden en las clases actuales:
  - `InventoryToolHandler`
  - `InventoryReadOnlyToolService`
  - `InventoryToolRepository`

La razón es que las imágenes de reservaciones/items/compras cruzan tres módulos funcionales distintos, mientras que las consultas por marca pertenecen directamente al módulo de inventario.

## Tools agregadas

### `images.getReservationImages`

Permite responder preguntas como:

- ¿Qué reservaciones tienen imágenes?
- ¿Qué reservas no tienen fotos?
- ¿La reserva de Lake View tiene imágenes?
- ¿Qué fotos hay para la reservación ABC?

Consulta principal:

- `reservations`
- `reservation_images`
- `properties`
- `platforms`
- `reservation_guests`
- `guests`

Siempre filtra por `organization_id` y excluye reservaciones con `deleted_at`.

### `images.getInventoryItemImages`

Permite responder preguntas como:

- ¿Qué items tienen imágenes?
- ¿Qué items no tienen imágenes?
- ¿El café tiene fotos?
- ¿Qué imágenes tiene el item X?

Consulta principal:

- `inventory_items`
- `inventory_item_images`
- `brands`

Siempre filtra por `organization_id` y excluye inventory items con `deleted_at`.

### `images.getPurchaseListImages`

Permite responder preguntas como:

- ¿Qué compras tienen imágenes?
- ¿Qué listas de compra no tienen fotos?
- ¿La compra de hoy tiene imágenes?
- ¿Qué imágenes tiene la lista de compras de la propiedad X?

Consulta principal:

- `purchase_lists`
- `purchase_images`
- `properties`
- `suppliers`
- `cities`

Siempre filtra por `organization_id` y excluye purchase lists con `deleted_at`.

### `inventory.getItemsByBrand`

Permite responder preguntas como:

- ¿Qué items tengo de la marca X?
- ¿Qué productos son marca Oster?
- ¿Qué items por marca tengo registrados?
- ¿Tengo algo de la marca Samsung?

Consulta principal:

- `inventory_items`
- `brands`
- `inventory_item_images` para conteo interno cuando la respuesta lo necesite.

Siempre filtra por `organization_id` y excluye inventory items/brands con `deleted_at`.

## Actualización de tools existentes

Las siguientes tools de inventario ahora incluyen `brandName` cuando existe:

- `inventory.search`
- `inventory.getFrequentlyUsed`
- `inventory.getUnusedItems`
- `inventory.getItemsUsedInReservations`
- `inventory.getItemsUsedInPurchases`
- `inventory.getItemsUsedInMaintenance`

## Routing esperado

Preguntas que deben ir a inventory:

```text
¿Qué productos tengo en inventario?
¿Qué productos tengo de la marca Pledge?
¿Qué items tengo por marca?
¿Qué productos tengo por marca?
¿Cuáles son los productos más usados?
¿Dónde he usado covertor elástico?
```

Preguntas que pueden seguir yendo a purchase analytics cuando el contexto sea claramente compras:

```text
¿Qué productos he comprado?
¿Qué productos compré en La Torre?
¿Cuáles son los productos más comprados?
```

## Formato de respuesta esperado

### Imágenes existentes

Cuando una entidad tiene imágenes, el resumen de la entidad debe ir en la línea principal y los archivos deben listarse como subitems:

```text
Estas reservaciones tienen imágenes registradas:
- Bungalow Tu Refugio Perfecto | código: JHK59745 | check-in: 2026-07-01 | check-out: 2026-07-08 | imágenes: 1
  - a3a417_051d2e2ead9c4e19939170196db54fd.jpg
```

No colocar los archivos inline como `| archivos: ...` cuando hay nombres de archivo disponibles.

### Entidades sin imágenes

Cuando la pregunta sea por entidades sin imágenes, no mostrar `| imágenes: 0` porque es redundante:

```text
Estas listas de compra no tienen imágenes registradas:
- fecha: 2026-06-08 | propiedad: — | proveedor: La Torre | ciudad: Guatemala | estado: OPEN
```

### Items por marca específica

```text
Encontré estos items relacionados con la marca “pledge”:
- Limpiador Multisuperficies | marca: Pledge | tipo: MATERIAL | unidad: —
- Lustrador de Muebles y Madera | marca: Pledge | tipo: MATERIAL | unidad: —
```

No incluir `estado` ni `imágenes` en esta respuesta salvo que la pregunta lo pida explícitamente.

### Items agrupados por marca

```text
Estos son los items de inventario agrupados por marca:

LG
- Estufa | tipo: SUPPLY | unidad: —

Pledge
- Limpiador Multisuperficies | tipo: MATERIAL | unidad: —
- Lustrador de Muebles y Madera | tipo: MATERIAL | unidad: —
```

## Seguridad multi-tenant

Todas las consultas usan `currentUserService.getCurrentOrganizationId()` y filtran por `organization_id`. No se debe retornar información de otra organización.

## Estado de imágenes

Las queries de imágenes usan imágenes con `status = 'ACTIVE'`.

Las tablas de imágenes usan hard delete. Si una imagen se elimina desde la aplicación, no debería quedar registro físico disponible para estas tools.

## Validación manual

### Imágenes de reservaciones

- ¿Qué reservaciones tienen imágenes?
- ¿Qué reservas no tienen fotos?
- ¿La reserva de Airbnb tiene imágenes?

### Imágenes de items

- ¿Qué items tienen imágenes?
- ¿Qué items no tienen imágenes?
- ¿Qué fotos tiene el café?

### Imágenes de compras

- ¿Qué listas de compra tienen imágenes?
- ¿Qué compras no tienen fotos?
- ¿La compra de hoy tiene imágenes?

### Items por marca

- ¿Qué items tengo de la marca X?
- ¿Qué productos tengo por marca?
- ¿Tengo items marca Oster?

### Marca en respuestas existentes

- ¿Qué items tengo en inventario?
- ¿Cuáles son los items más usados?
- ¿Dónde he usado café?
- ¿Qué items no se han usado?

Las respuestas deben incluir la marca cuando el item tenga `brand_id` asociado.
