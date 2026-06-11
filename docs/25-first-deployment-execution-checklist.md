# TAMIAS — Bloque 8E: First Deployment Execution Checklist

## Scope

This document is the execution checklist for the first real TAMIAS deployment.

It does not add infrastructure-as-code.

It does not create resources automatically.

It is intended to be used manually while creating:

```text
Testing / Portfolio environment
Production real environment
```

---

## Environments

| Environment | Purpose | Frontend |
|---|---|---|
| Testing / Portfolio | Public demo and portfolio | `https://tamias.juantzun.dev` |
| Production real | Real vacation rental operations | `https://tamias-prod.juantzun.dev` |

---

## Before starting

Confirm the latest repo state is committed and pushed.

```bash
git status
git branch
git log --oneline -5
```

Expected:

```text
Working tree clean
Current branch main
Latest deployment docs committed
```

---

# Phase 0 — Preflight validation

## 0.1 Local backend build

```bash
cd backend
./mvnw.cmd clean package -DskipTests
```

Expected:

```text
BUILD SUCCESS
```

## 0.2 Local frontend build

```bash
cd frontend
npm run build
```

Expected:

```text
Build succeeds
dist/tamias-frontend/browser generated
```

## 0.3 Docker backend image

```bash
docker build -t tamias-backend:local ./backend
```

Expected:

```text
Image builds successfully
```

## 0.4 Docker frontend image

```bash
docker build --build-arg FRONTEND_API_BASE_URL=/api/v1 -t tamias-frontend:local ./frontend
```

Expected:

```text
Image builds successfully
```

## 0.5 Prod-like compose

```bash
docker compose -f docker-compose.prod-like.yml up --build
```

Open:

```text
http://localhost:8088
```

Expected:

```text
Frontend loads
Backend starts
No restart loop
```

Stop:

```bash
docker compose -f docker-compose.prod-like.yml down
```

## 0.6 GitHub CI

Go to:

```text
GitHub -> Actions -> TAMIAS CI
```

Expected:

```text
backend-build passes
frontend-build passes
docker-build passes
Docker Hub push passes on main
```

## 0.7 Docker Hub

Expected repositories:

```text
tamias-backend
tamias-frontend
```

Expected tags:

```text
latest
sha-<short-sha>
```

---

# Phase 1 — Supabase

## 1.1 Create testing database/project

Create:

```text
tamias-testing
```

Collect:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
```

Convert URL to JDBC format if needed:

```text
jdbc:postgresql://<host>:5432/<database>?sslmode=require
```

Checklist:

```text
[ ] Supabase testing project/database created
[ ] JDBC URL collected
[ ] Username collected
[ ] Password collected
[ ] Credentials saved in private password manager
```

## 1.2 Create production database/project

Create:

```text
tamias-production
```

Collect:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
```

Checklist:

```text
[ ] Supabase production project/database created
[ ] JDBC URL collected
[ ] Username collected
[ ] Password collected
[ ] Credentials saved in private password manager
[ ] Confirm production DB is not the same as testing DB
```

---

# Phase 2 — AWS S3

## 2.1 Create testing bucket

Create:

```text
tamias-testing-files
```

Recommended region:

```text
us-east-2
```

Checklist:

```text
[ ] Bucket created
[ ] Bucket is private
[ ] Public access blocked
[ ] IAM credentials available
[ ] IAM permissions limited to required bucket
```

## 2.2 Create production bucket

Create:

```text
tamias-prod-files
```

Recommended region:

```text
us-east-2
```

Checklist:

```text
[ ] Bucket created
[ ] Bucket is private
[ ] Public access blocked
[ ] IAM credentials available
[ ] IAM permissions limited to required bucket
[ ] Confirm production bucket is not the same as testing bucket
```

## 2.3 Minimum IAM permissions

Minimum actions:

```text
s3:PutObject
s3:GetObject
s3:DeleteObject
s3:HeadObject
```

Scope them to:

```text
arn:aws:s3:::tamias-testing-files/*
arn:aws:s3:::tamias-prod-files/*
```

or create separate credentials per environment if desired.

---

# Phase 3 — Chroma

## 3.1 Decide Chroma isolation

Choose one:

```text
[ ] Separate Chroma instances
[ ] Same Chroma instance with separate collections
```

Recommended safest option:

```text
Separate Chroma instances
```

Minimum MVP option:

```text
Same Chroma instance with separate collections
```

## 3.2 Testing Chroma

Testing collection:

```text
tamias_testing_documents
```

Variables:

```text
CHROMA_HOST=https://<testing-chroma-host-without-port>
CHROMA_PORT=8000
CHROMA_BASE_URL=https://<testing-chroma-url>
CHROMA_COLLECTION_NAME=tamias_testing_documents
```

Checklist:

```text
[ ] Testing Chroma service/collection ready
[ ] CHROMA_HOST includes protocol
[ ] CHROMA_HOST does not include port
[ ] CHROMA_BASE_URL includes full URL
```

## 3.3 Production Chroma

Production collection:

```text
tamias_prod_documents
```

Variables:

```text
CHROMA_HOST=https://<production-chroma-host-without-port>
CHROMA_PORT=8000
CHROMA_BASE_URL=https://<production-chroma-url>
CHROMA_COLLECTION_NAME=tamias_prod_documents
```

Checklist:

```text
[ ] Production Chroma service/collection ready
[ ] CHROMA_HOST includes protocol
[ ] CHROMA_HOST does not include port
[ ] CHROMA_BASE_URL includes full URL
[ ] Confirm production collection is not the same as testing collection
```

---

# Phase 4 — Render backend testing

Create Render Web Service:

```text
tamias-api-testing
```

Settings:

```text
Repository: Juanjho120/tamias
Branch: main
Root Directory: backend
Runtime: Docker
Dockerfile Path: Dockerfile
Auto Deploy: true
Health Check Path: /actuator/health
```

Use variables from:

```text
deploy/render/testing.env.example
```

Checklist:

```text
[ ] Render testing service created
[ ] Environment variables added
[ ] SPRING_PROFILES_ACTIVE=prod
[ ] CORS_ALLOWED_ORIGINS=https://tamias.juantzun.dev
[ ] AWS_S3_BUCKET=tamias-testing-files
[ ] CHROMA_COLLECTION_NAME=tamias_testing_documents
[ ] Deploy started
[ ] Deploy succeeded
[ ] Health check passes
```

Health URL:

```text
https://tamias-api-testing.onrender.com/actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

---

# Phase 5 — Render backend production

Create Render Web Service:

```text
tamias-api-prod
```

Settings:

```text
Repository: Juanjho120/tamias
Branch: main
Root Directory: backend
Runtime: Docker
Dockerfile Path: Dockerfile
Auto Deploy: true
Health Check Path: /actuator/health
```

Use variables from:

```text
deploy/render/production.env.example
```

Checklist:

```text
[ ] Render production service created
[ ] Environment variables added
[ ] SPRING_PROFILES_ACTIVE=prod
[ ] CORS_ALLOWED_ORIGINS=https://tamias-prod.juantzun.dev
[ ] AWS_S3_BUCKET=tamias-prod-files
[ ] CHROMA_COLLECTION_NAME=tamias_prod_documents
[ ] Deploy started
[ ] Deploy succeeded
[ ] Health check passes
[ ] Confirm production backend does not point to testing DB/S3/Chroma
```

Health URL:

```text
https://tamias-api-prod.onrender.com/actuator/health
```

Expected:

```json
{
  "status": "UP"
}
```

---

# Phase 6 — Vercel frontend testing

Create Vercel project:

```text
tamias-testing
```

Settings:

```text
Repository: Juanjho120/tamias
Branch: main
Root Directory: frontend
Build Command: npm run build:prod
Output Directory: dist/tamias-frontend/browser
```

Environment variable:

```text
FRONTEND_API_BASE_URL=https://tamias-api-testing.onrender.com/api/v1
```

Domain:

```text
tamias.juantzun.dev
```

Checklist:

```text
[ ] Vercel testing project created
[ ] Root Directory is frontend
[ ] Build command is npm run build:prod
[ ] Output directory is dist/tamias-frontend/browser
[ ] FRONTEND_API_BASE_URL points to testing backend
[ ] Deployment succeeds
[ ] Custom domain added
```

---

# Phase 7 — Vercel frontend production

Create Vercel project:

```text
tamias-production
```

Settings:

```text
Repository: Juanjho120/tamias
Branch: main
Root Directory: frontend
Build Command: npm run build:prod
Output Directory: dist/tamias-frontend/browser
```

Environment variable:

```text
FRONTEND_API_BASE_URL=https://tamias-api-prod.onrender.com/api/v1
```

Domain:

```text
tamias-prod.juantzun.dev
```

Checklist:

```text
[ ] Vercel production project created
[ ] Root Directory is frontend
[ ] Build command is npm run build:prod
[ ] Output directory is dist/tamias-frontend/browser
[ ] FRONTEND_API_BASE_URL points to production backend
[ ] Deployment succeeds
[ ] Custom domain added
[ ] Confirm production frontend does not point to testing backend
```

---

# Phase 8 — Cloudflare DNS

Use:

```text
deploy/cloudflare/dns-records.md
```

## 8.1 Testing frontend DNS

Record:

```text
Type: CNAME
Name: tamias
Target: <value-provided-by-vercel-for-tamias-testing>
```

Checklist:

```text
[ ] Vercel target copied exactly
[ ] DNS record created
[ ] Domain verified in Vercel
[ ] https://tamias.juantzun.dev loads
```

## 8.2 Production frontend DNS

Record:

```text
Type: CNAME
Name: tamias-prod
Target: <value-provided-by-vercel-for-tamias-production>
```

Checklist:

```text
[ ] Vercel target copied exactly
[ ] DNS record created
[ ] Domain verified in Vercel
[ ] https://tamias-prod.juantzun.dev loads
```

---

# Phase 9 — Smoke tests

Use:

```text
deploy/smoke-tests/tamias-smoke-tests.http
deploy/smoke-tests/smoke-test-urls.md
```

## 9.1 Testing smoke test

Checklist:

```text
[ ] Frontend loads login page
[ ] No console errors related to API base URL
[ ] Backend health returns UP
[ ] Login works
[ ] Dashboard analytics loads
[ ] Properties list loads
[ ] Catalogs load
[ ] Maintenance loads
[ ] Reservations load
[ ] Purchases load
[ ] Tasks load
[ ] Documents load
[ ] Property image upload works
[ ] Document upload works
[ ] Document download works
[ ] Document processing works
[ ] AI assistant answers using testing documents
```

## 9.2 Production smoke test

Checklist:

```text
[ ] Frontend loads login page
[ ] No console errors related to API base URL
[ ] Backend health returns UP
[ ] Login works
[ ] Dashboard analytics loads
[ ] Properties list loads
[ ] Catalogs load
[ ] Maintenance loads
[ ] Reservations load
[ ] Purchases load
[ ] Tasks load
[ ] Documents load
[ ] Property image upload works
[ ] Document upload works
[ ] Document download works
[ ] Document processing works
[ ] AI assistant answers using production documents
```

---

# Phase 10 — Cross-environment safety validation

Checklist:

```text
[ ] Testing frontend calls tamias-api-testing only
[ ] Production frontend calls tamias-api-prod only
[ ] Testing backend CORS allows only https://tamias.juantzun.dev
[ ] Production backend CORS allows only https://tamias-prod.juantzun.dev
[ ] Testing backend uses testing DB
[ ] Production backend uses production DB
[ ] Testing uploads go to tamias-testing-files
[ ] Production uploads go to tamias-prod-files
[ ] Testing Chroma collection is tamias_testing_documents
[ ] Production Chroma collection is tamias_prod_documents
[ ] JWT secret is different between environments
```

---

# Phase 11 — Save final deployment log

After successful deployment, fill:

```text
docs/26-deployment-log.md
```

Do not save secrets.

Save only:

```text
service names
public URLs
bucket names
collection names
deployment dates
smoke test results
known issues
```

---

# Phase 12 — Recommended immediate post-deploy tasks

After both environments are live:

```text
[ ] Create first production admin user securely
[ ] Create initial production organization
[ ] Upload first real production property data
[ ] Upload first test document in testing only
[ ] Confirm RAG isolation
[ ] Take screenshots for portfolio
[ ] Update portfolio project page with testing URL
```

---

## Stop condition

Do not put real vacation rental data in production until all of these pass:

```text
[ ] Production backend health is UP
[ ] Production frontend loads
[ ] Production login works
[ ] Production uploads work
[ ] Production AI/RAG works or is intentionally disabled
[ ] Production DB/S3/Chroma are isolated from testing
```
