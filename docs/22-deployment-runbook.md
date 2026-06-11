# TAMIAS — Deployment Runbook

## Purpose

This runbook gives the exact order to configure TAMIAS in real infrastructure.

Use this document when setting up:

```text
Testing / Portfolio
Production real
```

For copy/paste templates, use:

```text
deploy/
docs/24-actual-deployment-preparation.md
```

---

# 1. GitHub repository secrets

Go to:

```text
GitHub repo
Settings
Secrets and variables
Actions
Repository secrets
```

Create:

```text
DOCKERHUB_USERNAME
DOCKERHUB_TOKEN
```

`DOCKERHUB_TOKEN` must be a Docker Hub access token, not your password.

Validate after pushing to `main`:

```text
GitHub Actions -> TAMIAS CI
Docker Hub -> tamias-backend / tamias-frontend
```

Expected image tags:

```text
latest
sha-<short-sha>
```

---

# 2. Supabase PostgreSQL

Create two separate Supabase projects or databases:

```text
tamias-testing
tamias-production
```

Testing is for demo/portfolio data.

Production is for real vacation rental data.

For each environment, collect:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
```

Use JDBC URL format in Render:

```text
jdbc:postgresql://<host>:5432/<database>?sslmode=require
```

Do not use the same database for both environments.

---

# 3. AWS S3

Create two private S3 buckets:

```text
tamias-testing-files
tamias-prod-files
```

Recommended region:

```text
us-east-2
```

Create or reuse IAM credentials with minimum required permissions:

```text
s3:PutObject
s3:GetObject
s3:DeleteObject
s3:HeadObject
```

Limit permissions to the TAMIAS buckets.

Environment variables needed by Render:

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
AWS_REGION
AWS_S3_BUCKET
S3_PRESIGNED_URL_EXPIRATION_SECONDS
```

---

# 4. Railway Chroma

Choose one of these options.

## Option A — Separate Chroma instances

Recommended if cost/ops is acceptable:

```text
chroma-testing
chroma-production
```

## Option B — Same Chroma instance, separate collections

Minimum option:

```text
tamias_testing_documents
tamias_prod_documents
```

Render backend variables:

Testing:

```text
CHROMA_HOST=https://<testing-chroma-host-without-port>
CHROMA_PORT=8000
CHROMA_BASE_URL=https://<testing-chroma-url>
CHROMA_COLLECTION_NAME=tamias_testing_documents
```

Production:

```text
CHROMA_HOST=https://<production-chroma-host-without-port>
CHROMA_PORT=8000
CHROMA_BASE_URL=https://<production-chroma-url>
CHROMA_COLLECTION_NAME=tamias_prod_documents
```

Important:

```text
CHROMA_HOST must include protocol.
CHROMA_HOST must not include the port.
CHROMA_BASE_URL must include protocol and port/path when required.
```

---

# 5. Render backend services

Create two Render web services:

```text
tamias-api-testing
tamias-api-prod
```

Recommended source:

```text
GitHub repository
```

Settings:

```text
Repository: Juanjho120/tamias
Branch: main
Root Directory: backend
Runtime: Docker
Dockerfile Path: Dockerfile
Auto Deploy: true
```

Health check path:

```text
/actuator/health
```

Runtime environment variable:

```text
SPRING_PROFILES_ACTIVE=prod
```

Use templates:

```text
deploy/render/testing.env.example
deploy/render/production.env.example
```

Testing backend URL target:

```text
https://tamias-api-testing.onrender.com/api/v1
```

Production backend URL target:

```text
https://tamias-api-prod.onrender.com/api/v1
```

---

# 6. Vercel frontend projects

Create two Vercel projects.

## Testing / Portfolio

```text
Project name: tamias-testing
Repository: Juanjho120/tamias
Branch: main
Root Directory: frontend
Build Command: npm run build:prod
Output Directory: dist/tamias-frontend/browser
Domain: tamias.juantzun.dev
```

Environment variable:

```text
FRONTEND_API_BASE_URL=https://tamias-api-testing.onrender.com/api/v1
```

Template:

```text
deploy/vercel/testing.env.example
```

## Production real

```text
Project name: tamias-production
Repository: Juanjho120/tamias
Branch: main
Root Directory: frontend
Build Command: npm run build:prod
Output Directory: dist/tamias-frontend/browser
Domain: tamias-prod.juantzun.dev
```

Environment variable:

```text
FRONTEND_API_BASE_URL=https://tamias-api-prod.onrender.com/api/v1
```

Template:

```text
deploy/vercel/production.env.example
```

---

# 7. Cloudflare DNS

In Cloudflare:

```text
Domain: juantzun.dev
DNS -> Records
```

Use:

```text
deploy/cloudflare/dns-records.md
```

Create records based on the values Vercel gives you.

Expected records:

```text
tamias.juantzun.dev
tamias-prod.juantzun.dev
```

Each must point to its own Vercel project.

Do not point both domains to the same Vercel project.

---

# 8. Smoke tests

Use:

```text
deploy/smoke-tests/tamias-smoke-tests.http
deploy/smoke-tests/smoke-test-urls.md
```

Run these after deploying each environment.

## Backend health

Testing:

```text
GET https://tamias-api-testing.onrender.com/actuator/health
```

Production:

```text
GET https://tamias-api-prod.onrender.com/actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

## Core checks

Validate at minimum:

```text
Login
Dashboard analytics
Properties
Catalogs
Maintenance
Reservations
Purchases
Tasks
Documents
AI Assistant
File upload/download
```

---

# 9. Rollback strategy

## Frontend Vercel rollback

Use Vercel deployment history.

Rollback the affected project only:

```text
tamias-testing
tamias-production
```

## Backend Render rollback

If deploying from GitHub/Dockerfile:

```text
Revert commit
Push to main
Render redeploys automatically
```

If later deploying from Docker Hub image:

```text
Redeploy a known sha-<short-sha> image
```

## Database rollback

Use Supabase backups.

Do not rely only on app-level rollback when migrations have already run.

---

# 10. Production safety before real use

Before using `tamias-prod.juantzun.dev` for real vacation rental operations, confirm:

```text
Production database is separate
Production S3 bucket is separate
Production Chroma collection or instance is separate
Production JWT secret is different
Production CORS only allows production frontend
Testing frontend cannot access production backend
Production frontend cannot access testing backend
```

---

# 11. Final deployment checklist

```text
[ ] GitHub CI passes
[ ] Docker Hub images published
[ ] Supabase testing created
[ ] Supabase production created
[ ] S3 testing bucket created
[ ] S3 production bucket created
[ ] Chroma testing ready
[ ] Chroma production ready
[ ] Render testing backend deployed
[ ] Render production backend deployed
[ ] Vercel testing frontend deployed
[ ] Vercel production frontend deployed
[ ] Cloudflare DNS configured
[ ] Testing smoke test passed
[ ] Production smoke test passed
```
