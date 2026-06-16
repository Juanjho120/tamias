# 46 — AI Orchestration Refactor 9P-A

## Goal

This phase is a safe refactor of the AI assistant orchestration layer. It is intentionally conservative: it does not change the external behavior of the assistant, and it does not add LLM-driven planning yet.

The purpose is to stabilize the codebase before adding smarter fallback behavior and OpenAI tool planning in later phases.

## Why this refactor is needed

Before 9P-A, the AI assistant had two large classes carrying too many responsibilities:

- `AiToolCallingService` handled routing, intent checks, normalization, guardrails, tool ordering and response composition.
- `AiReadOnlyToolService` handled SQL queries, response formatting, search extraction, normalization and tokenization.

This made the assistant fragile. A small routing fix could break unrelated questions, and repeated text-normalization logic created multiple sources of errors.

## What changed in 9P-A

### 1. Centralized text normalization

A new utility class was added:

```text
com.tamias.ai.tool.AiToolTextNormalizer
```

It centralizes:

- accent removal without Java regex
- whitespace collapsing without Java regex
- word splitting without Java regex
- line splitting without Java regex
- search-character cleanup
- routing normalization

`AiToolCallingService`, `AiReadOnlyToolService` and `AiChatSessionService` now delegate to this shared helper.

### 2. Added result status foundation

New classes were added:

```text
AiToolResult
AiToolResultStatus
```

The statuses are:

```text
HIT
EMPTY
DENIED
GUARDRAIL
NOT_APPLICABLE
ERROR
```

In 9P-A, `AiToolCallingService.tryHandle(...)` remains compatible with the current behavior by returning `Optional<AiToolAnswer>`. Internally, a new `tryHandleResult(...)` method is available as the bridge for later fallback logic.

### 3. Kept behavior stable

9P-A does not yet change the main AI answer flow:

```text
Question
  -> AiToolCallingService.tryHandle(...)
  -> if tool matched, return tool answer
  -> otherwise continue to RAG in AiRagService
```

This phase is a foundation, not the final orchestration behavior.

## What did not change

9P-A does not add:

- LLM-driven tool planning
- OpenAI function/tool calling
- Tool -> RAG fallback
- RAG -> Tool fallback
- final combined answer when both tool and RAG are empty
- handler/repository split by domain

Those are intentionally left for the next phase to avoid another large risky change.

## Next recommended phase

### 9P-B — Tool/RAG fallback orchestration

Recommended goals:

1. Make tools return `AiToolResultStatus.HIT` or `AiToolResultStatus.EMPTY` instead of treating every `AiToolAnswer` as final.
2. Allow Tool EMPTY -> RAG fallback when the question may be answered by documents.
3. Allow RAG EMPTY -> Tool fallback when the question is clearly operational.
4. Add a final unified message when both structured tools and RAG find nothing.
5. Keep `DENIED` and `GUARDRAIL` as terminal responses with no RAG fallback.

### 9P-C — LLM-driven planning and answer composition

Recommended goals:

1. Use the LLM to classify intent and propose tool/RAG/both.
2. Keep backend validation and execution authoritative.
3. Use structured output for the planning response.
4. Let the LLM compose final answers from tool evidence and RAG evidence.

## Safety constraints preserved

- Tools remain read-only.
- Admin-only tools still enforce administrator checks in backend.
- `organization_id` is always taken from the authenticated user context.
- `user_id` is always taken from the authenticated user context.
- No password hashes, tokens or internal secrets are exposed.
