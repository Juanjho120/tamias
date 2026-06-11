# TAMIAS — Bloque 8C: Deployment Documentation with Dual Environments

## Scope

This block documents the real deployment plan for TAMIAS.

It does not deploy infrastructure.

It does not change application runtime code.

It defines the deployment process for:

```text
Testing / Portfolio
Production real
```

---

## Final environment strategy

| Environment | Purpose | Frontend domain | Backend service |
|---|---|---|---|
| Testing / Portfolio | Demo and portfolio | `https://tamias.juantzun.dev` | `tamias-api-testing` |
| Production real | Real vacation rental usage | `https://tamias-prod.juantzun.dev` | `tamias-api-prod` |

---

## Platform map

| Component | Testing / Portfolio | Production real |
|---|---|---|
| Frontend | Vercel project: `tamias-testing` | Vercel project: `tamias-production` |
| Backend | Render service: `tamias-api-testing` | Render service: `tamias-api-prod` |
| PostgreSQL | Supabase project/db: `tamias-testing` | Supabase project/db: `tamias-production` |
| Files | S3 bucket: `tamias-testing-files` | S3 bucket: `tamias-prod-files` |
| Chroma | `tamias_testing_documents` collection or testing instance | `tamias_prod_documents` collection or production instance |
| Images | Docker Hub: `tamias-backend`, `tamias-frontend` | Docker Hub: `tamias-backend`, `tamias-frontend` |
| DNS | Cloudflare: `tamias.juantzun.dev` | Cloudflare: `tamias-prod.juantzun.dev` |

---

## Deployment model

Initial MVP model:

```text
main branch
  -> GitHub Actions CI
  -> Docker images published to Docker Hub
  -> Render testing backend auto-deploy
  -> Render production backend auto-deploy
  -> Vercel testing frontend auto-deploy
  -> Vercel production frontend auto-deploy
```

This keeps both environments updated with the latest changes.

Trade-off:

```text
Every push/merge to main reaches production.
```

This is acceptable for the MVP because the user explicitly wants both environments to stay updated with the latest changes.

Later, production can move to:

```text
main branch  -> testing
release tags -> production
```

or:

```text
main branch       -> testing
production branch -> production
```

---

## Backend deployment source

Recommended for MVP:

```text
Render builds backend from GitHub repository Dockerfile.
```

Backend settings:

```text
Root Directory: backend
Dockerfile: backend/Dockerfile
Runtime: Docker
Auto Deploy: true
Branch: main
```

Docker Hub images are still published by CI for:

```text
portfolio credibility
portable releases
future hosting options
manual rollback by SHA
```

Do not make Docker Hub the default backend deployment source yet unless you intentionally want manual image-based deploys.

---

## Frontend deployment source

Use Vercel from GitHub source.

Each Vercel project points to:

```text
Repository: Juanjho120/tamias
Branch: main
Root Directory: frontend
Build Command: npm run build:prod
Output Directory: dist/tamias-frontend/browser
```

Each project must set its own `FRONTEND_API_BASE_URL`.

Testing:

```text
FRONTEND_API_BASE_URL=https://tamias-api-testing.onrender.com/api/v1
```

Production:

```text
FRONTEND_API_BASE_URL=https://tamias-api-prod.onrender.com/api/v1
```

---

## Docker Hub publishing

Docker Hub publishing is part of:

```text
.github/workflows/ci.yml
```

Images:

```text
<DOCKERHUB_USERNAME>/tamias-backend:latest
<DOCKERHUB_USERNAME>/tamias-backend:sha-<short-sha>
<DOCKERHUB_USERNAME>/tamias-frontend:latest
<DOCKERHUB_USERNAME>/tamias-frontend:sha-<short-sha>
```

Publishing runs only on:

```text
push to main
```

It does not run on pull requests.

---

## DNS plan

Cloudflare should manage:

```text
juantzun.dev
```

Create frontend subdomains:

```text
tamias.juantzun.dev
tamias-prod.juantzun.dev
```

Each one should point to its corresponding Vercel project.

Optional backend subdomains can be added later:

```text
api-tamias.juantzun.dev
api-tamias-prod.juantzun.dev
```

For the MVP, using the Render `onrender.com` backend URLs is acceptable.

---

## Data isolation rule

Never share production data resources with testing.

Keep isolated:

```text
DATABASE_URL
AWS_S3_BUCKET
CHROMA_COLLECTION_NAME or Chroma instance
JWT_SECRET
CORS_ALLOWED_ORIGINS
```

Testing/demo can break.

Production real should remain clean and safe.

---

## Secrets rule

Never commit real secrets.

Real values must live in:

```text
GitHub repository secrets
Render environment variables
Vercel environment variables
Railway environment variables
Supabase settings
AWS IAM/S3
```

Use `.env.example` only for placeholders and naming reference.

---

## Related docs

```text
DEPLOYMENT.md
docs/17-environments-testing-production.md
docs/18-frontend-build-time-api-url.md
docs/19-docker-production-readiness.md
docs/20-docker-hub-image-publishing.md
docs/22-deployment-runbook.md
docs/23-environment-variables-checklist.md
```
