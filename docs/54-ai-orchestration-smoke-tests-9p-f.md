# 9P-F — AI Orchestration Smoke Tests

## Purpose

This phase validates the current TAMIAS AI orchestration after the 9P refactor series and the LLM-driven planning layer.

It does not introduce new business tools. It documents how to prove that the assistant correctly routes questions through:

- Read-only system tools
- RAG/document search
- Tool -> RAG fallback
- RAG -> tool fallback when allowed by the plan
- Combined tool + RAG answers
- Guardrails
- Admin-only permission denials

## Current architecture under test

Current flow:

```text
AiController
  -> AiRagService.chat(...)
      -> persist USER message
      -> AiToolCallingService.tryHandleResult(...)
      -> respect GUARDRAIL / DENIED immediately
      -> AiPlanningService.plan(...)
      -> execute Tool/RAG path selected by backend-validated plan
      -> AiAnswerCompositionService may rewrite tool HIT answers naturally
      -> persist ASSISTANT message
```

Key rule: the LLM planner proposes a route, but backend code still owns permission checks, user scope, organization scope, query execution, and guardrails.

## Test groups

Use `docs/47-ai-orchestration-smoke-test-plan.md` as the full test checklist.

The groups are:

1. General assistant and guardrails
2. Current profile and organization
3. Properties and catalogs
4. Inventory and maintenance analytics
5. Scheduled maintenance, reservations and guests
6. Reservation supplies and tasks
7. Purchases
8. Document metadata and RAG health
9. File, image and dashboard tools
10. Admin-only user, role and organization tools
11. AI chat history
12. RAG-only document content
13. Tool/RAG fallback scenarios

## Minimal smoke test before deeper QA

Run these first after every AI orchestration change:

```text
¿Qué puedes hacer?
¿Qué usuarios activos tengo?
¿Qué permisos tiene Maintenance Staff?
¿Qué accesos tengo?
¿Qué propiedades tengo?
¿Qué alertas operativas tengo?
¿Qué dice el PDF sobre visitantes?
¿Qué reglas hay sobre basura?
Según mis documentos y datos del sistema, ¿qué debo revisar antes de la próxima reserva?
Crea una reservación para mañana.
```

Expected behavior:

- User/role/property/dashboard questions should use tools.
- PDF/rules/manual questions should use RAG and show document sources when content exists.
- Mixed questions may use both tools and RAG.
- Write requests must be blocked.

## What to inspect in each response

For each response, inspect:

```text
answer
sourceCount
sources[]
toolEvidence[]
grounded
```

Interpretation:

- `sources[]` present means RAG/document content was used.
- `toolEvidence[]` present means a system tool was used or consulted.
- A mixed answer may contain both.
- Guardrail/denied responses should not contain document sources.

## Planner sanity checks

The LLM planner should generally route:

```text
PDF/document/manual/rules/plans -> RAG_ONLY or RAG_FIRST
users/roles/dashboard/counts -> TOOL_ONLY or TOOL_FIRST
mixed system + documents -> TOOL_AND_RAG
write actions -> DENY_WRITE/GUARDRAIL
ambiguous prompts -> CLARIFY or deterministic safe fallback
```

If a prompt is consistently routed incorrectly, adjust `AiPlanningService` prompt or heuristic fallback before changing repositories or SQL.

## Regression watchlist

Pay attention to these historical failure patterns:

- Admin permission prompt routed to current user access summary.
- Current session included in previous chat history.
- RAG question trapped by document metadata tool.
- Tool empty answer blocking RAG.
- Regex-related `PatternSyntaxException` or `StackOverflowError`.
- Native SQL optional parameters causing PostgreSQL type errors.
- SQL string concatenation missing spaces.
- Search extractor leaving garbage tokens such as `tiene`, `todos`, `sobre`.

## Completion criteria

9P-F is complete when:

- The full smoke test checklist has been run at least once as admin.
- Admin-only denial tests have been run at least once as non-admin.
- RAG-only document questions answer from indexed PDFs with sources.
- Tool/RAG mixed questions either combine evidence or clearly state what was missing.
- Results are documented with failures and follow-up fixes.
