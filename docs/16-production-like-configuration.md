# TAMIAS — Bloque 8A: Production-like Configuration

## Scope

This block prepares TAMIAS for a production-like environment without performing a real deployment yet.

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

The MVP deployment target remains:

```text
Frontend Angular        -> Vercel
Backend Spring Boot     -> Render
PostgreSQL              -> Supabase PostgreSQL
Files                   -> AWS S3
Chroma / Vector Store   -> Railway
OpenAI                  -> OpenAI API
Domain                  -> Cloudflare / tamias.juantzun.dev
```

---

## Files changed

```text
backend/src/main/resources/application.yaml
backend/src/main/resources/application-prod.yml
frontend/src/environments/environment.prod.ts
.env.example
docs/16-production-like-configuration.md
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
Production can activate prod with SPRING_PROFILES_ACTIVE=prod.
```

This avoids accidentally hardcoding the local profile in deployed environments.

---

## Backend production profile

The new file:

```text
backend/src/main/resources/application-prod.yml
```

uses environment variables for:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
JWT_EXPIRATION_MINUTES
CORS_ALLOWED_ORIGINS
AWS_REGION
AWS_S3_BUCKET
OPENAI_API_KEY
OPENAI_CHAT_MODEL
OPENAI_EMBEDDING_MODEL
CHROMA_HOST
CHROMA_PORT
CHROMA_BASE_URL
CHROMA_COLLECTION_NAME
```

The backend still uses:

```text
ddl-auto: validate
Flyway enabled
UTC database timezone
S3 storage provider
health/info actuator endpoints
```

---

## Frontend production API URL

The production frontend environment now points to:

```ts
apiBaseUrl: 'https://tamias-api.onrender.com/api/v1'
```

Before the real deployment, replace this value with the real Render backend URL if it is different.

For example:

```ts
apiBaseUrl: 'https://<your-real-render-service>.onrender.com/api/v1'
```

If you later add a reverse proxy or Vercel rewrites, this can be changed again.

---

## Root .env.example

The root `.env.example` documents all expected production-like variables.

Do not put real values in this file.

Real values must be configured in:

```text
Render environment variables
Vercel environment variables, if needed
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
PostgreSQL: localhost:5434
Chroma: localhost:8000
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

When deployment happens in a later block, validate:

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
Vercel config
Render blueprint
Railway service config
```

Those belong to later deployment blocks.

Recommended next step:

```text
Bloque 8B: Docker production readiness
```
