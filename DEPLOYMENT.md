# TAMIAS — Deployment Guide

This document is the entry point for deploying TAMIAS.

TAMIAS has two deployed environments:

| Environment | Purpose | Frontend domain |
|---|---|---|
| Testing / Portfolio | Public portfolio/demo environment | `https://tamias.juantzun.dev` |
| Production real | Real environment for vacation rental operations | `https://tamias-prod.juantzun.dev` |

## Recommended deployment order

1. Confirm GitHub CI and Docker Hub publishing works.
2. Create Supabase databases.
3. Create AWS S3 buckets and IAM credentials.
4. Create Railway Chroma service or collections.
5. Create Render backend services.
6. Create Vercel frontend projects.
7. Configure Cloudflare DNS.
8. Run smoke tests.
9. Save final environment URLs in documentation.

## Detailed docs

Read these documents in order:

```text
docs/21-deployment-dual-environments.md
docs/22-deployment-runbook.md
docs/23-environment-variables-checklist.md
docs/24-actual-deployment-preparation.md
```

## Deployment preparation kit

Use the templates in:

```text
deploy/
```

Important files:

```text
deploy/render/testing.env.example
deploy/render/production.env.example
deploy/vercel/testing.env.example
deploy/vercel/production.env.example
deploy/cloudflare/dns-records.md
deploy/smoke-tests/tamias-smoke-tests.http
deploy/smoke-tests/smoke-test-urls.md
```

## Quick environment map

```text
Testing / Portfolio
  Frontend: https://tamias.juantzun.dev
  Backend:  https://tamias-api-testing.onrender.com
  S3:       tamias-testing-files
  Chroma:   tamias_testing_documents

Production real
  Frontend: https://tamias-prod.juantzun.dev
  Backend:  https://tamias-api-prod.onrender.com
  S3:       tamias-prod-files
  Chroma:   tamias_prod_documents
```

## Backend hosting recommendation

For the initial MVP, use Render building from the GitHub repository Dockerfile.

Reason:

```text
Render can auto-deploy from GitHub when the backend Dockerfile changes.
Docker Hub images are still published by CI for portability and release traceability.
```

Docker Hub images can be used later for manual SHA-based deployments.

## Frontend hosting recommendation

Use two Vercel projects:

```text
tamias-testing
tamias-production
```

Both point to the same GitHub repository and branch.

Each project must define its own:

```text
FRONTEND_API_BASE_URL
```

and use:

```bash
npm run build:prod
```

Output directory:

```text
dist/tamias-frontend/browser
```
