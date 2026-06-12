# TAMIAS — AI Tool Calling Implementation 9B

## Scope

This phase adds the first backend implementation of controlled read-only AI tools.

No frontend changes are required in this phase.

## Implemented tools

```text
assistant.capabilities
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
assistant.readOnlyGuard
```

## Security model

```text
[✓] Read-only only
[✓] No free SQL from the model
[✓] No autonomous writes
[✓] organization_id resolved from CurrentUserService
[✓] user_id resolved from CurrentUserService
[✓] Native queries are backend-owned
[✓] Results are scoped to the authenticated organization
[✓] Passwords/tokens/secrets are not returned
```

## Main backend changes

```text
AiChatResponse now supports toolEvidence.
AiRagService checks read-only tools before running RAG.
AiToolCallingService routes natural-language questions to known tools.
AiReadOnlyToolService executes backend-owned read-only queries.
RAG fallback no longer exposes similarityThreshold to end users.
```

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
Crea una reservación para mañana.
```

Expected behavior:

```text
Capability/profile/organization questions do not go to RAG.
Operational questions return database-backed answers.
Write requests are rejected by the read-only guard.
Document content questions still use RAG.
```

## Known limitation

This phase uses deterministic routing with keyword/synonym groups.

It does not yet use model-native function calling.

That is intentional for the first backend implementation because it is easier to validate and safer before giving the model tool-selection power.

## Next phase

```text
Fase 9C — Integrate tools with AI Assistant using richer prompts and optional model-assisted tool selection.
```
