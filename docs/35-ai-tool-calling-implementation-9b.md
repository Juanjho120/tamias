# TAMIAS — AI Tool Calling Implementation 9B–9D

## Scope

This document summarizes the implemented AI Tool Calling work from phases 9B, 9C and 9D.

The broader catalog is tracked in:

```text
docs/36-ai-tool-calling-expanded-catalog-coverage.md
```

## Current implementation status

The current implementation is not the full expanded catalog. It is the first stable read-only tool foundation plus assistant integration and frontend evidence UX.

## Implemented backend tools

```text
assistant.capabilities
assistant.readOnlyGuard
assistant.operationalPreparation
assistant.operationalPlanning
assistant.documentOverview
assistant.propertyOperations
user.currentProfile
organization.currentSummary
property.search
dashboard.operationalSummary
reservation.upcoming
maintenance.lastPerformed
scheduledMaintenance.overdue
purchaseItem.lastPurchased
taskList.pending
document.searchMetadata
rag.documentIndexStatus
```

## Phase 9B — Backend read-only AI tools

Implemented controlled backend tools for safe operational queries.

Security model:

```text
[✓] Read-only only
[✓] No free SQL from the model
[✓] No autonomous writes
[✓] organization_id resolved from CurrentUserService
[✓] user_id resolved from CurrentUserService
[✓] Native queries are backend-owned
[✓] Results are scoped to the authenticated organization
[✓] Passwords, tokens and secrets are not returned
```

## Phase 9C — AI Assistant integration

Implemented assistant-level orchestration on top of the read-only tools.

Current assistant-level combined answers:

```text
assistant.operationalPreparation
assistant.operationalPlanning
assistant.documentOverview
assistant.propertyOperations
```

The assistant can now combine existing tools for operational planning questions while preserving the normal RAG fallback for document-content questions.

## Phase 9D — Frontend UX for system data responses

The frontend now supports tool evidence from backend responses.

User-visible behavior:

```text
[✓] Operational tool answers are visually distinguished from RAG-only answers.
[✓] Tool evidence can be shown as structured cards.
[✓] Document sources still work for RAG answers.
[✓] Ctrl + Enter / Cmd + Enter sends the prompt.
```

## Known limitation

This implementation still uses deterministic routing with keyword and synonym groups.

It does not implement the entire expanded catalog yet and does not use model-native function calling.

That is intentional for the current phase because the system is still validating backend-owned, read-only behavior before expanding the number of tools.

## Smoke test questions

Use the AI Assistant and ask:

```text
¿Qué puedes hacer?
¿Cómo me llamo?
¿Cuál es mi correo?
¿A qué organización pertenezco?
Dame un resumen operativo.
¿Qué reservas tengo esta semana?
¿Cuándo fue el último mantenimiento del filtro de agua?
¿Qué mantenimientos están vencidos?
¿Cuándo compré café por última vez?
¿Qué tareas tengo pendientes?
¿Qué documentos tengo procesados?
¿Qué documentos tienen chunks sin vector_store_id?
¿Tengo algo pendiente para preparar la casa antes de la próxima reserva?
¿Qué documentos tengo cargados y cómo está el índice RAG?
Crea una reservación para mañana.
```

Expected behavior:

```text
- Capability/profile/organization questions do not go to RAG.
- Operational questions return database-backed answers.
- Combined operational questions can call multiple read-only tools.
- Write requests are rejected by the read-only guard.
- Document content questions still use RAG.
- Frontend displays operational evidence when toolEvidence is present.
```

## Next catalog expansion

The next recommended phase is:

```text
9G — Property and catalog read-only tools
```

This should add the first expanded-catalog tools without turning the current service into an unmaintainable mega-class.
