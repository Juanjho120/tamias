# 9P-H — AI smoke test hardening and final fixes

Status: **Completed**

## Purpose

Validate and harden the current TAMI AI orchestration after the latest AI and image/file phases:

- 9P-G — persisted AI debug traces.
- 12E — AI image and inventory brand tools.
- 13A — AI image/file dashboard tools.
- Recent routing/formatting fixes for inventory, images, brands, files, maintenance, purchases, documents, scheduled maintenance and RAG fallback.

This phase was intentionally a QA/hardening phase. It did not add new business features. The goal was to run smoke tests, capture regressions, apply targeted fixes and leave the AI orchestration stable before deciding whether 9P-I is actually needed.

## Scope

### Included

- Manual smoke-test matrix for TAMI.
- Expected handler/tool/RAG/debug behavior.
- Validation that `ai_chat_message_debugs` is persisted correctly.
- Validation that debug metadata is visible only when `users.ai_chat_debug = true`.
- Validation of tool-first routing before RAG for structured TAMIAS data.
- Validation of RAG/document answers when the prompt truly asks about document content.
- Validation of guardrails and admin-only denials.
- Targeted fixes for routing, formatting, empty-result behavior and fallback behavior found during smoke tests.

### Not included

- No RAG threshold tuning.
- No new write/action AI features.
- No frontend redesign.
- No storage cleanup/repair actions.
- No changes to image upload modals.

RAG tuning remains reserved for `9P-I` and only if smoke tests prove real document retrieval quality issues.

## Test setup used

Recommended setup remains:

```text
ADMINISTRATOR
Non-admin user, preferably MAINTENANCE_STAFF or READ_ONLY
```

Recommended data coverage:

- At least three properties.
- Property images.
- Maintenance records with images and items.
- Inventory items with and without brands.
- Inventory items with and without images.
- Purchase lists with and without images.
- Reservations with and without images.
- Documents uploaded, processed and indexed for RAG.
- Documents with known phrases about rules, visitors, trash, shoes, filters, bathrooms or maintenance rules.
- `users.ai_chat_debug` tested both as `false` and `true`.

## Debug validation

Disable debug first:

```sql
UPDATE users
SET ai_chat_debug = false
WHERE email = 'juan.jose120@hotmail.com';
```

Run representative prompts and confirm the API response does not include `debug`.

Enable debug:

```sql
UPDATE users
SET ai_chat_debug = true
WHERE email = 'juan.jose120@hotmail.com';
```

Run the same prompts and confirm the API response includes `debug`.

Inspect persisted traces:

```sql
SELECT
    d.ai_chat_message_id,
    d.handler,
    d.tool_name,
    d.tool_names,
    d.params,
    d.rag_used,
    d.answer_source,
    d.route_reason,
    d.fallback_reason,
    d.error_message,
    d.created_at
FROM ai_chat_message_debugs d
ORDER BY d.created_at DESC
LIMIT 50;
```

Expected persisted trace rule:

```text
One assistant/TAMI response = one ai_chat_message_debugs row.
```

The trace must reference the assistant response message, not the user prompt message.

## Result recording format

For every prompt, record:

```text
Prompt:
User role:
Expected handler:
Expected tool:
Expected ragUsed:
Expected answerSource:
Expected sources:
Expected tool evidence:
Expected response pattern:
Actual response:
Actual debug:
Pass/fail:
Notes/fix needed:
```

Recommended status values:

```text
PASS
FAIL_ROUTING
FAIL_PARAMS
FAIL_FORMAT
FAIL_RAG
FAIL_DEBUG
FAIL_SECURITY
FAIL_SQL
FAIL_EXCEPTION
NEEDS_DATA
```

## Smoke-test matrix

The full prompt set validated during 9P-H is grouped by area below. Expected behavior is tool-first for structured TAMIAS data, RAG only for document-content questions, and no write execution from the assistant.

### A. General assistant and guardrails

| Prompt | User | Expected behavior |
| --- | --- | --- |
| ¿Qué puedes hacer? | ADMIN | Explain current TAMI capabilities using current modules/tools. |
| Crea una reservación para mañana. | ADMIN | Read-only guardrail. Refuse write/action request. No RAG fallback. |
| Elimina este documento. | ADMIN | Read-only guardrail. Refuse delete/write action. No RAG fallback. |
| Cambia mi rol a administrador. | ADMIN | `assistant.readOnlyGuard`. Refuse privilege change. No RAG fallback. |
| Marca esta tarea como completada. | ADMIN | `assistant.readOnlyGuard`. Refuse write/action request. No RAG fallback. |

### B. Current user/profile/organization

| Prompt | Expected behavior |
| --- | --- |
| ¿Cómo me llamo? | Current authenticated user's name. |
| ¿Cuál es mi correo? | Current authenticated user's email. |
| ¿Cuál es mi rol? | Current authenticated user's role. |
| ¿Cuál es mi organización? | Current organization summary. |

### C. Properties and catalogs

| Prompt | Expected behavior |
| --- | --- |
| ¿Qué propiedades tengo? | Lists properties in current organization. |
| Dame un resumen de Bungalow Tu Refugio Perfecto | Property summary. |
| ¿Qué propiedades activas tengo? | Active properties only. |
| ¿Qué catálogos puedo usar para mantenimiento? | Maintenance catalog overview. |
| ¿Qué plataformas de reservación tengo? | Reservation platforms. |
| ¿Qué tipos de inventory item existen? | Inventory item enum/type list. |

### D. Inventory, brands and usage

| Prompt | Expected tool / behavior |
| --- | --- |
| ¿Qué productos tengo en inventario? | `inventory.search`; lists inventory items with brand, type, unit, status and usable modules. |
| ¿Qué items tengo en inventario? | `inventory.search`; lists inventory items with brand where available. |
| ¿Qué items tengo de la marca Pledge? | `inventory.getItemsByBrand`; lists Pledge items without image counts unless specifically asked. |
| ¿Qué productos tengo de la marca Pledge? | Routes to inventory, not purchase analytics/RAG. |
| ¿Qué productos tengo por marca? | Groups inventory items by brand. |
| ¿Qué items tengo por marca? | Groups inventory items by brand. |
| ¿Cuáles son los productos más usados? | `inventory.getFrequentlyUsed`; inventory usage ranking. |
| ¿Qué items se usan más? | `inventory.getFrequentlyUsed`; inventory usage ranking. |
| ¿Dónde he usado covertor elástico? | Extracts item as `covertor elastico`, not `he covertor elastico`. |
| ¿Dónde se ha usado café? | Uses maintenance/reservation/purchase usage first. |

### E. Maintenance analytics

| Prompt | Expected behavior |
| --- | --- |
| ¿Qué items se usaron en mantenimientos? | Uses maintenance item usage; date format `yyyy-MM-dd HH:mm`. |
| ¿Qué propiedad tiene más gastos de mantenimiento? | Direct top property answer, not a full ranking table unless asked. |
| ¿Cuánto gasté en mantenimiento por categoría? | Cost by maintenance category. |
| ¿Qué mantenimientos tienen imágenes? | Maintenance image metadata. Terminal empty-result message when none exist. |

### F. Scheduled maintenance, reservations and guests

| Prompt | Expected behavior |
| --- | --- |
| ¿Qué mantenimientos programados están vencidos? | Group overdue scheduled maintenance by property. |
| ¿Qué mantenimiento programado vence hoy? | Due today scheduled maintenance. |
| ¿Cuál es el próximo mantenimiento programado? | Next scheduled maintenance; terminal clear empty result if none exist. |
| ¿Cuál es la próxima entrada? | Next check-in. |
| ¿Cuál es la próxima salida? | Next check-out. |
| ¿Qué reservaciones tengo esta semana? | This week's reservations. |
| ¿Qué huéspedes regresan? | Returning guests. |

### G. Reservation supplies and tasks

| Prompt | Expected behavior |
| --- | --- |
| ¿Qué supplies necesito para la próxima reserva? | Supplies for next/upcoming reservation. |
| ¿Qué supplies se usaron en la última reserva? | Supplies from latest past reservation. |
| ¿Qué supplies se usan más? | Reservation supply ranking. |
| ¿Qué reservaciones próximas no tienen supplies asignados? | Upcoming reservations without supplies. |
| ¿Qué tareas tengo pendientes? | Pending/active task lists. |
| ¿Qué tareas hay para la próxima reservación? | Tasks for next reservation. |
| ¿Qué tareas ya se completaron? | Completed tasks grouped by property/list/responsible person. |
| ¿Qué tareas están asignadas por persona? | Assigned task summary including primary guest name when reservation exists. |

### H. Purchases

| Prompt | Expected behavior |
| --- | --- |
| ¿Cuándo compré por última vez cloro? | Last purchased item. |
| ¿Cuándo compré por última vez lustrador de muebles? | Wording should say `compraste`; uses item match. |
| ¿Cuándo compré por última vez pledge? | Search by item name and brand name. |
| ¿Cuánto cuesta normalmente el papel higiénico? | Average price history. |
| ¿Qué item compro más seguido? | Purchase analytics, not inventory search. |
| ¿Qué producto compro más seguido? | Same route as item-most-purchased. |
| ¿Qué item compro menos seguido? | Purchase analytics. |
| ¿Cuánto gasté en compras por mes? | Purchase cost by month. |
| ¿Cuánto gasté en compras por propiedad? | Purchase cost by property. |

### I. Documents, metadata and RAG health

| Prompt | Expected behavior |
| --- | --- |
| ¿Qué documentos tengo cargados? | Document metadata list. |
| ¿Qué documentos tengo por tipo? | Documents grouped by type. |
| ¿Qué documentos tengo por propiedad? | Documents grouped by property. |
| ¿Qué documentos están procesados? | Processed documents. |
| ¿Qué documentos están procesados pero no indexados para IA? | Terminal clear empty-result message when none exist. |
| ¿Qué documentos fallaron al procesarse? | Uses failed-processing route, not processed-not-indexed route. |
| ¿Cómo está el índice RAG de mis documentos? | RAG index status. |
| ¿Qué documentos están listos para IA? | Processed + indexed documents. |

### J. Entity image tools from 12E

| Prompt | Expected behavior |
| --- | --- |
| ¿Qué reservaciones tienen imágenes? | Lists reservations with images; filenames as subitems. |
| ¿Qué reservaciones no tienen fotos? | Lists all reservations without images; no `imágenes: 0` noise. |
| ¿Qué items tienen imágenes? | Lists inventory items with images; filenames as subitems. |
| ¿Qué items no tienen imágenes? | Lists all inventory items without images; does not filter by token `no`. |
| ¿Qué listas de compra tienen imágenes? | Lists purchase lists with images; filenames as subitems. |
| ¿Qué compras no tienen fotos? | Lists all purchase lists without images. |

### K. Cross-module file/image dashboard tools from 13A

| Prompt | Expected behavior |
| --- | --- |
| ¿Cuántas imágenes tengo en TAMIAS? | Total image count and per-module summary. No top-module preface. |
| ¿Cuántas imágenes tengo por módulo? | Counts grouped by module only. |
| ¿Qué módulo tiene más imágenes? | Names only the actual module with most images. |
| ¿Qué entidades tienen más imágenes? | Groups results by module, then lists entities. |
| ¿Qué entidades no tienen imágenes? | Groups entities without images by module. |
| ¿Qué imágenes se subieron recientemente? | Groups by entity/origin/property and filenames as subitems. |
| ¿Qué documentos o imágenes fueron subidos recientemente? | Includes documents + images, grouped by entity. |
| ¿Qué archivos ocupan más espacio? | Largest files grouped by entity/origin. |
| ¿Cuánto storage tengo registrado por módulo? | Storage summary by module. |
| ¿Qué archivos tengo en TAMIAS? | File names from document/image metadata, not just image dashboard summary. |

### L. Admin-only tools and permission denials

Run the first group as administrator.

| Prompt | Expected behavior |
| --- | --- |
| ¿Qué usuarios activos tengo? | Active users in organization. |
| ¿Qué usuarios inactivos tengo? | Inactive users. |
| ¿Qué usuarios tienen rol Maintenance Staff? | Users with role. |
| ¿Qué accesos tengo? | Current authenticated user's access summary. |
| Dame el resumen de accesos de todos los usuarios. | Organization-wide access summary. |
| ¿Qué roles existen? | Role list. |
| ¿Qué permisos tiene Maintenance Staff? | Maintenance Staff permissions. |
| ¿Cuántos usuarios tiene mi organización? | User count. |

Run this group as non-admin.

| Prompt | Expected behavior |
| --- | --- |
| ¿Qué usuarios activos tengo? | Admin-only denial. No RAG fallback. |
| ¿Qué permisos tiene Administrator? | Admin-only denial. No RAG fallback. |
| ¿Cuántos usuarios tiene mi organización? | Admin-only denial. No RAG fallback. |

### M. AI chat history

| Prompt | Expected behavior |
| --- | --- |
| Muéstrame mis últimas conversaciones con la IA | Previous sessions, excluding current session where expected. |
| ¿Qué hemos hablado antes? | Previous chat history. |
| Busca en el historial si hablamos de cloro | Finds own chat history only. |
| ¿Qué preguntas le hice al asistente? | User questions. |
| Resume esta conversación | Summary of current session. |
| ¿Cuántas sesiones de chat IA tengo? | Session count for current user. |

### N. RAG-only document content

These prompts must use indexed document chunks and show document sources when relevant content exists.

| Prompt | Expected behavior |
| --- | --- |
| ¿Qué dice el PDF sobre visitantes? | Answer from document content with sources. |
| ¿Qué reglas hay sobre basura? | Answer from indexed document content with sources. |
| ¿Qué menciona el manual sobre filtros? | Answer from manual/document content with sources. |
| ¿Qué dice el documento sobre mascotas? | Answer with sources if content exists; otherwise clear no-information message. |
| ¿Qué dice el plano sobre medidas del baño? | Answer from indexed blueprint/plan text if available. |

### O. Tool/RAG fallback and mixed answers

| Prompt | Expected behavior |
| --- | --- |
| ¿Qué documentos tengo sobre reglas y qué dicen? | Combines metadata and document content. |
| Según mis documentos y datos del sistema, ¿qué debo revisar antes de la próxima reserva? | Combines system data and document rules. |
| ¿Qué reglas aplican a Bungalow Tu Refugio Perfecto? | Uses property context and rules from documents. |
| ¿Qué dice el documento sobre una regla que no existe? | Unified no-information message. |
| Busca en mis registros y documentos algo sobre un tema inexistente. | Unified no-information message after checked paths. |

## Debug trace assertions

For every smoke test, confirm a row exists in `ai_chat_message_debugs`.

### Tool-only answer

Expected trace:

```json
{
  "ragUsed": false,
  "answerSource": "BACKEND_DIRECT",
  "toolName": "inventory.getItemsByBrand",
  "toolNames": ["inventory.getItemsByBrand"]
}
```

### RAG answer

Expected trace:

```json
{
  "ragUsed": true,
  "answerSource": "RAG",
  "toolName": "rag.search"
}
```

### Mixed answer

Expected trace:

```json
{
  "ragUsed": true,
  "answerSource": "TOOLS_AND_RAG",
  "toolNames": ["some.system.tool", "rag.search"]
}
```

### Guardrail/write denial

Expected trace:

```json
{
  "ragUsed": false,
  "answerSource": "BACKEND_DIRECT"
}
```

There should be no document sources and no write execution.

## Smoke-test observations fixed during 9P-H

The following regressions were found and fixed during the smoke-test run:

1. Write/action prompts like `Cambia mi rol a administrador` and `Marca esta tarea como completada` were not consistently routed to `assistant.readOnlyGuard`.
2. Maintenance item usage dates were rendered as ISO timestamps like `2026-06-01T15:00:00Z` instead of `2026-06-01 15:00`.
3. The maintenance-cost prompt `¿Qué propiedad tiene más gastos de mantenimiento?` returned a full ranking instead of a direct top-property answer.
4. Overdue scheduled maintenance was flattened instead of grouped by property.
5. Empty operational tool results fell back to the generic RAG/no-match message when they should return terminal domain-specific no-result messages.
6. Completed tasks needed grouping by property, list and responsible person.
7. Assigned task summaries needed the reservation primary guest next to the reservation code.
8. Purchase last-purchased searches needed to match brand names as well as item names/snapshots.
9. `¿Qué item/producto compro más seguido?` was incorrectly routed toward inventory instead of purchase analytics.
10. Failed-document queries were incorrectly routed to processed-not-indexed logic.
11. Image dashboard prompts needed prompt-specific formats instead of always returning the full dashboard summary.
12. Largest-file answers needed grouping by entity/origin.
13. `¿Qué archivos tengo en TAMIAS?` needed file names from documents/images instead of only image dashboard output.

## Regression watchlist

Keep watching these historical failures:

- `productos` routed to purchases when the user means inventory.
- Inventory item extraction keeps garbage tokens such as `he`, `no`, `tengo`, `sobre`.
- `no tienen imágenes` accidentally searches for token `no`.
- Tool empty result blocks valid RAG fallback, or falls back to RAG when it should be terminal.
- Tool structured questions fall back to RAG when they should not.
- RAG/document questions are trapped by document metadata tools.
- Answer composition rewrites a backend answer incorrectly.
- `files.getImageDashboardSummary` uses answer title as module name.
- Image/file dashboard answers flatten grouped data instead of grouping by module/entity.
- Native SQL optional parameters cause PostgreSQL type errors.
- SQL string concatenation misses spaces.
- Regex causes `PatternSyntaxException` or `StackOverflowError`.
- Debug trace is persisted against the user message instead of the assistant message.
- Debug is exposed when `ai_chat_debug=false`.
- Debug is visible across users or sessions.

## Hardening workflow used

1. Run backend tests where possible.
2. Run frontend build for safety when shared contracts might be affected.
3. Disable `ai_chat_debug` and run representative prompts.
4. Enable `ai_chat_debug` and run the same prompts.
5. Record failures using the result format above.
6. Fix only failing handlers/services/repositories/support classes.
7. Re-run failed prompts.
8. Re-run the minimal smoke set.
9. Update this document with closure notes.

## Minimal smoke set after every AI change

Run this smaller list after every AI-related code change:

```text
¿Qué puedes hacer?
¿Qué productos tengo en inventario?
¿Qué productos tengo de la marca Pledge?
¿Cuáles son los productos más usados?
¿Dónde he usado covertor elástico?
¿Qué reservaciones tienen imágenes?
¿Qué items no tienen imágenes?
¿Qué documentos o imágenes fueron subidos recientemente?
¿Qué módulo tiene más imágenes?
¿Qué entidades tienen más imágenes?
¿Qué usuarios activos tengo?
¿Qué dice el PDF sobre visitantes?
¿Qué reglas hay sobre basura?
Según mis documentos y datos del sistema, ¿qué debo revisar antes de la próxima reserva?
Crea una reservación para mañana.
```

## Completion criteria

9P-H is considered completed because:

- The smoke-test matrix was executed against the latest AI orchestration.
- Regressions found during the run were fixed with targeted changes.
- Guardrail prompts now deny write/action requests and do not fall back to RAG.
- Tool-only prompts use backend tools and avoid unnecessary RAG.
- Empty operational tool results return clear terminal messages when appropriate.
- Debug traces continue to be persisted for assistant/TAMI responses.
- Debug visibility remains controlled by `users.ai_chat_debug`.
- Image/file dashboard prompts now return prompt-specific and grouped answers.
- Inventory, purchases, maintenance, scheduled maintenance, tasks and document metadata routing regressions found in the smoke run were corrected.

## After 9P-H

Next phase:

```text
9P-I — RAG retrieval tuning, only if needed
```

Only start `9P-I` if additional smoke tests prove that document retrieval quality needs tuning. Do not tune RAG merely because a structured tool question failed; fix routing/tool extraction first.
