# TAMIAS — Roadmap

This roadmap reflects the current state after AI tool calling/orchestration, S3/image hard-delete work, inventory brand/image work, AI debug traces, smoke-test hardening and Product Box Models through automatic contour detection/image enhancement and AI texture metadata/provider abstraction.

## Completed foundation

### Phase 0 — Project Setup
Status: Completed.

### Phase 1 — Security and SaaS Foundation
Status: Completed / MVP-ready.

### Phase 2 — Properties and Catalogs
Status: Completed / MVP-ready.

### Phase 3 — Maintenance
Status: Completed / MVP-ready.

### Phase 4 — Reservations and Tasks
Status: Completed / MVP-ready.

### Phase 5 — Purchase Lists
Status: Completed / MVP-ready.

### Phase 6 — Documents
Status: Completed / MVP-ready.

### Phase 7 — AI Document Search
Status: Completed / MVP-ready.

### Phase 8 — MVP Hardening
Status: Completed / partially superseded by security review phases.

## Completed AI tool calling and orchestration track

### Phase 9 — AI Tool Calling and AI Orchestration
Status: Completed through 9P-H.

Completed sub-phases:

```text
9B Backend read-only AI tools
9C AI Assistant integration
9D AI Assistant frontend UX
9G Property and catalog read-only tools
9H Inventory and maintenance analytics tools
9I Scheduled maintenance, reservation and guest tools
9J Reservation supply and task tools
9K Purchase analytics tools
9L Document metadata and RAG health tools
9M File, image and dashboard analytics tools
9N Admin user, role and organization tools
9O AI chat session history tools
9P-A AI orchestration safe refactor
9P-B AI tool handler split
9P-C AI read-only domain service split
9P-C.1 AI tool package reorganization
9P-C.2 AI tool repository split
9P-D Tool/RAG fallback
9P-E LLM-driven planning
9P-F AI orchestration smoke tests and RAG diagnostics
9P-G AI orchestration observability and persisted debug traces
9P-H AI smoke test hardening and final fixes
```

### 9P-I — RAG retrieval tuning
Status: Conditional / optional / not started.

Goals:

- Tune threshold, topK, metadata filters, property-aware retrieval and chunk quality only if PDFs/documents still show retrieval issues.
- Do not start this phase for structured-tool routing, parameter extraction or formatting problems.
- Keep 9P-I optional for now.

## Completed implementation queue after initial MVP

Phases 10A through 13A are completed and documented in their corresponding `.md` files.

## Current implementation queue

### Phase 14 — Product Box Models
Status: In progress / completed through 14L.

Documentation: `71-product-box-models-14.md`

Completed subphases:

```text
14A Product Box Models backend foundation
14B Product Box Face Images
14C Angular Product Box CRUD
14D Three.js Product Box Viewer
14E Product Box 3D Textures architecture/design
14F Texture metadata + original upload
14G OpenCV perspective correction backend
14H Angular corner editor + processed texture preview
14I Accept/retry/delete texture workflow
14J Automatic contour detection and image enhancement
14K AI Texture Enhancement architecture/design
14L AI Texture metadata and backend provider abstraction
```

Next subphases:

```text
14M AI Texture enhancement backend
14N Angular AI enhanced preview and accept workflow
14O Integration with Inventory/Purchases
14P AI awareness for Product Box Models
```

## Future phases

```text
15 Reports
16 Notifications and reminders
17 Blueprint Analysis
```
