# TAMIAS — Deployment Log

## Purpose

This file records public deployment information for TAMIAS.

Do not store secrets here.

Allowed:

```text
public URLs
service names
project names
bucket names
collection names
deployment dates
smoke test results
known issues
follow-up actions
```

Not allowed:

```text
passwords
tokens
API keys
database passwords
JWT secrets
AWS secret access keys
private connection strings
private Railway/Chroma URLs
private Supabase connection strings
```

---

# Deployment summary

| Field | Value |
|---|---|
| Deployment date | `2026-06-11` |
| Git branch | `main` |
| Deployment status | `SUCCESS` |
| Testing environment status | `LIVE / VALIDATED` |
| Production environment status | `LIVE / VALIDATED` |
| Docker backend image | `<DOCKERHUB_USERNAME>/tamias-backend:latest` |
| Docker frontend image | `<DOCKERHUB_USERNAME>/tamias-frontend:latest` |
| Docker SHA tags | `sha-<short-git-sha>` |
| CI status | `PASSING` |
| Docker Hub publishing | `VALIDATED` |

---

# Environment overview

| Environment | Purpose | Frontend | Backend |
|---|---|---|---|
| Testing / Portfolio | Public demo and portfolio | `https://tamias.juantzun.dev` | `https://tamias-api-testing.onrender.com` |
| Production real | Real vacation rental operations | `https://tamias-prod.juantzun.dev` | `https://tamias-api-prod.onrender.com` |

---

# Testing / Portfolio

## Public resources

| Resource | Value |
|---|---|
| Frontend domain | `https://tamias.juantzun.dev` |
| Vercel project | `tamias-testing` |
| Backend URL | `https://tamias-api-testing.onrender.com` |
| Backend API base URL | `https://tamias-api-testing.onrender.com/api/v1` |
| Render service | `tamias-api-testing` |
| Supabase project/db | `tamias-testing` |
| S3 bucket | `tamias-testing-files` |
| Chroma collection | `tamias_testing_documents` |
| CORS allowed origin | `https://tamias.juantzun.dev` |
| Frontend API env var | `FRONTEND_API_BASE_URL=https://tamias-api-testing.onrender.com/api/v1` |

## Testing smoke test result

| Check | Result | Notes |
|---|---|---|
| Frontend loads | `PASS` | Login page loads from custom domain. |
| Backend health | `PASS` | Backend deployed and running on Render. |
| Login | `PASS` | Admin login validated. |
| User creation | `PASS` | Additional user created successfully. |
| Dashboard analytics | `PASS` | Dashboard loads after login. |
| Properties | `PASS` | Module validated during smoke testing. |
| Files / S3 | `PASS` | Testing files isolated in `tamias-testing-files`. |
| Documents | `PASS` | Upload/process/index flow validated. |
| AI/RAG | `PASS` | Assistant responds using testing documents. |
| Chroma integration | `PASS` | Testing Chroma collection validated. |
| Environment isolation | `PASS` | Testing frontend calls testing backend only. |

---

# Production real

## Public resources

| Resource | Value |
|---|---|
| Frontend domain | `https://tamias-prod.juantzun.dev` |
| Vercel project | `tamias-production` |
| Backend URL | `https://tamias-api-prod.onrender.com` |
| Backend API base URL | `https://tamias-api-prod.onrender.com/api/v1` |
| Render service | `tamias-api-prod` |
| Supabase project/db | `tamias-production` |
| S3 bucket | `tamias-prod-files` |
| Chroma collection | `tamias_prod_documents` |
| CORS allowed origin | `https://tamias-prod.juantzun.dev` |
| Frontend API env var | `FRONTEND_API_BASE_URL=https://tamias-api-prod.onrender.com/api/v1` |

## Production smoke test result

| Check | Result | Notes |
|---|---|---|
| Frontend loads | `PASS` | Login page loads from custom domain. |
| Backend health | `PASS` | Backend deployed and running on Render. |
| Login | `PASS` | Admin login validated. |
| User creation | `PASS` | Additional user created successfully. |
| Dashboard analytics | `PASS` | Dashboard loads after login. |
| Properties | `PASS` | Module validated during smoke testing. |
| Files / S3 | `PASS` | Production files isolated in `tamias-prod-files`. |
| Documents | `PASS` | Upload/process/index flow validated. |
| AI/RAG | `PASS` | Assistant responds using production documents. |
| Chroma integration | `PASS` | Production Chroma collection validated. |
| Environment isolation | `PASS` | Production frontend calls production backend only. |

---

# Cross-environment validation

| Check | Result | Notes |
|---|---|---|
| Testing frontend calls testing backend only | `PASS` | No calls to production backend. |
| Production frontend calls production backend only | `PASS` | No calls to testing backend. |
| Testing DB separated from production DB | `PASS` | Supabase environments are separated. |
| Testing S3 separated from production S3 | `PASS` | `tamias-testing-files` and `tamias-prod-files` are separate buckets. |
| Testing Chroma separated from production Chroma | `PASS` | Collections are separated. |
| Testing users do not appear in production | `PASS` | User data is isolated. |
| Production users do not appear in testing | `PASS` | User data is isolated. |
| JWT secrets are environment-specific | `PASS` | Secrets are not stored in this file. |
| CORS is environment-specific | `PASS` | Each backend allows only its matching frontend domain. |

---

# Deployment notes

## Supabase connection

Render backend services use Supabase Session Pooler instead of direct database connection.

Reason:

```text
Direct Supabase database connection can resolve to IPv6.
Render connection failed with Network unreachable when using direct connection.
Session Pooler resolved the issue.
```

Public note:

```text
No database passwords, pooler usernames, or connection strings are stored in this file.
```

## Frontend build-time environment

The frontend uses:

```text
npm run build:prod
```

and generates:

```text
frontend/src/environments/environment.prod.ts
```

from:

```text
FRONTEND_API_BASE_URL
```

Important fix applied during deployment:

```text
Angular production file replacement must map environment.ts to environment.prod.ts.
```

Without that replacement, production builds can keep calling:

```text
http://localhost:8080/api/v1
```

Validated result:

```text
Testing frontend calls https://tamias-api-testing.onrender.com/api/v1
Production frontend calls https://tamias-api-prod.onrender.com/api/v1
```

## Docker and CI

CI validates and publishes Docker images.

Published images:

```text
<DOCKERHUB_USERNAME>/tamias-backend:latest
<DOCKERHUB_USERNAME>/tamias-backend:sha-<short-git-sha>
<DOCKERHUB_USERNAME>/tamias-frontend:latest
<DOCKERHUB_USERNAME>/tamias-frontend:sha-<short-git-sha>
```

Frontend is intentionally dockerized even though Vercel deploys from source.

---

# Known issues

## Render Free tier cold starts

Status:

```text
KNOWN / ACCEPTED
```

Description:

```text
Render free services can sleep after inactivity.
The first request after sleep can take time while the backend wakes up.
```

Current decision:

```text
Testing / Portfolio can stay on free tier for now.
Production should move to a paid Render instance before relying on it for real operational usage.
```

---

# Security notes

```text
[✓] No secrets stored in repository docs.
[✓] No database passwords stored in deployment log.
[✓] No AWS secret access keys stored in deployment log.
[✓] No JWT secrets stored in deployment log.
[✓] No OpenAI API key stored in deployment log.
[✓] Public URLs and resource names only.
```

---

# Follow-up actions

```text
[ ] Update portfolio page with https://tamias.juantzun.dev
[ ] Add screenshots after testing environment is stable
[ ] Consider paid Render instance for production real usage
[ ] Decide whether production should continue auto-deploying from main
[ ] Consider release-tag based production deployments later
[ ] Consider documenting initial admin bootstrap process separately
[ ] Consider adding a visible portfolio note about Render cold starts
```

---

# Final status

```text
TAMIAS dual-environment deployment completed and validated.

Testing / Portfolio:
  https://tamias.juantzun.dev

Production real:
  https://tamias-prod.juantzun.dev
```
