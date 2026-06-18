# 10A — Chat Ownership Security + Quick AI UI Fixes + Last Login

## Purpose

This phase fixes urgent AI chat privacy issues and small UX issues before continuing with deeper AI observability.

The most important security goal is:

```text
AI chats must be visible only to the authenticated user who owns them.
```

Organization isolation is still required, but it is not enough for AI chat history because multiple users inside the same organization may have different roles and may ask about sensitive data.

---

## Scope

### Backend

- Restrict AI chat sessions to the authenticated owner user.
- Restrict AI chat messages to sessions owned by the authenticated user.
- Restrict AI chat history tools to the authenticated owner user.
- Add `users.last_login`.
- Update `users.last_login` after a successful login.
- Surface `lastLogin` in access summary responses/tools where relevant.

### Frontend

- Send AI message with `Enter`.
- Insert newline with `Shift + Enter`.
- Rename visible assistant label from `ASISTENTE` to `TAMI`.
- Auto-collapse mobile sidebar after selecting an option.

---

## Security rules

### AI chat ownership

All normal AI Assistant chat APIs must enforce:

```text
organization_id = current user's organization
created_by / user_id / owner_user_id = current user
```

Use the real ownership field from the current entity/repository. Do not invent a new column without checking the existing entity and migrations.

### AI history tools

These tools must only return current user's own chat data:

```text
aiChat.recentSessions
aiChat.searchHistory
aiChat.recentMessages
aiChat.sessionsByProperty
aiChat.currentSessionSummary
aiChat.usageSummary
```

If a tool currently aggregates by organization only, update it to also filter by the authenticated user.

### Admin behavior

Administrators should not see every user's AI chat history in the normal AI Assistant UI/tools.

A future audit/admin feature could expose broader visibility, but it must be explicitly designed, authorized and documented. It is not part of this phase.

---

## Database changes

### users

Add:

```text
last_login timestamp column
```

Use the same timestamp type/style already used by existing audit fields in the project.

Expected behavior:

```text
successful login -> update users.last_login = current timestamp
failed login -> do not update users.last_login
```

---

## Backend implementation notes

Before modifying queries, verify:

```text
1. AiChatSession entity fields
2. AiChatMessage entity fields
3. Existing Flyway migrations for AI chat tables
4. Repository method naming and native SQL columns
5. Auth/current user helper methods
```

Do not invent fields such as `owner_id`, `user_id`, `name`, or similar unless they exist.

Suggested repository patterns:

```text
findByIdAndOrganization_IdAndCreatedBy_Id(...)
findByOrganization_IdAndCreatedBy_Id(...)
findByChatSession_IdAndOrganization_IdAndChatSession_CreatedBy_Id(...)
```

Adapt names to the actual entity model.

---

## Frontend behavior

### Enter and Shift + Enter

Expected behavior in the AI Assistant textarea:

```text
Enter         -> send message
Shift + Enter -> insert newline
```

Rules:

- Do not send empty/blank messages.
- Do not send while a request is already in progress.
- Keep multiline input possible with Shift + Enter.

### Assistant label

Replace visible UI text:

```text
ASISTENTE -> TAMI
```

Apply this only to visible labels/titles. Do not rename backend packages/classes unless needed.

### Mobile sidebar

When the sidebar is open on mobile and the user selects a route:

```text
navigate -> close sidebar automatically
```

The user should not need to press the `X` button after selecting a menu option.

---

## Acceptance tests

### Security

Use two users in the same organization:

```text
User A: Administrator
User B: Maintenance Staff or Read Only
```

Test:

```text
1. User A creates AI chats.
2. User B logs in.
3. User B opens AI Assistant.
4. User B must not see User A's chat sessions.
5. User B asks: Muéstrame mis últimas conversaciones con la IA.
6. Response must only include User B's own sessions.
7. User B asks: Busca en el historial si hablamos de cloro.
8. Response must only search User B's own messages.
```

### Last login

```text
1. Check user's last_login before login.
2. Login successfully.
3. Verify users.last_login changed.
4. Attempt failed login.
5. Verify users.last_login did not change because of failed login.
```

### AI input

```text
Enter sends message.
Shift + Enter creates a new line.
Blank message is not sent.
```

### TAMI label

```text
AI assistant UI displays TAMI instead of ASISTENTE.
```

### Mobile sidebar

```text
Open sidebar on mobile width.
Click any route.
Sidebar closes automatically.
```

---

## Out of scope

- Typewriter animation. That belongs to 10B.
- Admin audit view for all users' chat histories.
- AI observability/debug traces. That belongs to 9P-G.
- RAG retrieval tuning. That belongs to 9P-I if needed.
