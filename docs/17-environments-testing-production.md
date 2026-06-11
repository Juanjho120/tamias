# TAMIAS — Bloque 8A-2: Dual Environment Configuration

## Scope

This document defines the dual-environment deployment strategy for TAMIAS.

This block does not deploy anything yet.

It documents how TAMIAS should be configured for:

```text
Testing / Portfolio
Production real
```

---

## Environments

| Environment | Purpose | Frontend domain |
|---|---|---|
| Testing / Portfolio | Public portfolio/demo environment | `https://tamias.juantzun.dev` |
| Production real | Real usage for vacation rental properties | `https://tamias-prod.juantzun.dev` |

---

## Recommended platform layout

Use separate platform resources per environment.

| Component | Testing / Portfolio | Production real |
|---|---|---|
| Frontend | Vercel project: `tamias-testing` | Vercel project: `tamias-production` |
| Backend | Render service: `tamias-api-testing` | Render service: `tamias-api-prod` |
| PostgreSQL | Supabase project/db: `tamias-testing` | Supabase project/db: `tamias-production` |
| Files | S3 bucket: `tamias-testing-files` | S3 bucket: `tamias-prod-files` |
| Chroma | `tamias_testing_documents` collection or separate instance | `tamias_prod_documents` collection or separate instance |
| OpenAI | Same account/API key allowed, but monitor usage | Same account/API key allowed, but monitor usage |
| DNS | `tamias.juantzun.dev` | `tamias-prod.juantzun.dev` |

---

## Branch/deployment strategy

Initial MVP strategy:

```text
main branch
  -> deploys testing frontend
  -> deploys testing backend
  -> deploys production frontend
  -> deploys production backend
```

This keeps both environments updated with the latest changes.

Important trade-off:

```text
Every commit merged to main will also reach production.
```

This is acceptable for early MVP if you want both environments updated.

Later, when production has real operational usage, recommended strategy:

```text
main branch       -> testing / portfolio
release tags      -> production
or production branch -> production
```

---

## Why two Vercel projects

Two Vercel projects are recommended because each environment needs its own:

```text
custom domain
backend API URL
deployment variables
deployment history
access/logs separation
```

This avoids mixing the portfolio/demo environment with the real environment used for vacation rentals.

---

## Why two Render services

Two Render services are recommended because each backend needs isolated:

```text
DATABASE_URL
CORS_ALLOWED_ORIGINS
AWS_S3_BUCKET
CHROMA_COLLECTION_NAME
JWT_SECRET
logs
health checks
deploy history
```

Both services can point to the same GitHub repo and branch.

---

## Why separate databases

Testing/demo data must not mix with real production data.

Use:

```text
Testing DB    -> fake/demo/portfolio data
Production DB -> real vacation rental data
```

This protects real business data and allows you to freely test features in the portfolio environment.

---

## Why separate S3 buckets

Use different buckets:

```text
tamias-testing-files
tamias-prod-files
```

Benefits:

```text
No accidental mixing of documents/images
Safer cleanup of test files
Easier IAM auditing
Clearer backup strategy
```

---

## Chroma separation

Recommended:

```text
Testing:    tamias_testing_documents
Production: tamias_prod_documents
```

Even better, use separate Chroma instances if Railway cost/ops allows it.

Reason:

```text
RAG answers must never retrieve production documents from the testing environment, or testing documents from production.
```

---

## Backend variables per environment

Both Render services should use the same variable names but different values.

### Testing Render service

```text
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=<testing-supabase-jdbc-url>
DATABASE_USERNAME=<testing-db-user>
DATABASE_PASSWORD=<testing-db-password>
JWT_SECRET=<testing-jwt-secret>
CORS_ALLOWED_ORIGINS=https://tamias.juantzun.dev
AWS_REGION=us-east-2
AWS_S3_BUCKET=tamias-testing-files
OPENAI_API_KEY=<openai-api-key>
CHROMA_BASE_URL=<testing-chroma-url>
CHROMA_HOST=<testing-chroma-host>
CHROMA_PORT=8000
CHROMA_COLLECTION_NAME=tamias_testing_documents
```

### Production Render service

```text
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=<production-supabase-jdbc-url>
DATABASE_USERNAME=<production-db-user>
DATABASE_PASSWORD=<production-db-password>
JWT_SECRET=<production-jwt-secret>
CORS_ALLOWED_ORIGINS=https://tamias-prod.juantzun.dev
AWS_REGION=us-east-2
AWS_S3_BUCKET=tamias-prod-files
OPENAI_API_KEY=<openai-api-key>
CHROMA_BASE_URL=<production-chroma-url>
CHROMA_HOST=<production-chroma-host>
CHROMA_PORT=8000
CHROMA_COLLECTION_NAME=tamias_prod_documents
```

---

## Frontend API URL strategy

Important:

```text
Angular environment files are build-time.
```

That means `environment.prod.ts` is baked into the frontend build.

For two Vercel projects deploying from the same branch, each project must still produce a build with its own backend API URL.

### Recommended MVP approach

Generate `environment.prod.ts` during the Vercel build using an environment variable.

Each Vercel project should define:

```text
FRONTEND_API_BASE_URL
```

Example:

```text
Testing Vercel project:
FRONTEND_API_BASE_URL=https://tamias-api-testing.onrender.com/api/v1

Production Vercel project:
FRONTEND_API_BASE_URL=https://tamias-api-prod.onrender.com/api/v1
```

Then a later block can add a small script such as:

```text
frontend/scripts/write-prod-env.js
```

and change the Vercel build command to:

```text
node scripts/write-prod-env.js && npm run build
```

This allows both Vercel projects to use the same repository and branch while producing different API URLs.

### Alternative

Use runtime config:

```text
/assets/config.json
```

The app loads the API URL at startup.

This is more flexible but requires a slightly bigger frontend change.

For TAMIAS MVP, prefer the build-time script.

---

## DNS plan

Cloudflare DNS should have:

```text
tamias.juantzun.dev      -> CNAME to testing Vercel project
tamias-prod.juantzun.dev -> CNAME to production Vercel project
```

Do not point both domains to the same Vercel project unless you intentionally want them to use the same API configuration.

---

## Data safety rule

Never reuse the same production resources for testing/demo.

Avoid sharing:

```text
Production database
Production S3 bucket
Production Chroma collection
Production JWT secret
```

Testing can break. Production should not.

---

## Recommended next steps

1. Keep `application-prod.yml` shared.
2. Keep both backends using `SPRING_PROFILES_ACTIVE=prod`.
3. Add two Vercel projects.
4. Add two Render services.
5. Add separate Supabase/S3/Chroma resources.
6. In a later block, add frontend build-time API URL generation.

Recommended next block:

```text
Bloque 8B: Docker production readiness
```

Then:

```text
Bloque 8C: Deployment documentation with dual environments
```

Then:

```text
Bloque 8D: Actual deployment
```
