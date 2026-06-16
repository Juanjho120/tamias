# 9O — AI chat session history tools

## Goal

Add read-only AI tools that let the TAMIAS assistant answer questions about existing AI chat sessions and AI chat history without falling back to document RAG.

This phase focuses on metadata and stored messages from:

- `ai_chat_sessions`
- `ai_chat_messages`

## Scope

Implemented tools:

- `aiChat.recentSessions`
- `aiChat.searchHistory`
- `aiChat.recentMessages`
- `aiChat.sessionsByProperty`
- `aiChat.currentSessionSummary`
- `aiChat.usageSummary`

## Security and data isolation

- All queries are scoped to the authenticated user's current `organization_id`.
- No writes are performed.
- The tools only read existing AI chat sessions/messages.
- They do not expose tokens, credentials, secrets or internal security data.
- Access follows the existing AI chat session read model already exposed by `/api/v1/ai/chat-sessions`.

## Routing examples

Examples that should be routed to the new tools:

- `Muéstrame mis últimas conversaciones con la IA`
- `¿Qué hemos hablado antes?`
- `Busca en el historial si hablamos de cloro`
- `¿Qué preguntas le hice al asistente?`
- `Resume esta conversación`
- `¿Cuántas sesiones de chat IA tengo?`
- `¿Qué chats tengo sobre Bungalow Tu Refugio Perfecto?`

## Notes

This phase also removes the remaining Java regex usage from AI chat session default title creation. This avoids the same class of `StackOverflowError` seen previously when Java attempted to compile regex patterns such as `\\s+`.

SQL-side regex/text operations were not introduced by this phase. Existing PostgreSQL text matching remains inside SQL where applicable.
