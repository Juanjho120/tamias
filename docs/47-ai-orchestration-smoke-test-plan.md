# 47 — AI Orchestration Smoke Test Plan

This checklist validates TAMIAS AI orchestration after the 9P refactor series:

- 9P-A — orchestration foundation
- 9P-B — handler split
- 9P-C — read-only domain services
- 9P-C.1 — package reorganization
- 9P-C.2 — repository split
- 9P-D — Tool/RAG fallback
- 9P-E — LLM-driven planning
- 9P-F — smoke tests and RAG diagnostics

The goal is to confirm that the assistant chooses the correct route between system tools, RAG/document search, combined answers, guardrails, and permission denials.

## Test setup

Use at least two users:

1. `ADMINISTRATOR`
2. Non-admin user, preferably `MAINTENANCE_STAFF` or `READ_ONLY`

Use at least one property with:

- Uploaded documents
- Processed documents
- Indexed chunks
- At least one active reservation
- At least one maintenance record
- At least one scheduled maintenance record
- At least one task list
- At least one purchase list

For RAG tests, use PDF/documents that contain known phrases such as house rules, visitor rules, trash instructions, filter instructions, plumbing/electrical details, or maintenance rules.

## How to record results

For every prompt, capture:

```text
Prompt:
Expected route: TOOL_ONLY | RAG_ONLY | TOOL_FIRST | RAG_FIRST | TOOL_AND_RAG | GUARDRAIL | DENIED
Actual route observed:
Expected result:
Actual result:
Sources shown? yes/no
Tool evidence shown? yes/no
Pass? yes/no
Notes:
```

## A. General assistant and guardrails

| Prompt | Expected route | Expected result |
|---|---:|---|
| ¿Qué puedes hacer? | TOOL_ONLY | Assistant capabilities response. |
| Crea una reservación para mañana. | GUARDRAIL | Read-only denial. No RAG fallback. |
| Elimina este documento. | GUARDRAIL | Read-only denial. No RAG fallback. |
| Cambia mi rol a administrador. | GUARDRAIL | Read-only denial. No RAG fallback. |
| Marca esta tarea como completada. | GUARDRAIL | Read-only denial. No RAG fallback. |

## B. Current profile and organization

| Prompt | Expected route | Expected result |
|---|---:|---|
| ¿Cómo me llamo? | TOOL_ONLY | Current authenticated user name. |
| ¿Cuál es mi correo? | TOOL_ONLY | Current authenticated user email. |
| ¿Cuál es mi rol? | TOOL_ONLY | Current authenticated user role. |
| ¿Cuál es mi organización? | TOOL_ONLY | Current organization summary. |

## C. Properties and catalogs

| Prompt | Expected route | Expected result |
|---|---:|---|
| ¿Qué propiedades tengo? | TOOL_ONLY | Property list. |
| Dame un resumen de Bungalow Tu Refugio Perfecto | TOOL_ONLY | Property summary. |
| ¿Qué propiedades activas tengo? | TOOL_ONLY | Active properties. |
| ¿Qué propiedades inactivas tengo? | TOOL_ONLY | Inactive properties or empty result. |
| ¿Qué catálogos puedo usar para mantenimiento? | TOOL_ONLY | Maintenance catalog overview. |
| ¿Qué plataformas de reservación tengo? | TOOL_ONLY | Reservation platforms. |
| ¿Qué tipos de inventory item existen? | TOOL_ONLY | Inventory item enum/type list. |

## D. Inventory and maintenance analytics

| Prompt | Expected route | Expected result |
|---|---:|---|
| ¿Qué items se usan más? | TOOL_ONLY | Inventory usage ranking. |
| ¿Qué supplies se usan más en reservaciones? | TOOL_ONLY | Reservation supply ranking. |
| ¿Dónde se ha usado el café? | TOOL_FIRST | Inventory usage result or fallback if empty. |
| ¿Qué items se usaron en mantenimientos? | TOOL_ONLY | Maintenance item usage. |
| ¿Qué propiedad tiene más gastos de mantenimiento? | TOOL_ONLY | Maintenance cost by property. |
| ¿Cuánto gasté en mantenimiento por categoría? | TOOL_ONLY | Maintenance cost by category. |

## E. Scheduled maintenance, reservations and guests

| Prompt | Expected route | Expected result |
|---|---:|---|
| ¿Qué mantenimientos programados están vencidos? | TOOL_ONLY | Overdue scheduled maintenance. |
| ¿Qué mantenimiento programado vence hoy? | TOOL_ONLY | Due today scheduled maintenance. |
| ¿Cuál es el próximo mantenimiento programado? | TOOL_ONLY | Next scheduled maintenance. |
| ¿Cuál es la próxima entrada? | TOOL_ONLY | Next check-in. |
| ¿Cuál es la próxima salida? | TOOL_ONLY | Next check-out. |
| ¿Qué reservaciones tengo esta semana? | TOOL_ONLY | This week's reservations. |
| ¿Qué huéspedes regresan? | TOOL_ONLY | Returning guests. |

## F. Reservation supplies and tasks

| Prompt | Expected route | Expected result |
|---|---:|---|
| ¿Qué supplies necesito para la próxima reserva? | TOOL_ONLY | Supplies for next/upcoming reservation. |
| ¿Qué supplies se usaron en la última reserva? | TOOL_ONLY | Supplies for latest past reservation. |
| ¿Qué supplies se usan más? | TOOL_ONLY | Most-used reservation supplies. |
| ¿Qué reservaciones próximas no tienen supplies asignados? | TOOL_ONLY | Upcoming reservations without supplies. |
| ¿Qué tareas tengo pendientes? | TOOL_ONLY | Pending/active task lists. |
| ¿Qué tareas hay para la próxima reservación? | TOOL_ONLY | Task lists for next reservation. |
| ¿Qué tareas ya se completaron? | TOOL_ONLY | Completed task items/lists. |
| ¿Qué tareas están asignadas por persona? | TOOL_ONLY | Assigned task summary. |

## G. Purchases

| Prompt | Expected route | Expected result |
|---|---:|---|
| ¿Cuándo compré por última vez cloro? | TOOL_ONLY | Last purchased cloro item. |
| ¿Cuánto cuesta normalmente el papel higiénico? | TOOL_ONLY | Average unit cost or price history. |
| ¿Qué item compro más seguido? | TOOL_ONLY | Most purchased item by quantity. |
| ¿Qué item compro menos seguido? | TOOL_ONLY | Least purchased item by quantity. |
| ¿Cuánto gasté en compras por mes? | TOOL_ONLY | Purchase cost by month. |
| ¿Cuánto gasté en compras por propiedad? | TOOL_ONLY | Purchase cost by property. |

## H. Document metadata and RAG health

| Prompt | Expected route | Expected result |
|---|---:|---|
| ¿Qué documentos tengo cargados? | TOOL_ONLY | Document metadata list. |
| ¿Qué documentos tengo por tipo? | TOOL_ONLY | Documents grouped by type. |
| ¿Qué documentos tengo por propiedad? | TOOL_ONLY | Documents grouped by property. |
| ¿Qué documentos están procesados? | TOOL_ONLY | Processed documents. |
| ¿Qué documentos están procesados pero no indexados para IA? | TOOL_ONLY | Processed but not indexed. |
| ¿Qué documentos fallaron al procesarse? | TOOL_ONLY | Failed documents. |
| ¿Cómo está el índice RAG de mis documentos? | TOOL_ONLY | RAG health/index status. |
| ¿Qué documentos están listos para IA? | TOOL_ONLY | Processed + indexed documents. |

## I. File, image and dashboard tools

| Prompt | Expected route | Expected result |
|---|---:|---|
| ¿Qué archivos asociados a documentos tengo? | TOOL_ONLY | Files linked to documents. |
| ¿Qué archivos asociados a mantenimientos tengo? | TOOL_ONLY | Files/images linked to maintenance. |
| ¿Qué mantenimientos tienen imágenes? | TOOL_ONLY | Maintenance records with images only. |
| ¿Qué alertas operativas tengo? | TOOL_ONLY | Compact alert counters. |
| ¿Qué necesita atención hoy? | TOOL_ONLY | Detailed action list. |
| Dame un resumen de compras del dashboard | TOOL_ONLY | Dashboard purchase summary. |
| Dame un resumen de tareas del dashboard | TOOL_ONLY | Dashboard task summary. |

## J. Admin-only user, role and organization tools

Run these as an administrator.

| Prompt | Expected route | Expected result |
|---|---:|---|
| ¿Qué usuarios activos tengo? | TOOL_ONLY | Active users. |
| ¿Qué usuarios inactivos tengo? | TOOL_ONLY | Inactive/non-active users. |
| ¿Qué usuarios tienen rol Maintenance Staff? | TOOL_ONLY | Users with MAINTENANCE_STAFF. |
| ¿Qué accesos tengo? | TOOL_ONLY | Current authenticated user's access summary. |
| ¿Qué accesos tiene este usuario? | TOOL_ONLY | Current authenticated user's access summary unless a specific user is named. |
| Dame el resumen de accesos de todos los usuarios. | TOOL_ONLY | Organization-wide access summary. |
| ¿Qué roles existen? | TOOL_ONLY | Role list. |
| ¿Qué permisos tiene Maintenance Staff? | TOOL_ONLY | Role permission summary. |
| ¿Qué permisos tiene Administrator? | TOOL_ONLY | Role permission summary. |
| ¿Cuántos usuarios tiene mi organización? | TOOL_ONLY | Organization user count. |
| ¿Qué módulos estamos usando más? | TOOL_ONLY | Module usage summary. |

Run these as a non-admin user.

| Prompt | Expected route | Expected result |
|---|---:|---|
| ¿Qué usuarios activos tengo? | DENIED | Admin-only denial. No RAG fallback. |
| ¿Qué permisos tiene Administrator? | DENIED | Admin-only denial. No RAG fallback. |
| ¿Cuántos usuarios tiene mi organización? | DENIED | Admin-only denial. No RAG fallback. |

## K. AI chat history

| Prompt | Expected route | Expected result |
|---|---:|---|
| Muéstrame mis últimas conversaciones con la IA | TOOL_ONLY | Recent previous sessions, excluding current session. |
| ¿Qué hemos hablado antes? | TOOL_ONLY | Previous chat history, excluding current session. |
| Busca en el historial si hablamos de cloro | TOOL_ONLY | Search chat history, excluding current session. |
| ¿Qué preguntas le hice al asistente? | TOOL_ONLY | User questions from previous history or current-session context depending phrasing. |
| Resume esta conversación | TOOL_ONLY | Current session summary. |
| ¿Cuántas sesiones de chat IA tengo? | TOOL_ONLY | Chat session count. |
| ¿Qué chats tengo sobre Bungalow Tu Refugio Perfecto? | TOOL_ONLY | Sessions related to that property. |

## L. RAG-only document content

These must use indexed document chunks and answer with document sources `[S1]`, `[S2]` when matches exist.

| Prompt | Expected route | Expected result |
|---|---:|---|
| ¿Qué dice el PDF sobre visitantes? | RAG_ONLY or RAG_FIRST | Answer from document content with sources. |
| ¿Qué reglas hay sobre basura? | RAG_ONLY or RAG_FIRST | Answer from document content with sources. |
| ¿Qué menciona el manual sobre filtros? | RAG_ONLY or RAG_FIRST | Answer from document content with sources. |
| ¿Qué dice el documento sobre mascotas? | RAG_ONLY or RAG_FIRST | Answer from document content with sources, if present. |
| ¿Qué dice el plano sobre medidas del baño? | RAG_ONLY or RAG_FIRST | Answer from blueprint/plan content, if present. |

## M. Tool/RAG fallback scenarios

| Prompt | Expected route | Expected result |
|---|---:|---|
| ¿Qué documentos tengo sobre reglas y qué dicen? | TOOL_AND_RAG | Metadata + document content if documents exist. |
| Según mis documentos y datos del sistema, ¿qué debo revisar antes de la próxima reserva? | TOOL_AND_RAG | Combines upcoming reservation/tool data with document rules if available. |
| ¿Qué reglas aplican a Bungalow Tu Refugio Perfecto? | RAG_FIRST or TOOL_AND_RAG | Property context + rules from RAG if available. |
| ¿Qué dice el documento sobre una regla que no existe? | RAG_ONLY | Unified no-information message. |
| Busca en mis registros y documentos algo sobre un tema inexistente. | TOOL_AND_RAG | Unified no-information message after both paths fail. |

## Pass criteria

The assistant passes 9P-F when:

- Tool-only prompts return structured data without unnecessary RAG.
- RAG-only prompts return document answers with sources when indexed content exists.
- Empty tools can fall back to RAG when the prompt can be answered from documents.
- Guardrails and admin denials never fall back to RAG.
- Mixed prompts can combine structured data and document content.
- Final not-found responses clearly say what was checked.
- No `StackOverflowError`, regex `PatternSyntaxException`, SQL parameter errors, or accidental writes occur.
