# 15D — Icon-only Action Buttons with Tooltips

## Status

Implemented.

## Goal

Standardize TAMIAS action buttons so table and modal actions display only an icon while still keeping accessible labels and hover/focus tooltips.

This applies across modules, including but not limited to:

- Properties
- Catalogs / Inventory
- Maintenance
- Scheduled Maintenance
- Reservations
- Tasks
- Purchases
- Documents
- Product Box Models
- Organizations
- Users

## Design decision

15D uses two layers:

1. `IconActionButtonComponent` for new/refactored code that should explicitly render an icon-only action button.
2. `IconActionButtonAutoEnhancerService` as a global compatibility layer for existing buttons across all current modules.

The compatibility layer is initialized once from `app.config.ts` and observes DOM changes. It enhances existing Bootstrap action buttons inside tables, modals, button groups and dropdown menus.

## Behavior

For buttons in action scopes, the enhancer:

- Detects an existing Bootstrap icon when present.
- Uses the visible translated text as the tooltip/accessible label.
- Adds `title` when missing.
- Adds `aria-label` when missing.
- Applies an icon-only visual class.
- Adds a known icon automatically for common text-only actions such as Cancel, Save, Open, Process, Index, History, Pause, Reschedule and Generate record.

## Safety rules

The enhancer skips:

- `.btn-close`
- buttons marked with `data-tamias-icon-action="false"`
- buttons outside tables, modals, button groups, dropdown menus or explicit action scopes
- buttons where no icon can be detected or inferred

This avoids changing large page-level call-to-action buttons accidentally.

## Supported common actions

The automatic icon mapping includes English and Spanish labels for:

- Images / Imágenes
- Edit / Editar
- Delete / Eliminar
- 3D Box Models / Modelos 3D de cajas
- Faces / Caras
- Upload original / Subir original
- Upload image / Subir imagen
- Replace / Reemplazar
- Detect contour / Detectar contorno
- Items
- Tasks / Tareas
- Details / Detalles
- Supplies
- Cancel / Cancelar
- Deactivate / Desactivar
- Activate / Activar
- Open / Abrir
- Process / Procesar
- Index / Indexar
- Chunks
- History / Historial
- Reschedule / Reprogramar
- Pause / Pausar
- Generate record / Generar registro
- Memberships / Membresías
- Save / Guardar
- Confirm / Confirmar

## Verification checklist

- Buttons in Users actions are icon-only with tooltip.
- Buttons in Organizations actions are icon-only with tooltip.
- Buttons in Product Box tables/modals are icon-only with tooltip.
- Buttons in Purchases modals/tables are icon-only with tooltip.
- Buttons in Reservations, Tasks, Maintenance and Documents actions are icon-only with tooltip.
- Text-only modal footer actions such as Cancel/Save/Confirm receive an icon when their label is known.
- Main page-level primary actions are not unexpectedly converted unless they are inside an action scope.
- No native `confirm()` was introduced.
