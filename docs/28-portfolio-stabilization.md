# TAMIAS — Portfolio Stabilization

## Purpose

This document tracks the short stabilization phase after the first successful deployment.

Deployment was validated for:

```text
Testing / Portfolio -> https://tamias.juantzun.dev
Production real     -> https://tamias-prod.juantzun.dev
```

This phase focuses on presentation, documentation polish, screenshots, and known limitations.

---

## Current state

| Area | Status |
|---|---|
| Testing frontend | `LIVE / VALIDATED` |
| Testing backend | `LIVE / VALIDATED` |
| Production frontend | `LIVE / VALIDATED` |
| Production backend | `LIVE / VALIDATED` |
| Login | `PASS` |
| User creation | `PASS` |
| S3 file storage | `PASS` |
| AI/RAG | `PASS` |
| Chroma isolation | `PASS` |
| DB isolation | `PASS` |
| Environment isolation | `PASS` |

---

## Stabilization goals

```text
[ ] Make README portfolio-ready
[ ] Add known limitations
[ ] Add screenshot checklist
[ ] Update deployment log if needed
[ ] Capture screenshots from testing environment
[ ] Prepare short portfolio project description
[ ] Prepare long portfolio case study
[ ] Add note about Render cold starts
```

---

## Portfolio positioning

TAMIAS should be presented as:

```text
A full-stack SaaS-style property operations platform for small lodging businesses.
```

Suggested short description:

```text
TAMIAS is a SaaS-style platform for managing small lodging properties, including maintenance, reservations, task lists, purchase lists, documents, S3 file storage, and AI-assisted document search using RAG.
```

Suggested technical description:

```text
Built with Java 21, Spring Boot 3, Angular, PostgreSQL, AWS S3, Spring AI, OpenAI, Chroma, Docker, GitHub Actions, Vercel, Render, Supabase, Railway, Docker Hub, and Cloudflare.
```

---

## Portfolio highlights

Use these highlights:

```text
Dual-environment deployment
SaaS-style organization-based data isolation
JWT authentication and role-based access control
AWS S3 private file storage with pre-signed URLs
AI document search using RAG
Dockerized backend and frontend
CI validates and publishes Docker images
Real deployment on Vercel, Render, Supabase, Railway, AWS, Docker Hub, and Cloudflare
```

---

## Screenshots to capture

Use:

```text
docs/29-screenshot-checklist.md
```

Recommended environment for screenshots:

```text
https://tamias.juantzun.dev
```

Reason:

```text
The testing/portfolio environment can contain demo data without exposing real production data.
```

---

## Known limitations

Use:

```text
docs/30-known-limitations.md
```

The most important limitation to mention in portfolio/demo context:

```text
The backend can have cold starts because it is hosted on a free-tier instance.
```

---

## Suggested README badge/status section later

Optional future polish:

```text
Deployment: Live
Backend: Render
Frontend: Vercel
CI: GitHub Actions
Docker: Docker Hub
AI: OpenAI + Chroma
Storage: AWS S3
Database: Supabase PostgreSQL
```

---

## Follow-up checklist

```text
[ ] Add screenshots to portfolio website
[ ] Add project link to personal domain portfolio page
[ ] Add architecture diagram image
[ ] Add deployment diagram image
[ ] Add short demo data set
[ ] Consider adding a public demo user with restricted role
[ ] Consider paid Render instance for production
[ ] Decide if production should continue auto-deploying from main
```

---

## Last updated

```text
2026-06-11
```
