# TAMIAS — Bloque 8A: Production-like Configuration

## Scope

This block prepares TAMIAS for production-like environments without performing a real deployment yet.

It does not deploy to:

```text
Vercel
Render
Supabase
Railway
AWS
Cloudflare
```

It only adds and aligns configuration files.

---

## Deployment targets

TAMIAS will use two deployed environments.

| Environment | Purpose | Frontend domain |
|---|---|---|
| Testing / Portfolio | Public demo and portfolio environment | `https://tamias.juantzun.dev` |
| Production real | Real environment for vacation rental operations | `https://tamias-prod.juantzun.dev` |

The platform targets remain:

```text
Frontend Angular        -> Vercel
Backend Spring Boot     -> Render
PostgreSQL              -> Supabase PostgreSQL
Files                   -> AWS S3
Chroma / Vector Store   -> Railway
OpenAI                  -> OpenAI API
Domain                  -> Cloudflare / juantzun.dev
```

---

## Files changed

```text
backend/src/main/resources/application.yaml
backend/src/main/resources/application-prod.yml
frontend/src/environments/environment.prod.ts
.env.example
docs/16-production-like-configuration.md
docs/17-environments-testing-production.md
```

---

## Important application.yaml change

Previous behavior:

```yaml
spring:
  profiles:
    active: local
```

Production-like behavior:

```yaml
spring:
  profiles:
    default: local
```

Why:

```text
Local remains the default when no profile is provided.
Production-like deployed environments can activate prod with SPRING_PROFILES_ACTIVE=prod.
```

This avoids accidentally hardcoding the local profile in deployed environments.

---

## Backend production profile

The file:

```text
backend/src/main/resources/application-prod.yml
```

is shared by both Testing and Production.

Both Render services can run:

```text
SPRING_PROFILES_ACTIVE=prod
```

The difference between environments comes from environment variables.

Testing example:

```text
CORS_ALLOWED_ORIGINS=https://tamias.juantzun.dev
AWS_S3_BUCKET=tamias-testing-files
CHROMA_COLLECTION_NAME=tamias_testing_documents
```

Production example:

```text
CORS_ALLOWED_ORIGINS=https://tamias-prod.juantzun.dev
AWS_S3_BUCKET=tamias-prod-files
CHROMA_COLLECTION_NAME=tamias_prod_documents
```

---

## Frontend production API URL

Important:

```text
Angular environment files are build-time.
```

For two Vercel projects using the same main branch, each project needs a different backend API URL.

Recommended MVP strategy:

```text
Generate frontend/src/environments/environment.prod.ts during the Vercel build using FRONTEND_API_BASE_URL.
```

Testing Vercel project:

```text
FRONTEND_API_BASE_URL=https://tamias-api-testing.onrender.com/api/v1
```

Production Vercel project:

```text
FRONTEND_API_BASE_URL=https://tamias-api-prod.onrender.com/api/v1
```

This will be implemented in a later block.

Until then, remember that the committed `environment.prod.ts` is only a placeholder/default and must not be treated as the final dual-environment solution.

---

## Root .env.example

The root `.env.example` documents:

```text
shared backend variable names
testing environment examples
production environment examples
frontend build-time API URL strategy
```

Do not put real values in this file.

Real values must be configured in:

```text
Render environment variables
Vercel environment variables
Railway variables
AWS IAM/S3
Supabase project settings
GitHub Actions secrets
```

---

## Local validation

### Backend local profile still works

```bash
cd backend
./mvnw.cmd spring-boot:run
```

Expected:

```text
Active profile: local
PostgreSQL local config
Chroma local config
```

### Backend prod profile validation

Use environment variables first, then run:

```bash
cd backend
set SPRING_PROFILES_ACTIVE=prod
./mvnw.cmd clean package -DskipTests
```

On PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
./mvnw.cmd clean package -DskipTests
```

Important:

```text
The build should compile without needing real external services.
Running the app with prod profile will require valid env vars.
```

### Frontend production build

```bash
cd frontend
npm run build
```

---

## Manual smoke test after deployment later

When deployment happens in a later block, validate in both environments:

```text
GET /actuator/health
POST /api/v1/auth/login
GET /api/v1/dashboard/analytics
Upload property image
Upload document
Generate document download URL
Process document
Ask AI over indexed documents
```

---

## Not included in this block

This block intentionally does not add or modify:

```text
Dockerfile
docker-compose.prod-like.yml
GitHub Actions deployment jobs
Vercel build script
Render blueprint
Railway service config
```

Those belong to later deployment blocks.

Recommended next step:

```text
Bloque 8B: Docker production readiness
```
