# TAMIAS — Diseño DevOps y Despliegue MVP

Este documento define el diseño DevOps y la estrategia de despliegue para el MVP de TAMIAS.

Para el runbook completo de despliegue, ver:

```text
DEPLOYMENT.md
docs/21-deployment-dual-environments.md
docs/22-deployment-runbook.md
docs/23-environment-variables-checklist.md
```

---

## 1. Objetivo

El objetivo DevOps del MVP es permitir que TAMIAS pueda desarrollarse localmente, probarse automáticamente y desplegarse en infraestructura real con una configuración clara y reproducible.

El MVP debe demostrar:

```text
Docker
Docker Compose
GitHub Actions CI
Docker Hub image publishing
Frontend en Vercel
Backend en Render
PostgreSQL en Supabase
Archivos en AWS S3
Chroma/IA en Railway
Dominio en Cloudflare
Dos ambientes aislados
```

---

## 2. Arquitectura de despliegue MVP

```text
User Browser
    |
    | HTTPS
    v
Vercel Frontend
    |
    | HTTPS REST API
    v
Render Backend - Spring Boot
    |
    | JDBC
    v
Supabase PostgreSQL

Render Backend
    |
    | AWS SDK
    v
AWS S3

Render Backend
    |
    | HTTP / Spring AI
    v
OpenAI API

Render Backend
    |
    | HTTP
    v
Railway Chroma
```

---

## 3. Plataformas definidas

| Componente | Plataforma |
|---|---|
| Frontend Angular | Vercel |
| Backend Spring Boot | Render |
| Base de datos PostgreSQL | Supabase |
| Archivos | AWS S3 |
| Vector store / Chroma | Railway |
| IA Chat/Embeddings | OpenAI |
| Dominio | Cloudflare / `juantzun.dev` |
| Repositorio | GitHub |
| CI/CD | GitHub Actions |
| Docker images | Docker Hub |

---

## 4. Ambientes

### 4.1 Local

```text
Frontend: http://localhost:4200
Backend: http://localhost:8080
PostgreSQL: localhost:5434
Chroma: http://localhost:8000
```

### 4.2 Prod-like local Docker

```text
Frontend: http://localhost:8088
Backend: http://localhost:8080
PostgreSQL: localhost:5434
Chroma: http://localhost:8000
```

Command:

```bash
docker compose -f docker-compose.prod-like.yml up --build
```

### 4.3 Testing / Portfolio

```text
Frontend: https://tamias.juantzun.dev
Backend:  https://tamias-api-testing.onrender.com
DB:       Supabase testing
Files:    S3 bucket tamias-testing-files
Chroma:   tamias_testing_documents or testing Chroma instance
```

### 4.4 Production real

```text
Frontend: https://tamias-prod.juantzun.dev
Backend:  https://tamias-api-prod.onrender.com
DB:       Supabase production
Files:    S3 bucket tamias-prod-files
Chroma:   tamias_prod_documents or production Chroma instance
```

---

## 5. Estrategia de despliegue inicial

Durante el MVP, ambos ambientes pueden desplegar desde la misma rama:

```text
main
  -> GitHub Actions CI
  -> Docker Hub images
  -> Vercel testing
  -> Render testing
  -> Vercel production
  -> Render production
```

Esto mantiene ambos ambientes actualizados con los últimos cambios.

Trade-off:

```text
Cada merge a main también actualiza producción.
```

Cuando producción tenga uso real más crítico, mover a:

```text
main        -> testing
release tag -> production
```

o:

```text
main       -> testing
production -> production
```

---

## 6. Configuración recomendada por ambiente

### Testing / Portfolio

```text
Vercel project: tamias-testing
Domain: tamias.juantzun.dev
Render service: tamias-api-testing
CORS_ALLOWED_ORIGINS=https://tamias.juantzun.dev
AWS_S3_BUCKET=tamias-testing-files
CHROMA_COLLECTION_NAME=tamias_testing_documents
FRONTEND_API_BASE_URL=https://tamias-api-testing.onrender.com/api/v1
```

### Production real

```text
Vercel project: tamias-production
Domain: tamias-prod.juantzun.dev
Render service: tamias-api-prod
CORS_ALLOWED_ORIGINS=https://tamias-prod.juantzun.dev
AWS_S3_BUCKET=tamias-prod-files
CHROMA_COLLECTION_NAME=tamias_prod_documents
FRONTEND_API_BASE_URL=https://tamias-api-prod.onrender.com/api/v1
```

---

## 7. Frontend Vercel settings

Use two Vercel projects.

Both use:

```text
Root Directory: frontend
Build Command: npm run build:prod
Output Directory: dist/tamias-frontend/browser
```

Each project must define its own:

```text
FRONTEND_API_BASE_URL
```

---

## 8. Backend Render settings

Use two Render web services.

Recommended for MVP:

```text
Repository: Juanjho120/tamias
Branch: main
Root Directory: backend
Runtime: Docker
Dockerfile Path: Dockerfile
Auto Deploy: true
```

Each Render service must define its own environment variables.

---

## 9. Docker Hub

CI publishes:

```text
<DOCKERHUB_USERNAME>/tamias-backend:latest
<DOCKERHUB_USERNAME>/tamias-backend:sha-<short-sha>
<DOCKERHUB_USERNAME>/tamias-frontend:latest
<DOCKERHUB_USERNAME>/tamias-frontend:sha-<short-sha>
```

Docker Hub images are not required for Vercel.

For Render MVP, prefer building from the repository Dockerfile to keep auto-deploy simple.

---

## 10. Cloudflare

Create frontend DNS records:

```text
tamias.juantzun.dev
tamias-prod.juantzun.dev
```

Each domain must point to its corresponding Vercel project.

---

## 11. Environment variables

For the full checklist, see:

```text
docs/23-environment-variables-checklist.md
```

---

## 12. Deployment runbook

For exact steps, see:

```text
docs/22-deployment-runbook.md
```
