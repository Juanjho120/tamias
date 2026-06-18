# 10B — Typewriter Animation for TAMI Responses

## Purpose

Improve the AI Assistant user experience by making TAMI's answer appear progressively, similar to a typing/typewriter effect.

This phase uses the simple approach:

```text
Backend returns the full answer.
Frontend simulates the typing animation letter by letter.
```

No backend streaming is required in this phase.

---

## Scope

### Frontend only

- Add typewriter animation for assistant answers.
- Keep complete answer data in memory.
- Preserve sources, tool evidence, grounded status and metadata.
- Avoid changing the AI backend API contract.

---

## UX behavior

When the backend response arrives:

```text
1. Store the full assistant response internally.
2. Display the answer progressively.
3. Show sources/tool evidence after the answer finishes, or keep them visually attached while the text is being typed.
4. Allow the user to continue normally after animation ends.
```

Recommended details:

```text
Typing speed: configurable in component/service.
Animation unit: character-based first; word-based can be evaluated later.
While typing: input can stay disabled or enabled depending current UX choice.
```

For MVP simplicity, prefer:

```text
Disable sending another prompt while the answer is typing.
```

---

## Important rules

- Do not mutate the real answer text.
- Do not animate sources as if they were part of the natural language answer unless the current UI already renders them inline.
- Do not lose markdown formatting permanently.
- Do not remove `toolEvidence[]` or `sources[]`.
- Do not require backend SSE/WebSocket.

---

## Suggested implementation approach

Create a small frontend helper/state pattern:

```text
fullText: string
visibleText: string
isTyping: boolean
interval/timer subscription
```

Flow:

```text
onAssistantResponse(response):
  fullText = response.answer
  visibleText = ''
  isTyping = true
  start timer

onTimerTick:
  visibleText += next characters
  if visibleText == fullText:
    isTyping = false
    stop timer
```

If the component is destroyed:

```text
clear timer/subscription
```

---

## Optional improvement

Add a future button:

```text
Skip animation
```

This is optional and not required for this phase.

---

## Acceptance tests

```text
1. Ask a question to TAMI.
2. Backend returns complete answer.
3. UI displays answer progressively.
4. Final visible answer exactly matches backend answer.
5. Sources are still shown.
6. Tool evidence is still shown.
7. No duplicate messages are created.
8. Navigating away does not leave active timers/subscriptions.
```

---

## Out of scope

- True streaming from backend.
- SSE/WebSocket.
- Token-level streaming from OpenAI.
- Backend changes.
