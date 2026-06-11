# TAMIAS — Bloque 8D: Actual Deployment Preparation

## Scope

This block prepares TAMIAS for the first real deployment.

It does not create cloud resources.

It does not deploy the application.

It does not change runtime logic.

It adds deployment templates and smoke test files that can be used during the real deployment.

---

## Current confirmed foundation

The repository already has:

```text
Two-environment deployment docs
Production-like backend config
Frontend build-time API URL generation
Backend Dockerfile
Frontend Dockerfile
Prod-like Docker Compose
CI Docker image validation
Docker Hub image publishing in main CI
Deployment runbook
Environment variables checklist
```

The current deployment strategy is:

```text
Testing / Portfolio -> tamias.juantzun.dev
Production real     -> tamias-prod.juantzun.dev
```

---

## Files added

```text
deploy/README.md
deploy/render/testing.env.example
deploy/render/production.env.example
deploy/vercel/testing.env.example
deploy/vercel/production.env.example
deploy/cloudflare/dns-records.md
deploy/smoke-tests/tamias-smoke-tests.http
deploy/smoke-tests/smoke-test-urls.md
docs/24-actual-deployment-preparation.md
```

---

## Why this block exists

Before creating real resources, it is safer to have:

```text
exact environment variable templates
exact Vercel variable templates
DNS notes
smoke test URLs
cross-environment safety checks
```

This reduces mistakes during deployment.

---

## Render templates

Templates:

```text
deploy/render/testing.env.example
deploy/render/production.env.example
```

Use them to create the environment variables for:

```text
tamias-api-testing
tamias-api-prod
```

Important:

```text
Do not paste the example placeholders as real values.
Replace them in Render only.
Do not commit real secrets.
```

---

## Vercel templates

Templates:

```text
deploy/vercel/testing.env.example
deploy/vercel/production.env.example
```

Testing:

```text
FRONTEND_API_BASE_URL=https://tamias-api-testing.onrender.com/api/v1
```

Production:

```text
FRONTEND_API_BASE_URL=https://tamias-api-prod.onrender.com/api/v1
```

Both Vercel projects must use:

```text
Root Directory: frontend
Build Command: npm run build:prod
Output Directory: dist/tamias-frontend/browser
```

---

## Cloudflare notes

DNS file:

```text
deploy/cloudflare/dns-records.md
```

Create records only after Vercel provides the target values.

Do not guess the CNAME target.

Expected domains:

```text
tamias.juantzun.dev
tamias-prod.juantzun.dev
```

---

## Smoke tests

Files:

```text
deploy/smoke-tests/tamias-smoke-tests.http
deploy/smoke-tests/smoke-test-urls.md
```

Use `tamias-smoke-tests.http` with:

```text
VS Code REST Client
JetBrains HTTP Client
```

Minimum checks:

```text
health
login
dashboard analytics
properties
documents
file upload/download
AI/RAG
cross-environment isolation
```

---

## Real deployment order

Use this order:

```text
1. Confirm GitHub CI passes.
2. Confirm Docker Hub images are published.
3. Create Supabase testing database.
4. Create Supabase production database.
5. Create S3 testing bucket.
6. Create S3 production bucket.
7. Create Chroma testing resource.
8. Create Chroma production resource.
9. Create Render testing backend.
10. Create Render production backend.
11. Create Vercel testing frontend.
12. Create Vercel production frontend.
13. Configure Cloudflare DNS.
14. Run testing smoke tests.
15. Run production smoke tests.
16. Save final deployment URLs in docs.
```

---

## Decision: no render.yaml yet

This block intentionally does not add:

```text
render.yaml
vercel.json
```

Reason:

```text
Manual dashboard setup is safer for the first deployment because TAMIAS has two environments with different secrets and resources.
```

After the first successful deployment, infrastructure-as-code can be added if needed.

---

## Production safety checklist

Before putting real vacation rental data in production:

```text
[ ] Production DB is separate from testing DB
[ ] Production S3 bucket is separate from testing bucket
[ ] Production Chroma collection/instance is separate from testing
[ ] Production JWT secret is different from testing
[ ] Production CORS only allows https://tamias-prod.juantzun.dev
[ ] Production Vercel FRONTEND_API_BASE_URL points to tamias-api-prod
[ ] Testing Vercel FRONTEND_API_BASE_URL points to tamias-api-testing
[ ] Testing frontend cannot access production backend
[ ] Production frontend cannot access testing backend
```

---

## Next recommended block

```text
Bloque 8E: First deployment execution checklist
```

That block should not add many files.

It should guide the actual manual creation of the resources one by one.
