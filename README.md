# TAMIAS

**TAMIAS** is a SaaS platform designed to help owners and managers of small lodging properties organize and control their daily operations.

The product is focused on small-scale rental businesses such as:

- Vacation homes
- Apartments
- Bungalows
- Cabins
- Villas

TAMIAS centralizes property operations including maintenance, scheduled maintenance, reservations, task lists, purchase lists, inventory items, reservation supplies, important documents, reporting foundations, user access management, self-service profile management, mandatory temporary password changes, and AI-assisted document search.

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
- User and profile management
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
- No simple way to manage user access and profile updates

TAMIAS solves this by providing a centralized platform for managing key operational data.

---

## Core MVP Features

The current MVP includes:

- Authentication
- Organizations
- Users and roles
- User management for administrators
- Self-service profile management
- Mandatory temporary password change
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

## User Access Model

TAMIAS separates user administration from self-service profile management.

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

The backend stores this using:

```text
password_change_required
```

If `passwordChangeRequired = true`, the frontend must force the user to change the password before continuing to the rest of the application.

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
| Administrator | Full access within the organization, including user management |
| Property Manager | Manages daily operations |
| Maintenance Staff | Handles assigned maintenance and tasks |
| Read Only | Can view information but cannot modify it |

Every authenticated role can access `/profile` to update personal data and change password.

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
- User management
- Self-service profile
- Mandatory temporary password change
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

## Security Principles

TAMIAS follows these security principles:

- JWT-based authentication
- Role-based access control
- Organization-based data isolation
- Backend-enforced permissions
- Private S3 buckets
- Pre-signed URLs for file access
- Mandatory password change for temporary passwords
- Self-service password changes for authenticated users
- No secrets in frontend code
- No secrets committed to GitHub
- No unrestricted SQL execution from AI
- CORS restricted by environment

---

## License

This project is currently intended as a personal portfolio and product development project. License details will be defined later.
