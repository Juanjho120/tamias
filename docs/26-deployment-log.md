# TAMIAS — Deployment Log

## Purpose

Use this file to record public deployment information.

Do not store secrets here.

Allowed:

```text
public URLs
service names
bucket names
collection names
deployment dates
smoke test results
known issues
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
```

---

# Deployment summary

| Field | Value |
|---|---|
| Deployment date | `<yyyy-mm-dd>` |
| Git commit SHA | `<commit-sha>` |
| Docker backend image | `<dockerhub-username>/tamias-backend:sha-<short-sha>` |
| Docker frontend image | `<dockerhub-username>/tamias-frontend:sha-<short-sha>` |

---

# Testing / Portfolio

| Resource | Value |
|---|---|
| Frontend domain | `https://tamias.juantzun.dev` |
| Vercel project | `tamias-testing` |
| Backend URL | `https://tamias-api-testing.onrender.com` |
| Render service | `tamias-api-testing` |
| Supabase project/db | `tamias-testing` |
| S3 bucket | `tamias-testing-files` |
| Chroma collection | `tamias_testing_documents` |

## Testing smoke test result

| Check | Result | Notes |
|---|---|---|
| Frontend loads | `PENDING` |  |
| Backend health | `PENDING` |  |
| Login | `PENDING` |  |
| Dashboard analytics | `PENDING` |  |
| Properties | `PENDING` |  |
| Files | `PENDING` |  |
| Documents | `PENDING` |  |
| AI/RAG | `PENDING` |  |

---

# Production real

| Resource | Value |
|---|---|
| Frontend domain | `https://tamias-prod.juantzun.dev` |
| Vercel project | `tamias-production` |
| Backend URL | `https://tamias-api-prod.onrender.com` |
| Render service | `tamias-api-prod` |
| Supabase project/db | `tamias-production` |
| S3 bucket | `tamias-prod-files` |
| Chroma collection | `tamias_prod_documents` |

## Production smoke test result

| Check | Result | Notes |
|---|---|---|
| Frontend loads | `PENDING` |  |
| Backend health | `PENDING` |  |
| Login | `PENDING` |  |
| Dashboard analytics | `PENDING` |  |
| Properties | `PENDING` |  |
| Files | `PENDING` |  |
| Documents | `PENDING` |  |
| AI/RAG | `PENDING` |  |

---

# Known issues

```text
None yet.
```

---

# Follow-up actions

```text
[ ] Update portfolio page with testing URL
[ ] Add screenshots after testing deployment is stable
[ ] Decide whether production should continue auto-deploying from main
[ ] Consider release-tag based production deployments later
```
