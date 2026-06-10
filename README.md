# TAMIAS

**TAMIAS** is a SaaS platform designed to help owners and managers of small lodging properties organize and control their daily operations.

The product is focused on small-scale rental businesses such as:

- Vacation homes
- Apartments
- Bungalows
- Cabins
- Villas

TAMIAS centralizes property operations including maintenance, scheduled maintenance, reservations, task lists, purchase lists, inventory items, reservation supplies, important documents, reporting foundations, and AI-assisted document search.

---

## Project Purpose

TAMIAS is being built as a real product and as a main portfolio project.

The goal is to demonstrate practical experience in:

- Full stack application architecture
- Java 21 and Spring Boot 3 backend development
- Angular frontend development
- PostgreSQL database design
- SaaS multi-tenancy
- JWT authentication and role-based access control
- AWS S3 file storage
- AI-powered document search using RAG
- Docker-based local development
- CI/CD with GitHub Actions
- Real deployment using Vercel, Render, Supabase, Railway, and AWS

---

## Problem TAMIAS Solves

Small lodging property managers often track operations manually across notebooks, spreadsheets, WhatsApp messages, folders, and disconnected tools.

This creates problems such as:

- Missing maintenance history
- No clear record of repair costs
- Disorganized reservations
- Forgotten tasks before guest check-in
- Purchase lists scattered across messages
- Important property rules and manuals hard to search
- No quick way to answer operational questions
- No centralized tracking of operational items and supplies

TAMIAS solves this by providing a centralized platform for managing key operational data.

---

## Core MVP Features

The current MVP includes:

- Authentication
- Organizations
- Users and roles
- Properties
- Catalogs
- Inventory Items
- Maintenance records
- Maintenance record items
- Maintenance images
- Scheduled maintenance
- Scheduled maintenance history
- Reservations
- Guests
- Reservation supplies
- Task lists
- Purchase lists
- Purchase items
- Document management
- AWS S3 file storage
- AI document processing
- AI document search using RAG
- AI chat sessions
- Dashboard calendar
- Basic deployment foundation

---

## Future Features

Planned future features include:

- MVP hardening and automated test coverage
- Advanced dashboard analytics
- Production-like deployment
- AI tool calling over controlled PostgreSQL domain tools
- Advanced notifications
- JasperReports PDF reporting
- Blueprint analysis with OCR and vision models
- AI agents by business domain
- Formal inventory stock control
- Platform integrations with Airbnb, Booking, and VRBO
- Billing and subscriptions

---

## Tech Stack

### Frontend

- Angular
- TypeScript
- Bootstrap
- Bootstrap Icons
- Angular Reactive Forms
- FullCalendar
- RxJS
- ngx-translate

### Backend

- Java 21
- Spring Boot 3
- Spring Security
- JWT Authentication
- Spring Data JPA
- Hibernate
- Flyway
- Swagger/OpenAPI

### Database

- PostgreSQL

### File Storage

- AWS S3
- Pre-signed URLs

### Artificial Intelligence

- Spring AI
- OpenAI
- Chroma
- RAG
- Embeddings
- Tool Calling, future phase
- AI Agents, future phase
- Ollama, optional local experimentation

### DevOps

- Docker
- Docker Compose
- GitHub Actions
- CI/CD

### Deployment

- Frontend: Vercel
- Backend: Render
- Database: Supabase PostgreSQL
- Vector Store / AI services: Railway
- Files: AWS S3
- Domain: `tamias.juantzun.dev`

---

## Architecture

TAMIAS starts as a **Modular Monolith**.

This means the system is deployed as a single Spring Boot backend application, but internally organized by business modules.

This approach was chosen because:

- It keeps the MVP simpler to build and deploy.
- It avoids unnecessary microservice complexity.
- It is easier to maintain during early product development.
- It is appropriate for the expected scale of approximately 5 simultaneous users per organization.
- It still allows clean separation by domain.

High-level architecture:

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
OpenAI / Chroma / Ollama
```

---

## SaaS and Multi-Tenancy

TAMIAS is designed as a SaaS product from the beginning.

The multi-tenant model is:

```text
Shared database + shared schema + organization_id
```

Operational data is isolated by organization through the `organization_id` column.

Important rule:

> The backend must always resolve the current organization from the authenticated user context. The frontend must not be trusted as the source of truth for `organization_id`.

---

## Initial Roles

| Role | Description |
|---|---|
| Administrator | Full access within the organization |
| Property Manager | Manages daily operations |
| Maintenance Staff | Handles assigned maintenance and tasks |
| Read Only | Can view information but cannot modify it |

---

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
| [`docs/08-inventory-items-and-reservation-supplies.md`](docs/08-inventory-items-and-reservation-supplies.md) | Inventory Items and Reservation Supplies design |
| [`docs/PROJECT_CONTEXT.md`](docs/PROJECT_CONTEXT.md) | Short project context |
| [`docs/ROADMAP.md`](docs/ROADMAP.md) | Project roadmap |
| [`docs/DECISIONS.md`](docs/DECISIONS.md) | Architecture and technical decisions |

---

## Current Project Status

```text
MVP feature implementation in progress.
Core operational modules are implemented.
Current focus: documentation sync, MVP hardening, dashboard analytics, deployment, and AI tool calling.
```

Completed or mostly completed:

- Backend foundation
- Frontend foundation
- Authentication
- Organizations and users
- Properties
- Catalogs
- Inventory Items
- Maintenance
- Scheduled maintenance
- Reservations
- Reservation Supplies
- Task Lists
- Purchase Lists
- Documents
- S3 integration
- RAG document search
- Dashboard calendar

Next recommended phases:

1. MVP hardening.
2. Automated tests and quality checks.
3. Dashboard analytics.
4. Production-like deployment.
5. AI tool calling with controlled read-only tools.

---

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

---

## Environment Variables

Main backend variables:

```text
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
OPENAI_API_KEY
OPENAI_CHAT_MODEL
OPENAI_EMBEDDING_MODEL
CHROMA_BASE_URL
CHROMA_COLLECTION_NAME
```

Secrets must never be committed to the repository.

---

## AI Assistant

The AI Assistant MVP focuses on document search using RAG.

The assistant answers questions such as:

- What do the house rules say about pets?
- Is smoking allowed?
- What is not allowed in the property?
- Where is the electrical panel located?
- What does the manual say about the water filter?

AI rules:

- Answers must be based on available documents.
- Answers must include sources when available.
- If information is not found, the assistant must say so clearly.
- The assistant must not invent property rules.
- The assistant must respect organization-level data isolation.
- SQL execution by the AI is not allowed in the MVP.

---

## Security Principles

TAMIAS follows these security principles:

- JWT-based authentication
- Role-based access control
- Organization-based data isolation
- Backend-enforced permissions
- Private S3 buckets
- Pre-signed URLs for file access
- No secrets in frontend code
- No secrets committed to GitHub
- No unrestricted SQL execution from AI
- CORS restricted by environment

---

## Database Principles

The MVP database follows these rules:

- Primary IDs use UUID.
- Operational entities include `organization_id`.
- Main tables include `created_at` and `updated_at`.
- Operational records may include `created_by` and `updated_by`.
- Important records use `status`.
- Critical operational records use soft delete.
- Unique constraints include `organization_id` where applicable.
- Flyway manages schema migrations.

---

## License

This project is currently intended as a personal portfolio and product development project. License details will be defined later.
