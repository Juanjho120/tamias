# TAMIAS — Portfolio Case Study

## Project Overview

**TAMIAS** is a full-stack SaaS-style platform for managing small lodging properties such as vacation homes, apartments, bungalows, cabins, and small rental units.

The platform centralizes daily operational tasks that are usually managed through disconnected tools like spreadsheets, WhatsApp messages, folders, printed notes, and manual reminders.

TAMIAS was built as both a real product idea and a portfolio project to demonstrate full-stack engineering, SaaS-style architecture, cloud deployment, file storage, and AI-assisted document search.

## Live Demo

```text
Testing / Portfolio:
https://tamias.juantzun.dev

Repository:
https://github.com/Juanjho120/tamias
```

> Note: the demo backend may take a few seconds to respond after inactivity because it is hosted on a free-tier backend instance.

## Problem

Small lodging property managers often need to track maintenance history, scheduled maintenance, reservations, guest preparation, task lists, purchase lists, inventory items, property documents, house rules, manuals, images, and receipts.

Without a centralized system, this information becomes scattered across different tools and hard to search later.

Examples:

```text
When was the water filter replaced?
What supplies are needed for a reservation?
Where is the maintenance evidence image?
What does the house rules document say about check-in?
When was an item last purchased?
```

TAMIAS solves this by bringing operations, files, documents, and AI-assisted search into one platform.

## Solution

TAMIAS provides a centralized web platform where users can manage:

```text
properties
maintenance records
scheduled maintenance
reservations
guests
reservation supplies
task lists
purchase lists
inventory items
documents
AI document search
users and roles
```

It also integrates AWS S3 for private file storage, OpenAI + Chroma for AI/RAG document search, Supabase PostgreSQL for deployed databases, Vercel and Render for deployment, and Docker/GitHub Actions for CI/CD validation.

## Main Features

### Authentication and User Management

TAMIAS includes JWT-based authentication and role-based access control.

Administrators can create users, assign roles, activate or deactivate users, and force temporary password changes.

Every authenticated user can update profile information and change their own password.

### Organization-Based Data Isolation

TAMIAS follows a SaaS-style model:

```text
Shared database + shared schema + organization_id
```

Operational data is scoped by organization. The backend resolves the active organization from the authenticated user context instead of trusting the frontend.

### Properties

Users can manage rental properties, including basic property information, property status, and property images.

Images are stored privately in AWS S3 and accessed through controlled URLs.

### Maintenance

The maintenance module supports maintenance records, maintenance categories, maintenance types, cost tracking, evidence images, and detail items.

### Scheduled Maintenance

Scheduled maintenance helps track recurring or future maintenance work, including next due dates, status, and history.

### Reservations

The reservation module supports reservation records, guest information, check-in/check-out dates, reservation status, and reservation supplies.

### Task Lists and Purchase Lists

TAMIAS includes task lists, task items, purchase lists, purchase items, and inventory items to help property managers prepare for guests and organize recurring work.

### Documents and AI Assistant

Users can upload documents such as house rules, property manuals, maintenance instructions, vendor notes, and policy documents.

The backend processes documents into chunks, indexes them in Chroma, and allows the AI assistant to answer questions using RAG.

Example:

```text
Question:
What is the testing keyword in the uploaded house rules document?

Answer:
The testing keyword is QUETZAL-TEST-123.
```

## Technical Architecture

TAMIAS starts as a modular monolith.

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

Deployment:

```text
GitHub -> GitHub Actions -> Docker Hub

Frontend:
GitHub -> Vercel -> Cloudflare domain

Backend:
GitHub/Docker -> Render

Database:
Supabase PostgreSQL

Vector Store:
Railway Chroma

Files:
AWS S3
```

## Tech Stack

### Frontend

```text
Angular
TypeScript
Bootstrap
Bootstrap Icons
RxJS
ngx-translate
FullCalendar
```

### Backend

```text
Java 21
Spring Boot 3
Spring Security
JWT
Spring Data JPA
Hibernate
Flyway
Swagger / OpenAPI
```

### Database

```text
PostgreSQL
Supabase PostgreSQL
Flyway migrations
```

### AI

```text
Spring AI
OpenAI
Chroma
RAG
Embeddings
```

### Storage

```text
AWS S3
Private buckets
Pre-signed URLs
```

### DevOps

```text
Docker
Docker Compose
GitHub Actions
Docker Hub
Vercel
Render
Railway
Supabase
Cloudflare
```

## Deployment Strategy

TAMIAS is deployed in two separated environments.

| Environment | Purpose | Frontend | Backend |
|---|---|---|---|
| Testing / Portfolio | Public demo and portfolio | `https://tamias.juantzun.dev` | `https://tamias-api-testing.onrender.com` |
| Production real | Real property operations | `https://tamias-prod.juantzun.dev` | `https://tamias-api-prod.onrender.com` |

Each environment has its own frontend project, backend service, database, S3 bucket, Chroma collection, environment variables, and CORS configuration.

Validated isolation:

```text
testing does not call production backend
production does not call testing backend
testing files do not go to production S3
production files do not go to testing S3
testing Chroma data does not mix with production Chroma data
users are separated by environment
```

## Technical Challenges

### 1. Environment Separation

One of the biggest challenges was separating testing and production across Vercel, Render, Supabase, AWS S3, Railway Chroma, Cloudflare, Docker Hub, and GitHub Actions.

This required environment-specific URLs, buckets, databases, collections, CORS origins, and frontend build variables.

### 2. Angular Production Environment Replacement

During deployment, the frontend initially called:

```text
http://localhost:8080/api/v1
```

from the deployed environment.

The issue was that the production environment file was generated correctly, but Angular was not replacing `environment.ts` with `environment.prod.ts`.

The fix was to configure Angular production `fileReplacements`.

### 3. Supabase Connection from Render

The backend initially failed to connect to Supabase from Render because the direct database host could resolve to an IPv6 path not available from the hosting environment.

The fix was to use Supabase Session Pooler for Render-hosted backend services.

### 4. Private File Storage with AWS S3

TAMIAS uses private S3 buckets and controlled file access.

This required environment-specific buckets, minimal IAM permissions, private bucket configuration, file upload flow, file download/view flow, and pre-signed URLs.

### 5. Document Processing and RAG

The AI assistant required a pipeline that could upload a document, extract text, split text into chunks, store chunks in PostgreSQL, index chunks into Chroma, search relevant chunks, send context to the AI model, and return grounded answers.

### 6. Docker and CI/CD

The project includes Docker support for both backend and frontend.

CI validates Docker builds and publishes images to Docker Hub.

The frontend is intentionally dockerized even though Vercel deploys from source. This keeps the project production-ready for future containerized hosting.

## Security Considerations

TAMIAS includes these security-focused decisions:

```text
JWT authentication
role-based access control
organization-based data filtering
backend-owned organization resolution
private S3 buckets
pre-signed file access
mandatory temporary password change
no secrets committed to GitHub
environment-specific CORS
no unrestricted SQL execution from AI
```

## What I Learned

This project strengthened my experience with designing SaaS-style data isolation, deploying full-stack applications across multiple cloud providers, handling production-like frontend environment variables, debugging real deployment issues, integrating private S3 file storage, building AI/RAG document search, validating cross-environment safety, dockerizing backend and frontend applications, publishing Docker images from GitHub Actions, and documenting deployment limitations.

## Current Status

```text
MVP deployed and validated.
```

Validated:

```text
login
user creation
dashboard
properties
documents
AI assistant
S3 storage
Chroma integration
testing/production isolation
deployment documentation
```

## Known Limitations

Current known limitations:

```text
Render free-tier cold starts can slow down the first backend request after inactivity.
Production currently auto-deploys from main.
Initial admin bootstrap is manual.
Automated test coverage should be expanded.
AI tool calling over PostgreSQL domain tools is planned but not implemented yet.
```

## Next Steps

Planned improvements:

```text
add more automated tests
add release-based production deployments
improve monitoring and alerting
formalize database backup/restore process
add richer analytics
add AI tool calling over controlled read-only domain tools
add more PDF/reporting capabilities
```

## Screenshots

Recommended screenshot sections:

```text
Login
Dashboard
Reservation calendar
Properties
Maintenance
Reservations
Task lists
Purchase lists
Documents
AI assistant
User management
GitHub Actions
Docker Hub
Vercel / Render deployments
```

Use:

```text
docs/33-portfolio-screenshot-placement.md
```

for suggested screenshot placement.

## Last Updated

```text
2026-06-11
```
