# TAMIAS

![CI](https://github.com/Juanjho120/tamias/actions/workflows/ci.yml/badge.svg)

TAMIAS is a full-stack SaaS platform for owners and managers of small lodging properties. It helps centralize daily operations such as properties, reservations, maintenance, tasks, purchases, inventory, documents, payments, images, and AI-assisted operational queries.

The project is designed as a portfolio-grade, production-oriented application with a Java/Spring Boot backend, Angular frontend, PostgreSQL database, AWS S3 private file storage, Chroma vector search, and an AI assistant named **TAMI**.

## Live environments

| Environment | Purpose | URL |
|---|---|---|
| Testing / Portfolio | Public demo and portfolio environment | <https://tamias.juantzun.dev> |
| Production real | Real operational environment | Documented in `DEPLOYMENT.md` |

## Current status

TAMIAS is MVP-ready and actively evolving.

Completed major blocks include:

- SaaS security foundation and organization scoping.
- Properties, catalogs, maintenance, reservations, tasks, purchases, documents, inventory and payments.
- Private S3-backed image/document storage with presigned URLs.
- TAMI AI assistant with RAG, read-only tool calling, operational summaries and debug traces.
- Product Box Models with Three.js rendering and OpenCV-based texture processing.
- Organization administration, multi-organization switching and global UX polish.
- Pre-Reports operational polish through the maintenance last-by-person AI tool.

Next planned phase:

```text
Phase 17 — Reports
```

## Main features

### Property operations

- Property management.
- Reservation tracking.
- Guest supplies and reservation-related inventory usage.
- Task management.
- Dashboard and calendar views.

### Maintenance

- Maintenance record management.
- Scheduled maintenance.
- Maintenance categories and types.
- Maintenance people/responsible tracking.
- Materials used and serviced items tracking.
- Before, after and general maintenance images.
- AI queries such as latest maintenance by person.

### Inventory and purchases

- Inventory item catalog.
- Brand support for inventory items.
- Purchase lists and purchase images.
- Operational usage tracking across maintenance, reservations and purchases.

### Documents and files

- Document upload and storage in private S3.
- PDF and Office document parsing support.
- RAG indexing through Chroma.
- Organization-scoped document search.
- Hard-delete lifecycle for RAG documents and entity images.

### Payments

- Manual payment registry for operational expenses.
- Payment categories.
- Payment methods and property association.
- Payment receipt images.
- AI read-only awareness for payment summaries and search.

TAMIAS does **not** process real payments, charge cards, store card numbers, store bank account numbers or execute transfers.

### TAMI AI assistant

TAMI is the application assistant. It supports:

- Document search and RAG-based answers.
- Read-only operational tools.
- Organization-scoped answers.
- Tool routing for properties, catalogs, inventory, maintenance, reservations, purchases, payments, documents, images and product boxes.
- Debug traces for users with AI debug permissions.
- Guardrails to avoid write/action requests.

### Product Box Models

TAMIAS includes a 3D Product Box module for reconstructing rectangular product/package boxes using real dimensions and face textures.

Implemented capabilities include:

- Product box metadata stored in PostgreSQL.
- Private face images stored in S3.
- Three.js viewer in Angular.
- OpenCV Java perspective correction.
- Manual corner selection and automatic contour detection helper.
- Optional AI-enhancement metadata and workflow preparation.
- Runtime controls for environments where OpenCV should be disabled.

## Tech stack

### Backend

- Java 21
- Spring Boot 3
- Spring Security + JWT
- Spring Data JPA / Hibernate
- Flyway
- PostgreSQL
- Spring AI
- OpenAI
- Chroma vector store
- AWS SDK for S3
- OpenCV Java
- PDFBox
- Apache POI
- Maven
- Docker

### Frontend

- Angular 19
- TypeScript
- Bootstrap 5
- Bootstrap Icons
- RxJS
- ngx-translate
- Three.js
- SCSS

### Infrastructure

- PostgreSQL / Supabase
- AWS S3
- Chroma
- Render backend hosting
- Vercel frontend hosting
- Cloudflare DNS
- GitHub Actions CI
- Docker / Docker Compose

## Repository structure

```text
.
├── backend/                  # Spring Boot API
├── frontend/                 # Angular application
├── docs/                     # Architecture, roadmap and phase documentation
├── deploy/                   # Deployment templates and smoke-test helpers
├── .github/workflows/        # CI pipeline
├── docker-compose.yml        # Local PostgreSQL + Chroma
├── docker-compose.prod-like.yml
├── DEPLOYMENT.md             # Deployment entry point
└── README.md
```

## Local development

### Requirements

- Java 21
- Node.js 22+
- npm
- Docker Desktop
- Git
- AWS S3 credentials for file/image flows
- OpenAI API key for AI features

### 1. Start local infrastructure

From the repository root:

```bash
docker compose up -d postgres chroma
```

This starts:

```text
PostgreSQL: localhost:5434
Chroma:     localhost:8000
```

Default local database credentials:

```text
Database: tamias
User:     tamias
Password: tamias
```

### 2. Configure backend environment variables

For local development, the backend profile is `local` by default. Export the required values before starting the backend.

Linux/macOS:

```bash
export OPENAI_API_KEY="your-openai-api-key"
export AWS_ACCESS_KEY_ID="your-aws-access-key-id"
export AWS_SECRET_ACCESS_KEY="your-aws-secret-access-key"
export AWS_REGION="us-east-2"
export AWS_S3_BUCKET="your-local-or-dev-bucket"
export PRODUCT_BOX_OPENCV_ENABLED="true"
```

Windows PowerShell:

```powershell
$env:OPENAI_API_KEY="your-openai-api-key"
$env:AWS_ACCESS_KEY_ID="your-aws-access-key-id"
$env:AWS_SECRET_ACCESS_KEY="your-aws-secret-access-key"
$env:AWS_REGION="us-east-2"
$env:AWS_S3_BUCKET="your-local-or-dev-bucket"
$env:PRODUCT_BOX_OPENCV_ENABLED="true"
```

For low-memory environments, Product Box OpenCV processing can be disabled:

```bash
PRODUCT_BOX_OPENCV_ENABLED=false
```

### 3. Run the backend

Linux/macOS:

```bash
cd backend
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Backend URL:

```text
http://localhost:8080/api/v1
```

Swagger/OpenAPI UI, when enabled by the running profile:

```text
http://localhost:8080/swagger-ui/index.html
```

### 4. Run the frontend

In a second terminal:

```bash
cd frontend
npm install
npm start
```

Frontend URL:

```text
http://localhost:4200
```

The local Angular environment points to:

```text
http://localhost:8080/api/v1
```

## Prod-like local run

A full prod-like stack is available through Docker Compose:

```bash
docker compose -f docker-compose.prod-like.yml up --build
```

Default exposed URLs:

```text
Frontend: http://localhost:8088
Backend:  http://localhost:8080
Postgres: localhost:5434
Chroma:   localhost:8000
```

Before using the prod-like stack with real AI and S3 features, provide the required environment variables such as `OPENAI_API_KEY`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION` and `AWS_S3_BUCKET`.

## Useful commands

### Backend

```bash
cd backend
./mvnw clean package
./mvnw test
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd clean package
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm start
npm run build
npm run build:prod
npm test
```

### Docker

```bash
docker compose up -d postgres chroma
docker compose down
docker compose -f docker-compose.prod-like.yml up --build
```

## Configuration reference

Main configuration files:

```text
.env.example
backend/src/main/resources/application.yaml
backend/src/main/resources/application-local.yml
backend/src/main/resources/application-prod.yml
frontend/src/environments/environment.ts
frontend/src/environments/environment.prod.ts
```

Important backend environment variables:

| Variable | Purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Runtime profile, usually `local`, `ci` or `prod` |
| `PORT` | Backend server port |
| `DATABASE_URL` | JDBC PostgreSQL URL for production |
| `DATABASE_USERNAME` | PostgreSQL username |
| `DATABASE_PASSWORD` | PostgreSQL password |
| `JWT_SECRET` | JWT signing secret |
| `JWT_EXPIRATION_MINUTES` | JWT expiration window |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origins |
| `AWS_ACCESS_KEY_ID` | AWS access key for S3 |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key for S3 |
| `AWS_REGION` | AWS region |
| `AWS_S3_BUCKET` | S3 bucket name |
| `OPENAI_API_KEY` | OpenAI key for TAMI / embeddings |
| `OPENAI_CHAT_MODEL` | Chat model name |
| `OPENAI_EMBEDDING_MODEL` | Embedding model name |
| `CHROMA_BASE_URL` | Chroma base URL |
| `CHROMA_COLLECTION_NAME` | Chroma collection |
| `PRODUCT_BOX_OPENCV_ENABLED` | Enables/disables Product Box OpenCV processing |

Frontend production builds can use:

```text
FRONTEND_API_BASE_URL
```

## Database

TAMIAS uses PostgreSQL with Flyway migrations.

Migration folder:

```text
backend/src/main/resources/db/migration
```

Local database:

```text
jdbc:postgresql://localhost:5434/tamias
```

Production deployments should use managed PostgreSQL/Supabase and must keep testing/demo data isolated from real operational data.

## File storage

TAMIAS stores files and images in private S3 buckets.

Rules documented in the project decisions:

- Store files by organization, module and entity id.
- Keep `s3_key` as the relative key used by S3 operations.
- Do not store image/document binaries in PostgreSQL.
- Use presigned URLs for file previews/downloads.
- Hard-delete entity image rows only after successful S3 deletion.

## AI and vector search

TAMI uses:

- OpenAI for chat and embeddings.
- Chroma for vector storage.
- PostgreSQL for metadata and operational data.
- Controlled backend read-only tools for structured operational questions.

RAG tuning should not be used to compensate for broken structured-tool routing. AI-related changes should be validated against the smoke-test documentation before moving on.

## CI/CD

The repository includes a GitHub Actions workflow that:

- Builds the backend with Java 21 and Maven.
- Builds the Angular frontend with Node 22.
- Builds Docker images for backend and frontend.
- Publishes Docker images to Docker Hub on pushes to `main` when Docker Hub secrets are configured.

Workflow:

```text
.github/workflows/ci.yml
```

## Deployment

Deployment documentation starts here:

```text
DEPLOYMENT.md
```

Detailed deployment docs include:

```text
docs/21-deployment-dual-environments.md
docs/22-deployment-runbook.md
docs/23-environment-variables-checklist.md
docs/24-actual-deployment-preparation.md
docs/25-first-deployment-execution-checklist.md
docs/26-deployment-log.md
docs/27-first-deployment-troubleshooting.md
```

Deployment strategy:

- Backend: Render service built from `backend/Dockerfile`.
- Frontend: Vercel Angular static deployment.
- Database: Supabase/PostgreSQL.
- Files: AWS S3.
- Vector search: Chroma.
- DNS: Cloudflare.

## Documentation map

Start with:

```text
docs/ROADMAP.md
docs/DECISIONS.md
DEPLOYMENT.md
```

Important phase documents:

```text
docs/71-product-box-models-14.md
docs/93-organization-administration-global-ux-15.md
docs/105-ai-read-only-tool-support-decomposition-15f.md
docs/106-payments-16.md
docs/110-pre-reports-operational-polish.md
```

Future phase documents:

```text
docs/107-reports-17.md
docs/108-notifications-reminders-18.md
docs/109-blueprint-analysis-19.md
```

## Security notes

- The frontend must not be trusted as the source of organization isolation.
- Organization scoping is enforced in the backend from authenticated user context.
- AI tools are read-only and must use controlled domain queries.
- No free-form SQL execution is exposed to TAMI.
- Administrative tools require authorization checks.
- S3 objects remain private and are accessed through presigned URLs.
- Real secrets must never be committed.

## Author

Built by **Juan Tzun** as a full-stack operational platform and portfolio project.
