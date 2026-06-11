# TAMIAS

**TAMIAS** is a full-stack SaaS-style platform for owners and managers of small lodging properties such as vacation homes, apartments, cabins, villas, and bungalows.

The project helps centralize daily operations that are often scattered across notebooks, spreadsheets, WhatsApp messages, folders, and disconnected tools.

## Live Environments

| Environment | Purpose | URL |
|---|---|---|
| Testing / Portfolio | Public demo and portfolio environment | https://tamias.juantzun.dev |
| Production real | Real usage environment for vacation rental operations | https://tamias-prod.juantzun.dev |

> Note: the backend may take a few seconds to respond after inactivity because the current hosting plan can have cold starts.

## Project Purpose

TAMIAS is being built both as a real product and as a main portfolio project.

The goal is to demonstrate practical experience with:

- Full-stack application architecture.
- Java 21 and Spring Boot 3 backend development.
- Angular frontend development.
- PostgreSQL database design.
- SaaS-style multi-tenancy.
- JWT authentication and role-based access control.
- AWS S3 file storage.
- AI-powered document search using RAG.
- Docker-based local and production-like validation.
- CI/CD with GitHub Actions.
- Real deployment using Vercel, Render, Supabase, Railway, AWS, Docker Hub, and Cloudflare.

## Problem TAMIAS Solves

Small lodging property managers often need to track:

- Maintenance history.
- Scheduled maintenance.
- Repair costs.
- Reservations.
- Guest-related preparation.
- Task lists.
- Purchase lists.
- Inventory items.
- Reservation supplies.
- Important documents, property rules, manuals, and policies.

Without a centralized system, this information can become hard to audit, search, and maintain.

TAMIAS provides a centralized operational platform for small lodging businesses.

## Current MVP Features

The current MVP includes:

- Authentication.
- Organizations.
- Users and roles.
- Administrator user management.
- Self-service profile management.
- Mandatory temporary password change.
- Properties.
- Catalogs.
- Inventory items.
- Maintenance records.
- Maintenance record items.
- Maintenance images.
- Scheduled maintenance.
- Scheduled maintenance history.
- Reservations.
- Guests.
- Reservation supplies.
- Task lists.
- Purchase lists.
- Purchase items.
- Document management.
- AWS S3 file storage.
- AI document processing.
- AI document indexing with Chroma.
- AI document search using RAG.
- AI chat sessions.
- Dashboard calendar.
- Dashboard analytics.
- Dual-environment deployment.

## User Access Model

TAMIAS separates administrator user management from self-service profile management.

### User Management

The `/users` screen is intended for administrators.

Administrators can:

- List organization users.
- Create users with a temporary password.
- Assign roles.
- Update role and status.
- Activate users.
- Deactivate users.
- Delete users.

### My Profile

The `/profile` screen is available to every authenticated user.

Users can:

- Update their first name.
- Update their last name.
- Change their own password.

### Mandatory Temporary Password Change

When an administrator creates a new user, the initial password is considered temporary.

The backend stores this state using:

```text
password_change_required
```

If `passwordChangeRequired = true`, the frontend forces the user to change the password before continuing to the rest of the application.

## Tech Stack

### Frontend

- Angular.
- TypeScript.
- Bootstrap.
- Bootstrap Icons.
- Angular Reactive Forms.
- FullCalendar.
- RxJS.
- ngx-translate.

### Backend

- Java 21.
- Spring Boot 3.
- Spring Security.
- JWT authentication.
- Spring Data JPA.
- Hibernate.
- Flyway.
- Swagger/OpenAPI.

### Database

- PostgreSQL.
- Supabase PostgreSQL for deployed environments.
- Flyway migrations.

### File Storage

- AWS S3.
- Private buckets.
- Pre-signed URLs.

### Artificial Intelligence

- Spring AI.
- OpenAI.
- Chroma.
- RAG.
- Embeddings.
- AI chat sessions.

### DevOps and Deployment

- Docker.
- Docker Compose.
- GitHub Actions.
- Docker Hub image publishing.
- Vercel frontend hosting.
- Render backend hosting.
- Railway Chroma hosting.
- Supabase PostgreSQL.
- AWS S3.
- Cloudflare DNS.

## Architecture

TAMIAS starts as a **modular monolith**.

It is deployed as a single Spring Boot backend application, but internally organized by business modules.

```text
Angular Frontend
    |
    | REST API + JWT
    v
Spring Boot Backend
    |
    | JPA / Hibernate
    v
PostgreSQL

Spring Boot Backend
    |
    | AWS SDK
    v
AWS S3

Spring Boot Backend
    |
    | Spring AI
    v
OpenAI + Chroma
```

## SaaS and Multi-Tenancy

TAMIAS is designed as a SaaS-style product.

The multi-tenant model is:

```text
Shared database + shared schema + organization_id
```

Operational data is isolated by organization through the `organization_id` column.

Important rule:

> The backend must always resolve the current organization from the authenticated user context. The frontend must not be trusted as the source of truth for `organization_id`.

## Initial Roles

| Role | Description |
|---|---|
| Administrator | Full access within the organization, including user management |
| Property Manager | Manages daily operations |
| Maintenance Staff | Handles assigned maintenance and tasks |
| Read Only | Can view information but cannot modify it |

Every authenticated role can access `/profile` to update personal data and change password.

## Deployment Summary

TAMIAS currently has two validated environments.

| Component | Testing / Portfolio | Production real |
|---|---|---|
| Frontend | `https://tamias.juantzun.dev` | `https://tamias-prod.juantzun.dev` |
| Backend | `https://tamias-api-testing.onrender.com` | `https://tamias-api-prod.onrender.com` |
| Database | Supabase testing | Supabase production |
| Files | `tamias-testing-files` | `tamias-prod-files` |
| Chroma | `tamias_testing_documents` | `tamias_prod_documents` |

Deployment status:

```text
Testing / Portfolio: LIVE / VALIDATED
Production real:     LIVE / VALIDATED
```

Validated:

- Login in both environments.
- User creation in both environments.
- S3 isolation.
- Chroma isolation.
- AI/RAG document search.
- Environment-specific backend routing.
- No crossed data between environments.

## Known Limitations

Current known limitations:

- Render free-tier cold starts can make the first backend request slow after inactivity.
- Production currently auto-deploys from `main`; a release-tag or production-branch strategy may be added later.
- Initial admin bootstrap is currently manual and should be formalized later.
- Automated test coverage should be expanded.
- AI tool calling over controlled PostgreSQL domain tools is planned but not part of the current MVP.

See:

```text
docs/30-known-limitations.md
```

## Local Development

Start local dependencies:

```bash
docker compose up -d
```

Stop local dependencies:

```bash
docker compose down
```

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Run tests:

```bash
./mvnw test
```

Build:

```bash
./mvnw clean package
```

### Frontend

```bash
cd frontend
npm install
npm start
```

Run tests:

```bash
npm test
```

Build:

```bash
npm run build
```

Production-style frontend build:

```bash
FRONTEND_API_BASE_URL=https://tamias-api-testing.onrender.com/api/v1 npm run build:prod
```

### Prod-like Docker Stack

```bash
docker compose -f docker-compose.prod-like.yml up --build
```

Frontend:

```text
http://localhost:8088
```

Stop:

```bash
docker compose -f docker-compose.prod-like.yml down
```

## Environment Variables

Main backend variables:

```text
SPRING_PROFILES_ACTIVE
PORT
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
JWT_EXPIRATION_MINUTES
CORS_ALLOWED_ORIGINS
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
AWS_REGION
AWS_S3_BUCKET
S3_PRESIGNED_URL_EXPIRATION_SECONDS
OPENAI_API_KEY
OPENAI_CHAT_MODEL
OPENAI_EMBEDDING_MODEL
OPENAI_TEMPERATURE
CHROMA_HOST
CHROMA_PORT
CHROMA_BASE_URL
CHROMA_COLLECTION_NAME
AI_DEFAULT_TOP_K
AI_DEFAULT_SIMILARITY_THRESHOLD
MAX_FILE_SIZE
MAX_REQUEST_SIZE
```

Main frontend variable:

```text
FRONTEND_API_BASE_URL
```

Secrets must never be committed to the repository.

## Security Principles

TAMIAS follows these security principles:

- JWT-based authentication.
- Role-based access control.
- Organization-based data isolation.
- Backend-enforced permissions.
- Private S3 buckets.
- Pre-signed URLs for file access.
- Mandatory password change for temporary passwords.
- Self-service password changes for authenticated users.
- No secrets in frontend code.
- No secrets committed to GitHub.
- No unrestricted SQL execution from AI.
- Environment-specific CORS.

## Documentation

| Document | Purpose |
|---|---|
| [`docs/01-architecture-mvp.md`](docs/01-architecture-mvp.md) | Architecture and MVP scope |
| [`docs/02-database-design-mvp.md`](docs/02-database-design-mvp.md) | Database design |
| [`docs/03-api-design-mvp.md`](docs/03-api-design-mvp.md) | REST API design |
| [`docs/04-backend-design-mvp.md`](docs/04-backend-design-mvp.md) | Spring Boot backend design |
| [`docs/05-frontend-design-mvp.md`](docs/05-frontend-design-mvp.md) | Angular frontend design |
| [`docs/06-ai-design-mvp.md`](docs/06-ai-design-mvp.md) | AI and RAG design |
| [`docs/07-devops-deployment-mvp.md`](docs/07-devops-deployment-mvp.md) | DevOps and deployment design |
| [`DEPLOYMENT.md`](DEPLOYMENT.md) | Deployment guide |
| [`docs/26-deployment-log.md`](docs/26-deployment-log.md) | Deployment log |
| [`docs/28-portfolio-stabilization.md`](docs/28-portfolio-stabilization.md) | Stabilization plan |
| [`docs/29-screenshot-checklist.md`](docs/29-screenshot-checklist.md) | Portfolio screenshot checklist |
| [`docs/30-known-limitations.md`](docs/30-known-limitations.md) | Known limitations and follow-ups |

## Current Project Status

```text
MVP deployed and validated in testing and production environments.
```

Next recommended phases:

1. Portfolio screenshots and project page.
2. Automated test coverage expansion.
3. Production monitoring and paid backend instance evaluation.
4. Release-tag based production deployments.
5. AI tool calling over controlled read-only domain tools.
6. PDF reporting and advanced analytics.

## License

This project is currently intended as a personal portfolio and product development project.

License details will be defined later.
