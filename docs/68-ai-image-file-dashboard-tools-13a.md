# 13A — AI image/file dashboard tools

Status: Completed.

## Purpose

Add read-only AI tools that let TAMI answer high-level questions about images and files across TAMIAS modules.

This phase remains the owner of the `13A` number. Do not reuse `13A` for later Product Box Models work.

## Implementation summary

Implemented in backend AI tools only. No frontend changes, no write actions and no database migrations are part of this phase.

Implemented tools:

```text
files.getImageDashboardSummary
files.getRecentUploads
files.getLargestFiles
files.getEntitiesWithoutImages
files.getEntitiesWithMostImages
```

13A extends the existing 9M file/image/dashboard module instead of creating a duplicate package.

## After 13A

The AI observability and smoke-test follow-up work has now been completed through:

```text
9P-G — AI orchestration observability and persisted debug traces
9P-H — Smoke test hardening / final fixes
```

`9P-I — RAG retrieval tuning` remains optional and should only be started if real document retrieval quality issues appear.

The next planned feature phase is:

```text
14 — Product Box Models
```
