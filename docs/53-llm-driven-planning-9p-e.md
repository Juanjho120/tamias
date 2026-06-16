# 9P-E — LLM-driven planning

## Objective

Introduce an LLM planning layer into TAMIAS AI orchestration while keeping backend-controlled execution and existing safety boundaries.

Before this phase, TAMIAS used deterministic Java routing first. The router selected a read-only tool using handlers, and `AiRagService` decided whether to answer with the tool or fall back to RAG based on `AiToolResult`.

This phase adds an LLM planner so the model can help decide whether a question should use:

- Structured system data/tools.
- Document search/RAG.
- Tool first, then RAG fallback.
- RAG first, then tool fallback.
- Tool + RAG together.
- A read-only guard for write-like requests.
- A clarification response when the path is too ambiguous.

The backend still owns execution. The LLM does not receive authority to run arbitrary SQL, bypass permissions, or choose organization/user scope.

## New package

```text
com.tamias.ai.planning
  AiPlanDecisionType.java
  AiExecutionPlan.java
  AiPlanResponse.java
  AiPlanningService.java
  AiAnswerCompositionService.java
```

## Planning decisions

```text
TOOL_FIRST
RAG_FIRST
TOOL_ONLY
RAG_ONLY
TOOL_AND_RAG
CLARIFY
DENY_WRITE
```

## Runtime flow

```text
User question
  ↓
AiRagService persists USER message
  ↓
Deterministic tool pre-check runs
  ↓
GUARDRAIL / DENIED results are respected immediately
  ↓
AiPlanningService asks the LLM for an execution plan
  ↓
Backend validates the plan and decides execution path
  ↓
Tools and/or RAG execute in backend-controlled code
  ↓
AiAnswerCompositionService can use the LLM to rewrite tool HIT answers naturally
  ↓
AiRagService persists ASSISTANT response
```

## Safety rules

- The LLM planner only proposes a path.
- Backend validates and executes.
- `organization_id` and current user are still backend-owned.
- Admin-only tools are still enforced in repository/service code.
- Guardrails and permission denials do not fall back to RAG.
- No write operations are executed.
- The LLM is instructed not to invent tool names, SQL, user IDs, organization IDs, or hidden data.

## Configuration

The phase adds optional configuration switches with safe defaults:

```yaml
tamias:
  ai:
    planning:
      enabled: true
      compose-tool-answers: true
```

If planning fails or returns invalid JSON, TAMIAS falls back to deterministic heuristics.

## Planner output

The planner is prompted to return only JSON:

```json
{
  "decision": "TOOL_FIRST",
  "reason": "The question asks for structured operational data.",
  "confidence": 0.85
}
```

The backend parses this JSON and converts it into `AiExecutionPlan`.

## Answer composition

Tool HIT answers can now be passed through `AiAnswerCompositionService` so the LLM can make the response more natural while using only backend-provided data.

For example, the backend still produces the original tool answer and evidence, but the LLM may rewrite it in a clearer way without adding facts.

If composition fails, TAMIAS returns the original backend tool answer.

## What this phase does not do yet

This phase does not expose OpenAI function/tool calling directly. The backend still uses its existing Java handlers and repositories to execute tools safely.

A future phase can add OpenAI function calling / structured tool invocation more directly, but this phase already makes the LLM participate in planning and answer composition.

## Suggested tests

Document/RAG-first:

```text
¿Qué dice el PDF sobre visitantes?
¿Qué reglas hay sobre basura?
¿Qué menciona el manual sobre filtros?
```

Tool-first:

```text
¿Qué usuarios activos tengo?
¿Cuántos usuarios tiene mi organización?
¿Qué permisos tiene Maintenance Staff?
¿Qué propiedades tengo?
```

Tool + RAG / mixed:

```text
Según mis documentos y datos del sistema, ¿qué debo revisar antes de la próxima reserva?
¿Qué reglas del documento aplican a la propiedad Bungalow Tu Refugio Perfecto?
```

Guardrails:

```text
Crea una reservación para mañana.
Elimina este documento.
Marca esta tarea como completada.
```

Fallback:

```text
¿Qué dice el documento sobre una regla que no existe?
Busca en mis registros y documentos algo sobre un tema inexistente.
```
