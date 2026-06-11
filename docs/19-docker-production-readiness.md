# TAMIAS — Bloque 8B: Docker Production Readiness

## Scope

This block prepares Docker images for TAMIAS without performing a real deployment.

It adds production-ready Docker support for:

```text
Backend Spring Boot
Frontend Angular + Nginx
Prod-like local compose stack
CI Docker image validation
```

---

## Important decision

Even though Vercel does not require a frontend Docker image, TAMIAS will still keep the frontend dockerized.

Reason:

```text
The frontend Docker image provides portability, local prod-like validation, and CI confidence.
```

Vercel remains the recommended hosting platform for the deployed frontend, but the frontend Docker image must continue to build successfully in CI.

---

## Files added or changed

```text
backend/Dockerfile
backend/.dockerignore
frontend/Dockerfile
frontend/.dockerignore
frontend/nginx/templates/default.conf.template
docker-compose.prod-like.yml
.github/workflows/ci.yml
docs/19-docker-production-readiness.md
```

---

## Backend Docker image

The backend Dockerfile uses a multi-stage build:

```text
Build stage:   eclipse-temurin:21-jdk-alpine
Runtime stage: eclipse-temurin:21-jre-alpine
```

It builds the Spring Boot jar with:

```bash
./mvnw -B clean package -DskipTests
```

Runtime:

```text
non-root user
port 8080
java -jar /app/app.jar
```

---

## Frontend Docker image

The frontend Dockerfile uses:

```text
Build stage:   node:22-alpine
Runtime stage: nginx:1.27-alpine
```

It builds Angular with:

```bash
npm run build:prod
```

and uses the build arg:

```text
FRONTEND_API_BASE_URL
```

For the prod-like Docker setup, this is set to:

```text
/api/v1
```

Then Nginx proxies `/api/` to the backend container.

---

## Nginx proxy behavior

The frontend container serves Angular static files.

It also proxies:

```text
/api/*
```

to:

```text
BACKEND_API_URL
```

In `docker-compose.prod-like.yml`:

```text
BACKEND_API_URL=http://backend:8080
```

This allows the browser to call:

```text
http://localhost:8088/api/v1/...
```

while Nginx forwards the request to:

```text
http://backend:8080/api/v1/...
```

---

## Prod-like local compose

Run:

```bash
docker compose -f docker-compose.prod-like.yml up --build
```

URLs:

```text
Frontend: http://localhost:8088
Backend:  http://localhost:8080
Postgres: localhost:5434
Chroma:   http://localhost:8000
```

Stop:

```bash
docker compose -f docker-compose.prod-like.yml down
```

Remove volumes:

```bash
docker compose -f docker-compose.prod-like.yml down -v
```

---

## Required external variables for full functionality

The prod-like compose file has placeholders for:

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
AWS_REGION
AWS_S3_BUCKET
OPENAI_API_KEY
```

Without real values:

```text
Basic app startup can work.
S3 upload/download features may fail.
AI/RAG features may fail.
```

This is expected.

For full validation, export the real testing values before running compose.

PowerShell example:

```powershell
$env:AWS_ACCESS_KEY_ID="..."
$env:AWS_SECRET_ACCESS_KEY="..."
$env:AWS_REGION="us-east-2"
$env:AWS_S3_BUCKET="tamias-testing-files"
$env:OPENAI_API_KEY="..."
docker compose -f docker-compose.prod-like.yml up --build
```

Git Bash example:

```bash
AWS_ACCESS_KEY_ID=... \
AWS_SECRET_ACCESS_KEY=... \
AWS_REGION=us-east-2 \
AWS_S3_BUCKET=tamias-testing-files \
OPENAI_API_KEY=... \
docker compose -f docker-compose.prod-like.yml up --build
```

---

## CI Docker validation

The CI workflow now validates:

```text
backend Docker image build
frontend Docker image build
```

The frontend Docker build uses:

```bash
docker build --build-arg FRONTEND_API_BASE_URL=/api/v1 -t tamias-frontend:ci ./frontend
```

This guarantees the frontend remains dockerized, even though Vercel deploys from GitHub source.

---

## Build images manually

Backend:

```bash
docker build -t tamias-backend:local ./backend
```

Frontend:

```bash
docker build --build-arg FRONTEND_API_BASE_URL=/api/v1 -t tamias-frontend:local ./frontend
```

---

## Run images manually

Backend only:

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL="jdbc:postgresql://host.docker.internal:5434/tamias" \
  -e DATABASE_USERNAME=tamias \
  -e DATABASE_PASSWORD=tamias \
  -e JWT_SECRET="change-me-change-me-change-me-change-me-change-me-change-me-change-me-change-me" \
  -e CORS_ALLOWED_ORIGINS="http://localhost:8088" \
  -e AWS_REGION=us-east-2 \
  -e AWS_S3_BUCKET=tamias-testing-files \
  -e OPENAI_API_KEY=placeholder \
  -e CHROMA_HOST=host.docker.internal \
  -e CHROMA_PORT=8000 \
  -e CHROMA_BASE_URL=http://host.docker.internal:8000 \
  -e CHROMA_COLLECTION_NAME=tamias_prod_like_documents \
  tamias-backend:local
```

Frontend only:

```bash
docker run --rm -p 8088:80 \
  -e BACKEND_API_URL=http://host.docker.internal:8080 \
  tamias-frontend:local
```

---

## Deployment note

### Vercel

Vercel still deploys the frontend from source.

Use:

```text
Build command: npm run build:prod
```

The Docker frontend image is for:

```text
CI validation
local prod-like testing
portability
future hosting options
```

### Render

Render can deploy the backend using:

```text
backend/Dockerfile
```

Recommended Render settings:

```text
Root Directory: backend
Dockerfile Path: Dockerfile
Port: 8080
SPRING_PROFILES_ACTIVE=prod
```

---

## Next step

Recommended next block:

```text
Bloque 8C: Deployment documentation with dual environments
```
