# 9P-G — AI orchestration observability and persisted debug traces

Status: **Implemented / validation pending**

## Context

TAMIAS already has read-only AI tools, LLM-driven planning, tool/RAG fallback, smoke-test coverage, image/file dashboard tools, and several routing fixes for inventory, brands, images, files and documents.

The next weakness is observability: when TAMI gives an unexpected answer, it is still hard to know whether the issue came from:

- planner intent selection,
- handler routing,
- tool parameter extraction,
- tool execution,
- RAG fallback,
- answer composition,
- or a direct backend answer being rewritten by the LLM.

This phase adds persisted debug traces per TAMI response so we can inspect how the answer was produced without guessing.

## Goal

Persist one debug trace for each assistant/TAMI response message and optionally expose it to users with an explicit debug flag.

The debug trace must be stored in a new table:

```text
ai_chat_message_debugs
```

The trace must reference the `ai_chat_messages.id` row that represents the assistant response.

## Non-goals

This phase must not:

- tune RAG thresholds,
- change prompt templates unless required for trace capture,
- change tool business logic,
- add write/action AI tools,
- expose debug traces to all users,
- expose other users' chat traces,
- add a full admin analytics dashboard,
- add storage cleanup/repair actions.

RAG tuning remains reserved for `9P-I` and only if needed.

## Data model

### Migration

Expected migration name:

```text
V31__create_ai_chat_message_debugs.sql
```

The migration should add the user flag and the debug table.

```sql
ALTER TABLE users
ADD COLUMN IF NOT EXISTS ai_chat_debug BOOLEAN NOT NULL DEFAULT false;
```

```sql
CREATE TABLE ai_chat_message_debugs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    ai_chat_message_id UUID NOT NULL,

    handler VARCHAR(150),
    tool_name VARCHAR(150),
    tool_names JSONB NOT NULL DEFAULT '[]'::jsonb,
    params JSONB NOT NULL DEFAULT '{}'::jsonb,

    rag_used BOOLEAN NOT NULL DEFAULT false,
    answer_source VARCHAR(50) NOT NULL,

    route_reason VARCHAR(500),
    fallback_reason VARCHAR(500),
    error_message TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_chat_message_debugs_message
        FOREIGN KEY (ai_chat_message_id)
        REFERENCES ai_chat_messages(id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX ux_ai_chat_message_debugs_message_id
    ON ai_chat_message_debugs(ai_chat_message_id);

CREATE INDEX idx_ai_chat_message_debugs_tool_name
    ON ai_chat_message_debugs(tool_name);

CREATE INDEX idx_ai_chat_message_debugs_answer_source
    ON ai_chat_message_debugs(answer_source);

CREATE INDEX idx_ai_chat_message_debugs_created_at
    ON ai_chat_message_debugs(created_at);
```

### Why one row per assistant message?

The preferred model is one consolidated trace per TAMI response.

This keeps message rendering and debugging simple:

```text
ai_chat_messages.id -> ai_chat_message_debugs.ai_chat_message_id
```

For multi-tool answers, store the primary/summary tool in `tool_name` and every executed tool in `tool_names`.

Example:

```json
{
  "handler": "InventoryToolHandler",
  "toolName": "inventory.whereUsed",
  "toolNames": [
    "inventory.getItemsUsedInMaintenance",
    "inventory.getItemsUsedInReservations",
    "inventory.getItemsUsedInPurchases"
  ],
  "params": {
    "itemName": "covertor elastico"
  },
  "ragUsed": false,
  "answerSource": "BACKEND_DIRECT"
}
```

## Field definitions

### `ai_chat_message_id`

Must reference the assistant/TAMI response message.

It must not reference the user prompt message.

### `handler`

The backend handler that claimed or handled the request.

Examples:

```text
InventoryToolHandler
EntityImageToolHandler
FileImageDashboardToolHandler
DocumentToolHandler
AiRagFallback
```

### `tool_name`

The primary tool used for the answer.

Examples:

```text
inventory.getItemsByBrand
images.getReservationImages
files.getRecentUploads
rag.search
```

For multi-tool flows, this can be a logical summary tool such as:

```text
inventory.whereUsed
```

### `tool_names`

JSON array with all tools executed during the response.

Examples:

```json
["inventory.getItemsByBrand"]
```

```json
[
  "inventory.getItemsUsedInMaintenance",
  "inventory.getItemsUsedInReservations",
  "inventory.getItemsUsedInPurchases"
]
```

### `params`

JSON object with sanitized routing/tool parameters.

Examples:

```json
{"brandName":"Pledge"}
```

```json
{"itemName":"covertor elastico"}
```

Do not store unnecessary sensitive values. Do not duplicate the whole user prompt if it is already persisted in `ai_chat_messages`.

### `rag_used`

`true` only when RAG retrieval was actually used.

A structured tool answer with no RAG fallback must store:

```json
{"ragUsed": false}
```

### `answer_source`

`answerSource` describes how the final answer was produced. It should not be overloaded with tool names.

Recommended values:

```text
BACKEND_DIRECT
LLM_COMPOSED
RAG
TOOLS_AND_RAG
NO_MATCH
ERROR
```

Meaning:

| Value | Meaning |
| --- | --- |
| `BACKEND_DIRECT` | Backend structured answer returned as-is. |
| `LLM_COMPOSED` | Tool/system data existed, but final answer passed through LLM composition. |
| `RAG` | Answer came from document/RAG retrieval only. |
| `TOOLS_AND_RAG` | Both structured tools and RAG contributed to the final answer. |
| `NO_MATCH` | No tool/RAG match was found. |
| `ERROR` | An exception or controlled failure happened. |

### `route_reason`

Optional short reason explaining why a handler/tool was selected.

Examples:

```text
Matched inventory brand query.
Matched reservation image metadata query.
Matched cross-module file dashboard query.
```

### `fallback_reason`

Optional reason why fallback was used or skipped.

Examples:

```text
Tool returned no rows; RAG fallback skipped because question is structured metadata.
Tool could not answer; RAG fallback attempted.
```

### `error_message`

Optional sanitized error message when the execution failed.

Do not store stack traces in this field.

## Backend implementation plan

### New/updated files

Expected new files:

```text
backend/src/main/java/com/tamias/ai/entity/AiChatMessageDebug.java
backend/src/main/java/com/tamias/ai/repository/AiChatMessageDebugRepository.java
backend/src/main/java/com/tamias/ai/dto/AiChatMessageDebugResponse.java
backend/src/main/java/com/tamias/ai/dto/AiToolDebugTrace.java
backend/src/main/java/com/tamias/ai/enums/AiAnswerSource.java
backend/src/main/java/com/tamias/ai/service/AiChatDebugTraceService.java
backend/src/main/resources/db/migration/V31__create_ai_chat_message_debugs.sql
```

Expected updated files:

```text
backend/src/main/java/com/tamias/user/entity/User.java
backend/src/main/java/com/tamias/ai/dto/AiChatMessageResponse.java
backend/src/main/java/com/tamias/ai/dto/AiChatResponse.java
backend/src/main/java/com/tamias/ai/mapper/AiChatMapper.java
backend/src/main/java/com/tamias/ai/service/AiChatService.java
backend/src/main/java/com/tamias/ai/planning/*
backend/src/main/java/com/tamias/ai/tool/handler/*
backend/src/main/java/com/tamias/ai/tool/service/*
```

Only touch specific handlers/services where trace metadata is needed. Do not collapse the existing modular AI package into one large class.

### Capture flow

Recommended flow:

1. User sends prompt.
2. Backend stores the user message.
3. AI orchestration starts a mutable debug trace object.
4. Planner/handler records selected handler and route reason.
5. Tool execution records primary tool, all tools and params.
6. RAG step records `ragUsed` and fallback reason.
7. Answer composition records `answerSource`.
8. Backend stores the assistant/TAMI message.
9. Backend persists one `ai_chat_message_debugs` row referencing the assistant message id.
10. API response includes `debug` only when the authenticated user has `ai_chat_debug = true`.

## API response behavior

Debug must be optional.

When `users.ai_chat_debug = false`:

```json
{
  "message": {
    "role": "ASSISTANT",
    "content": "..."
  }
}
```

When `users.ai_chat_debug = true`:

```json
{
  "message": {
    "role": "ASSISTANT",
    "content": "...",
    "debug": {
      "handler": "InventoryToolHandler",
      "toolName": "inventory.getItemsByBrand",
      "toolNames": ["inventory.getItemsByBrand"],
      "params": {"brandName":"Pledge"},
      "ragUsed": false,
      "answerSource": "BACKEND_DIRECT",
      "routeReason": "Matched inventory brand query.",
      "fallbackReason": null
    }
  }
}
```

## Frontend behavior

The backend should be the authority for whether debug is included.

Frontend should not decide if the user is allowed to see debug. If `debug` is absent, render nothing.

Initial UI can be simple:

```text
Debug trace
handler: InventoryToolHandler
tool: inventory.getItemsByBrand
params: {"brandName":"Pledge"}
ragUsed: false
answerSource: BACKEND_DIRECT
```

A polished UI can be deferred if needed, but the response contract should be ready.

## Security rules

- Debug traces must be saved for every assistant response.
- Debug traces must only be returned to the authenticated owner of the chat/session.
- Debug traces must only be included in responses when `users.ai_chat_debug = true`.
- Organization isolation still applies through the existing AI chat ownership/session checks.
- Admin role alone should not automatically expose another user's chat traces in the normal assistant UI.
- Do not expose raw stack traces, secrets, JWTs, S3 credentials, presigned URLs or arbitrary SQL.
- `params` must be sanitized and limited to the structured parameters used by tools.

## Error behavior

If TAMI fails before producing an assistant response, the implementation should still prefer storing a controlled assistant error message and a debug row with:

```text
answerSource = ERROR
```

If debug persistence itself fails, the normal user-facing chat response should not crash unless the failure indicates a transaction consistency issue that prevents saving the assistant message. Prefer logging the debug persistence failure and continue with the chat response.

## Testing checklist

### Migration

- `users.ai_chat_debug` exists and defaults to `false`.
- `ai_chat_message_debugs` exists.
- FK references `ai_chat_messages(id)`.
- Deleting a chat message cascades to its debug row.
- Unique index prevents multiple debug rows for the same assistant message.

### Backend tests

- A normal TAMI answer persists one debug row.
- Tool-only response persists `BACKEND_DIRECT` and `ragUsed=false`.
- RAG answer persists `RAG` and `ragUsed=true`.
- Tool + RAG answer persists `TOOLS_AND_RAG` and `ragUsed=true`.
- Failed answer persists `ERROR` with sanitized `errorMessage`.
- API response omits debug when `ai_chat_debug=false`.
- API response includes debug when `ai_chat_debug=true`.
- User cannot read another user's debug trace through chat history.

### Manual smoke tests

Run these prompts with `ai_chat_debug=false` and verify no debug block appears:

```text
¿Qué productos tengo en inventario?
¿Qué items tengo de la marca Pledge?
¿Qué reservaciones tienen imágenes?
¿Qué documentos o imágenes fueron subidos recientemente?
```

Then set `ai_chat_debug=true` for the user and verify the same answers include debug metadata.

Suggested SQL:

```sql
UPDATE users
SET ai_chat_debug = true
WHERE email = '<your-email>';
```

Inspect persisted traces:

```sql
SELECT
    d.id,
    d.ai_chat_message_id,
    d.handler,
    d.tool_name,
    d.tool_names,
    d.params,
    d.rag_used,
    d.answer_source,
    d.created_at
FROM ai_chat_message_debugs d
ORDER BY d.created_at DESC
LIMIT 20;
```

## Acceptance criteria

- A new assistant message creates exactly one debug trace row.
- Debug row references the assistant `ai_chat_messages.id`.
- Trace includes handler, primary tool, all tools, params, `ragUsed` and `answerSource`.
- Debug is hidden by default.
- Debug is visible only when the authenticated user has `ai_chat_debug=true`.
- Tool names are not stored inside `answerSource`.
- Normal TAMI behavior remains unchanged for users without debug enabled.

## Next phases

After this phase:

```text
9P-H — Smoke test hardening / final fixes
```

Then only if needed:

```text
9P-I — RAG retrieval tuning
```


## Implementation notes

Implemented in backend only.

- `V31__create_ai_chat_message_debugs.sql` adds `users.ai_chat_debug` and creates `ai_chat_message_debugs`.
- `AiChatMessageDebug` persists one consolidated trace per assistant/TAMI message.
- `AiChatDebugTraceService` always saves traces but only exposes them when the current user has `ai_chat_debug = true`.
- `AiToolCallingService` now attaches handler/tool metadata to `AiToolResult`.
- `AiRagService` persists debug traces immediately after saving the assistant message.
- `AiChatResponse` and `AiChatMessageResponse` include an optional `debug` object.
- Existing users default to `ai_chat_debug = false`, so normal chat responses stay clean.

## Validation prompts

Use these after enabling `users.ai_chat_debug = true` for the target user:

```sql
UPDATE users
SET ai_chat_debug = true
WHERE email = '<your-user-email>';
```

Suggested prompts:

- ¿Qué productos tengo de la marca Pledge?
- ¿Qué imágenes se subieron recientemente?
- ¿Qué documentos hablan sobre reglas de la casa?
- ¿Qué items no tienen imágenes?

Expected behavior:

- Normal users do not receive `debug` in responses.
- Debug-enabled users receive `debug` only for their own chat messages.
- Every assistant message has one row in `ai_chat_message_debugs`.
