# 9P-H — AI smoke test hardening and final fixes

Status: **Design ready / execution next**

## Purpose

Validate the current TAMI AI orchestration after the latest phases:

- 9P-G — persisted AI debug traces
- 12E — AI image and inventory brand tools
- 13A — AI image/file dashboard tools
- Recent routing/formatting fixes for inventory, images, brands, files and RAG fallback

This phase is intentionally a QA/hardening phase. It should not add new business features. The goal is to run a repeatable smoke-test matrix, capture regressions, and then apply only targeted fixes.

## Scope

### Included

- Manual smoke-test matrix for TAMI.
- Expected handler/tool/RAG/debug behavior.
- Validation that `ai_chat_message_debugs` is persisted correctly.
- Validation that debug metadata is visible only when `users.ai_chat_debug = true`.
- Validation of tool-first routing before RAG for structured TAMIAS data.
- Validation of RAG/document answers when the prompt truly asks about document content.
- Validation of guardrails and admin-only denials.

### Not included

- No RAG threshold tuning unless a test proves it is needed.
- No new tools.
- No write/action AI features.
- No frontend redesign.
- No storage cleanup/repair actions.
- No changes to image upload modals.

RAG tuning remains reserved for `9P-I` and only if the smoke tests show real retrieval issues.

## Test setup

Use at least two users:

```text
ADMINISTRATOR
Non-admin user, preferably MAINTENANCE_STAFF or READ_ONLY
```

Recommended data setup:

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

## Debug setup

Disable debug first:

```sql
UPDATE users
SET ai_chat_debug = false
WHERE email = 'juan.jose120@hotmail.com';
```

Then run a subset of the smoke tests and confirm the API response does not include `debug`.

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

### A. General assistant and guardrails

| Prompt | User | Expected handler | Expected tool | ragUsed | answerSource | Expected result |
| --- | --- | --- | --- | --- | --- | --- |
| ¿Qué puedes hacer? | ADMIN | capability/general handler | assistant capabilities tool or backend capabilities path | false | BACKEND_DIRECT or LLM_COMPOSED | Explains TAMI capabilities using current tools/modules. |
| Crea una reservación para mañana. | ADMIN | guardrail/write blocker | none | false | BACKEND_DIRECT or NO_MATCH | Refuses write/action request. No RAG fallback. |
| Elimina este documento. | ADMIN | guardrail/write blocker | none | false | BACKEND_DIRECT or NO_MATCH | Refuses delete/write action. No RAG fallback. |
| Cambia mi rol a administrador. | ADMIN | guardrail/write blocker | none | false | BACKEND_DIRECT or NO_MATCH | Refuses privilege change. No RAG fallback. |
| Marca esta tarea como completada. | ADMIN | guardrail/write blocker | none | false | BACKEND_DIRECT or NO_MATCH | Refuses write/action request. No RAG fallback. |

### B. Current user/profile/organization

| Prompt | User | Expected handler | Expected tool | ragUsed | answerSource | Expected result |
| --- | --- | --- | --- | --- | --- | --- |
| ¿Cómo me llamo? | ADMIN | User/current access handler | current user/profile tool | false | BACKEND_DIRECT | Current authenticated user's name. |
| ¿Cuál es mi correo? | ADMIN | User/current access handler | current user/profile tool | false | BACKEND_DIRECT | Current authenticated user's email. |
| ¿Cuál es mi rol? | ADMIN | User/current access handler | current user/profile tool | false | BACKEND_DIRECT | Current authenticated user's role. |
| ¿Cuál es mi organización? | ADMIN | Organization/current access handler | current organization tool | false | BACKEND_DIRECT | Current organization summary. |

### C. Properties and catalogs

| Prompt | User | Expected handler | Expected tool | ragUsed | answerSource | Expected result |
| --- | --- | --- | --- | --- | --- | --- |
| ¿Qué propiedades tengo? | ADMIN | Property/catalog handler | property list/search tool | false | BACKEND_DIRECT | Lists properties in current organization. |
| Dame un resumen de Bungalow Tu Refugio Perfecto | ADMIN | Property/catalog handler | property summary tool | false | BACKEND_DIRECT | Property summary. |
| ¿Qué propiedades activas tengo? | ADMIN | Property/catalog handler | property list/search tool | false | BACKEND_DIRECT | Active properties only. |
| ¿Qué catálogos puedo usar para mantenimiento? | ADMIN | Property/catalog handler | maintenance catalogs tool | false | BACKEND_DIRECT | Maintenance catalog overview. |
| ¿Qué plataformas de reservación tengo? | ADMIN | Property/catalog handler | platforms tool | false | BACKEND_DIRECT | Reservation platforms. |
| ¿Qué tipos de inventory item existen? | ADMIN | Inventory/catalog handler | inventory type/catalog tool | false | BACKEND_DIRECT | Inventory item enum/type list. |

### D. Inventory, brands and usage

| Prompt | User | Expected handler | Expected tool | ragUsed | answerSource | Expected result |
| --- | --- | --- | --- | --- | --- | --- |
| ¿Qué productos tengo en inventario? | ADMIN | InventoryToolHandler | inventory.search | false | BACKEND_DIRECT | Lists inventory items with brand, type, unit, status and usable modules. |
| ¿Qué items tengo en inventario? | ADMIN | InventoryToolHandler | inventory.search | false | BACKEND_DIRECT | Lists inventory items with brand where available. |
| ¿Qué items tengo de la marca Pledge? | ADMIN | InventoryToolHandler | inventory.getItemsByBrand | false | BACKEND_DIRECT | Lists Pledge items, without image counts unless specifically asked. |
| ¿Qué productos tengo de la marca Pledge? | ADMIN | InventoryToolHandler | inventory.getItemsByBrand | false | BACKEND_DIRECT | Routes to inventory, not purchase analytics/RAG. |
| ¿Qué productos tengo por marca? | ADMIN | InventoryToolHandler | inventory.getItemsByBrand | false | BACKEND_DIRECT | Groups inventory items by brand. |
| ¿Qué items tengo por marca? | ADMIN | InventoryToolHandler | inventory.getItemsByBrand | false | BACKEND_DIRECT | Groups inventory items by brand. |
| ¿Cuáles son los productos más usados? | ADMIN | InventoryToolHandler | inventory.getFrequentlyUsed | false | BACKEND_DIRECT | Inventory usage ranking. |
| ¿Qué items se usan más? | ADMIN | InventoryToolHandler | inventory.getFrequentlyUsed | false | BACKEND_DIRECT | Inventory usage ranking. |
| ¿Dónde he usado covertor elástico? | ADMIN | InventoryToolHandler | inventory.whereUsed or inventory usage tools | false | BACKEND_DIRECT | Extracts item as `covertor elastico`, not `he covertor elastico`. |
| ¿Dónde se ha usado café? | ADMIN | InventoryToolHandler | inventory.whereUsed or inventory usage tools | false or true only if valid fallback | BACKEND_DIRECT or TOOLS_AND_RAG | Uses maintenance/reservation/purchase usage first. |

### E. Maintenance analytics

| Prompt | User | Expected handler | Expected tool | ragUsed | answerSource | Expected result |
| --- | --- | --- | --- | --- | --- | --- |
| ¿Qué items se usaron en mantenimientos? | ADMIN | Inventory/Maintenance handler | inventory.getItemsUsedInMaintenance | false | BACKEND_DIRECT | Maintenance item usage. |
| ¿Qué propiedad tiene más gastos de mantenimiento? | ADMIN | Maintenance analytics handler | maintenance cost ranking tool | false | BACKEND_DIRECT | Property ranked by maintenance cost. |
| ¿Cuánto gasté en mantenimiento por categoría? | ADMIN | Maintenance analytics handler | maintenance cost by category tool | false | BACKEND_DIRECT | Cost by category. |
| ¿Qué mantenimientos tienen imágenes? | ADMIN | File/image or maintenance handler | maintenance image metadata tool | false | BACKEND_DIRECT | Maintenance records with images. |

### F. Scheduled maintenance, reservations and guests

| Prompt | User | Expected handler | Expected tool | ragUsed | answerSource | Expected result |
| --- | --- | --- | --- | --- | --- | --- |
| ¿Qué mantenimientos programados están vencidos? | ADMIN | Scheduled maintenance handler | scheduled maintenance overdue tool | false | BACKEND_DIRECT | Overdue scheduled maintenance. |
| ¿Qué mantenimiento programado vence hoy? | ADMIN | Scheduled maintenance handler | scheduled maintenance due today tool | false | BACKEND_DIRECT | Due today scheduled maintenance. |
| ¿Cuál es el próximo mantenimiento programado? | ADMIN | Scheduled maintenance handler | next scheduled maintenance tool | false | BACKEND_DIRECT | Next scheduled maintenance. |
| ¿Cuál es la próxima entrada? | ADMIN | Reservation handler | next check-in tool | false | BACKEND_DIRECT | Next check-in. |
| ¿Cuál es la próxima salida? | ADMIN | Reservation handler | next check-out tool | false | BACKEND_DIRECT | Next check-out. |
| ¿Qué reservaciones tengo esta semana? | ADMIN | Reservation handler | reservations by date range tool | false | BACKEND_DIRECT | This week's reservations. |
| ¿Qué huéspedes regresan? | ADMIN | Guest/reservation handler | returning guests tool | false | BACKEND_DIRECT | Returning guests. |

### G. Reservation supplies and tasks

| Prompt | User | Expected handler | Expected tool | ragUsed | answerSource | Expected result |
| --- | --- | --- | --- | --- | --- | --- |
| ¿Qué supplies necesito para la próxima reserva? | ADMIN | Reservation supply handler | supplies for next reservation tool | false | BACKEND_DIRECT | Supplies for next/upcoming reservation. |
| ¿Qué supplies se usaron en la última reserva? | ADMIN | Reservation supply handler | supplies for latest reservation tool | false | BACKEND_DIRECT | Supplies from latest past reservation. |
| ¿Qué supplies se usan más? | ADMIN | Reservation supply handler | most used supplies tool | false | BACKEND_DIRECT | Reservation supply ranking. |
| ¿Qué reservaciones próximas no tienen supplies asignados? | ADMIN | Reservation supply handler | reservations without supplies tool | false | BACKEND_DIRECT | Upcoming reservations without supplies. |
| ¿Qué tareas tengo pendientes? | ADMIN | Task handler | pending task tool | false | BACKEND_DIRECT | Pending/active task lists. |
| ¿Qué tareas hay para la próxima reservación? | ADMIN | Task handler | reservation task list tool | false | BACKEND_DIRECT | Tasks for next reservation. |
| ¿Qué tareas ya se completaron? | ADMIN | Task handler | completed tasks tool | false | BACKEND_DIRECT | Completed tasks. |
| ¿Qué tareas están asignadas por persona? | ADMIN | Task handler | assigned tasks summary tool | false | BACKEND_DIRECT | Assigned task summary. |

### H. Purchases

| Prompt | User | Expected handler | Expected tool | ragUsed | answerSource | Expected result |
| --- | --- | --- | --- | --- | --- | --- |
| ¿Cuándo compré por última vez cloro? | ADMIN | Purchase handler | last purchased item tool | false | BACKEND_DIRECT | Last purchased cloro item. |
| ¿Cuánto cuesta normalmente el papel higiénico? | ADMIN | Purchase handler | average unit cost/price history tool | false | BACKEND_DIRECT | Average price history. |
| ¿Qué item compro más seguido? | ADMIN | Purchase handler | most purchased item tool | false | BACKEND_DIRECT | Most purchased item by quantity/frequency. |
| ¿Qué item compro menos seguido? | ADMIN | Purchase handler | least purchased item tool | false | BACKEND_DIRECT | Least purchased item by quantity/frequency. |
| ¿Cuánto gasté en compras por mes? | ADMIN | Purchase handler | purchase cost by month tool | false | BACKEND_DIRECT | Purchase cost by month. |
| ¿Cuánto gasté en compras por propiedad? | ADMIN | Purchase handler | purchase cost by property tool | false | BACKEND_DIRECT | Purchase cost by property. |

### I. Documents, metadata and RAG health

| Prompt | User | Expected handler | Expected tool | ragUsed | answerSource | Expected result |
| --- | --- | --- | --- | --- | --- | --- |
| ¿Qué documentos tengo cargados? | ADMIN | Document metadata handler | document metadata tool | false | BACKEND_DIRECT | Document metadata list. |
| ¿Qué documentos tengo por tipo? | ADMIN | Document metadata handler | documents by type tool | false | BACKEND_DIRECT | Documents grouped by type. |
| ¿Qué documentos tengo por propiedad? | ADMIN | Document metadata handler | documents by property tool | false | BACKEND_DIRECT | Documents grouped by property. |
| ¿Qué documentos están procesados? | ADMIN | Document metadata handler | processed documents tool | false | BACKEND_DIRECT | Processed documents. |
| ¿Qué documentos están procesados pero no indexados para IA? | ADMIN | Document metadata handler | processed not indexed tool | false | BACKEND_DIRECT | Processed but not indexed. |
| ¿Qué documentos fallaron al procesarse? | ADMIN | Document metadata handler | failed documents tool | false | BACKEND_DIRECT | Failed documents. |
| ¿Cómo está el índice RAG de mis documentos? | ADMIN | RAG health handler | rag health/index status tool | false | BACKEND_DIRECT | RAG index status. |
| ¿Qué documentos están listos para IA? | ADMIN | Document metadata handler | AI-ready documents tool | false | BACKEND_DIRECT | Processed + indexed documents. |

### J. Entity image tools from 12E

| Prompt | User | Expected handler | Expected tool | ragUsed | answerSource | Expected result |
| --- | --- | --- | --- | --- | --- | --- |
| ¿Qué reservaciones tienen imágenes? | ADMIN | EntityImageToolHandler | images.getReservationImages | false | BACKEND_DIRECT | Lists reservations with images; filenames as subitems. |
| ¿Qué reservaciones no tienen fotos? | ADMIN | EntityImageToolHandler | images.getReservationImages | false | BACKEND_DIRECT | Lists all reservations without images; no `imágenes: 0` noise. |
| ¿Qué items tienen imágenes? | ADMIN | EntityImageToolHandler | images.getInventoryItemImages | false | BACKEND_DIRECT | Lists inventory items with images; filenames as subitems. |
| ¿Qué items no tienen imágenes? | ADMIN | EntityImageToolHandler | images.getInventoryItemImages | false | BACKEND_DIRECT | Lists all inventory items without images; does not filter by token `no`. |
| ¿Qué listas de compra tienen imágenes? | ADMIN | EntityImageToolHandler | images.getPurchaseListImages | false | BACKEND_DIRECT | Lists purchase lists with images; filenames as subitems. |
| ¿Qué compras no tienen fotos? | ADMIN | EntityImageToolHandler | images.getPurchaseListImages | false | BACKEND_DIRECT | Lists all purchase lists without images. |

### K. Cross-module file/image dashboard tools from 13A

| Prompt | User | Expected handler | Expected tool | ragUsed | answerSource | Expected result |
| --- | --- | --- | --- | --- | --- | --- |
| ¿Cuántas imágenes tengo en TAMIAS? | ADMIN | FileImageDashboardToolHandler | files.getImageDashboardSummary | false | BACKEND_DIRECT | Total image count and per-module summary. |
| ¿Cuántas imágenes tengo por módulo? | ADMIN | FileImageDashboardToolHandler | files.getImageDashboardSummary | false | BACKEND_DIRECT | Counts grouped by module. |
| ¿Qué módulo tiene más imágenes? | ADMIN | FileImageDashboardToolHandler | files.getImageDashboardSummary | false | BACKEND_DIRECT | Names the actual module with most images, not the answer title. |
| ¿Qué entidades tienen más imágenes? | ADMIN | FileImageDashboardToolHandler | files.getEntitiesWithMostImages | false | BACKEND_DIRECT | Groups results by module, then lists entities. |
| ¿Qué entidades no tienen imágenes? | ADMIN | FileImageDashboardToolHandler | files.getEntitiesWithoutImages | false | BACKEND_DIRECT | Groups entities without images by module. |
| ¿Qué imágenes se subieron recientemente? | ADMIN | FileImageDashboardToolHandler | files.getRecentUploads | false | BACKEND_DIRECT | Groups by entity/origin/property and filenames as subitems. |
| ¿Qué documentos o imágenes fueron subidos recientemente? | ADMIN | FileImageDashboardToolHandler | files.getRecentUploads | false | BACKEND_DIRECT | Includes documents + images, grouped by entity. |
| ¿Qué archivos ocupan más espacio? | ADMIN | FileImageDashboardToolHandler | files.getLargestFiles | false | BACKEND_DIRECT | Largest files ordered by `size_bytes`. |
| ¿Cuánto storage tengo registrado por módulo? | ADMIN | FileImageDashboardToolHandler | file.storageSummary or files.getImageDashboardSummary | false | BACKEND_DIRECT | Storage summary by module. |
| ¿Qué archivos tengo en TAMIAS? | ADMIN | FileImageDashboardToolHandler | file.searchMetadata | false | BACKEND_DIRECT | File metadata across documents/images. |

### L. Admin-only tools and permission denials

Run the first group as administrator.

| Prompt | User | Expected handler | Expected tool | ragUsed | answerSource | Expected result |
| --- | --- | --- | --- | --- | --- | --- |
| ¿Qué usuarios activos tengo? | ADMIN | Admin/user handler | active users tool | false | BACKEND_DIRECT | Active users in organization. |
| ¿Qué usuarios inactivos tengo? | ADMIN | Admin/user handler | inactive users tool | false | BACKEND_DIRECT | Inactive users. |
| ¿Qué usuarios tienen rol Maintenance Staff? | ADMIN | Admin/user handler | users by role tool | false | BACKEND_DIRECT | Users with role. |
| ¿Qué accesos tengo? | ADMIN | Access/current user handler | current access summary tool | false | BACKEND_DIRECT | Current authenticated user's access summary. |
| Dame el resumen de accesos de todos los usuarios. | ADMIN | Admin/access handler | organization access summary tool | false | BACKEND_DIRECT | Organization-wide access summary. |
| ¿Qué roles existen? | ADMIN | Admin/role handler | role list tool | false | BACKEND_DIRECT | Role list. |
| ¿Qué permisos tiene Maintenance Staff? | ADMIN | Admin/role handler | role permission summary tool | false | BACKEND_DIRECT | Maintenance Staff permissions. |
| ¿Cuántos usuarios tiene mi organización? | ADMIN | Admin/org handler | organization user count tool | false | BACKEND_DIRECT | User count. |

Run this group as non-admin.

| Prompt | User | Expected handler | Expected tool | ragUsed | answerSource | Expected result |
| --- | --- | --- | --- | --- | --- | --- |
| ¿Qué usuarios activos tengo? | NON_ADMIN | Admin/user handler | denied | false | BACKEND_DIRECT or NO_MATCH | Admin-only denial. No RAG fallback. |
| ¿Qué permisos tiene Administrator? | NON_ADMIN | Admin/role handler | denied | false | BACKEND_DIRECT or NO_MATCH | Admin-only denial. No RAG fallback. |
| ¿Cuántos usuarios tiene mi organización? | NON_ADMIN | Admin/org handler | denied | false | BACKEND_DIRECT or NO_MATCH | Admin-only denial. No RAG fallback. |

### M. AI chat history

| Prompt | User | Expected handler | Expected tool | ragUsed | answerSource | Expected result |
| --- | --- | --- | --- | --- | --- | --- |
| Muéstrame mis últimas conversaciones con la IA | ADMIN | Chat history handler | ai chat history tool | false | BACKEND_DIRECT | Previous sessions, excluding current session where expected. |
| ¿Qué hemos hablado antes? | ADMIN | Chat history handler | ai chat history tool | false | BACKEND_DIRECT | Previous chat history. |
| Busca en el historial si hablamos de cloro | ADMIN | Chat history handler | chat history search tool | false | BACKEND_DIRECT | Finds own chat history only. |
| ¿Qué preguntas le hice al asistente? | ADMIN | Chat history handler | user questions history tool | false | BACKEND_DIRECT | User questions. |
| Resume esta conversación | ADMIN | Chat history handler | current session summary tool | false | BACKEND_DIRECT or LLM_COMPOSED | Summary of current session. |
| ¿Cuántas sesiones de chat IA tengo? | ADMIN | Chat history handler | chat session count tool | false | BACKEND_DIRECT | Session count for current user. |

### N. RAG-only document content

These prompts must use indexed document chunks and show document sources when relevant content exists.

| Prompt | User | Expected handler | Expected tool | ragUsed | answerSource | Expected result |
| --- | --- | --- | --- | --- | --- | --- |
| ¿Qué dice el PDF sobre visitantes? | ADMIN | RAG path | rag.search | true | RAG | Answer from document content with sources. |
| ¿Qué reglas hay sobre basura? | ADMIN | RAG path | rag.search | true | RAG | Answer from indexed document content with sources. |
| ¿Qué menciona el manual sobre filtros? | ADMIN | RAG path | rag.search | true | RAG | Answer from manual/document content with sources. |
| ¿Qué dice el documento sobre mascotas? | ADMIN | RAG path | rag.search | true | RAG | Answer with sources if content exists; otherwise clear no-information message. |
| ¿Qué dice el plano sobre medidas del baño? | ADMIN | RAG path | rag.search | true | RAG | Answer from indexed blueprint/plan text if available. |

### O. Tool/RAG fallback and mixed answers

| Prompt | User | Expected handler | Expected tool | ragUsed | answerSource | Expected result |
| --- | --- | --- | --- | --- | --- | --- |
| ¿Qué documentos tengo sobre reglas y qué dicen? | ADMIN | Document metadata + RAG | document metadata tool + rag.search | true | TOOLS_AND_RAG | Combines metadata and document content. |
| Según mis documentos y datos del sistema, ¿qué debo revisar antes de la próxima reserva? | ADMIN | Mixed orchestration | reservation/dashboard tools + rag.search | true | TOOLS_AND_RAG | Combines system data and document rules. |
| ¿Qué reglas aplican a Bungalow Tu Refugio Perfecto? | ADMIN | RAG first or mixed | property context tool + rag.search | true | RAG or TOOLS_AND_RAG | Uses property context and rules from documents. |
| ¿Qué dice el documento sobre una regla que no existe? | ADMIN | RAG path | rag.search | true | RAG or NO_MATCH | Unified no-information message. |
| Busca en mis registros y documentos algo sobre un tema inexistente. | ADMIN | Mixed path | relevant tools + rag.search | true or false depending planner | TOOLS_AND_RAG or NO_MATCH | Unified no-information message after checked paths. |

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

## Regression watchlist

Pay special attention to these historical failures:

- `productos` routed to purchases when the user means inventory.
- Inventory item extraction keeps garbage tokens such as `he`, `no`, `tengo`, `sobre`.
- `no tienen imágenes` accidentally searches for token `no`.
- Tool empty result blocks valid RAG fallback.
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

## Hardening workflow

1. Run `./mvnw test`.
2. Run `npm run build` for safety, even when backend-only changes are expected.
3. Disable `ai_chat_debug` and run representative prompts.
4. Enable `ai_chat_debug` and run the same prompts.
5. Record failures using the result format above.
6. Fix only the failing handlers/services/repositories.
7. Re-run the failed prompts.
8. Re-run the minimal smoke set.
9. Update this document with any permanent regression tests or known limitations.

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

9P-H can be marked completed when:

- The full matrix has been run at least once as `ADMINISTRATOR`.
- Admin-only denial prompts have been run at least once as non-admin.
- Every assistant response persists exactly one debug row.
- Debug is hidden when `ai_chat_debug=false`.
- Debug is visible when `ai_chat_debug=true`.
- Tool-only prompts use tools and avoid unnecessary RAG.
- RAG-only prompts answer from document sources when indexed content exists.
- Mixed prompts either combine evidence correctly or clearly state what was missing.
- Guardrail prompts never execute writes and never fall back to RAG.
- No SQL, regex, routing, parameter extraction or answer-format regressions remain open.
- Any remaining issues are documented as known limitations or moved to `9P-I` if they are specifically RAG retrieval problems.

## After 9P-H

Next phase:

```text
9P-I — RAG retrieval tuning, only if needed
```

Only start `9P-I` if this matrix proves that document retrieval quality needs tuning. Do not tune RAG merely because a structured tool question failed; fix routing/tool extraction first.
