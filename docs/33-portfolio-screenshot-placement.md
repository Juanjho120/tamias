# TAMIAS — Portfolio Screenshot Placement Guide

## Purpose

Use this guide to decide where to place the screenshots captured from the testing/portfolio environment.

Recommended environment:

```text
https://tamias.juantzun.dev
```

## Suggested screenshot order

### 1. Hero section

Use:

```text
Dashboard overview
```

Suggested caption:

```text
TAMIAS dashboard with reservation calendar and operational analytics.
```

### 2. Authentication section

Use:

```text
Login page
Password change screen, if captured
```

Suggested caption:

```text
JWT-based authentication with support for temporary password change.
```

### 3. Property management section

Use:

```text
Properties list
Property image preview
Property create/edit modal
```

Suggested caption:

```text
Property management with image uploads stored through private AWS S3 storage.
```

### 4. Maintenance section

Use:

```text
Maintenance records
Maintenance detail
Scheduled maintenance
Maintenance evidence image
```

Suggested caption:

```text
Maintenance records and scheduled maintenance tracking for lodging operations.
```

### 5. Reservation operations section

Use:

```text
Reservation calendar
Reservations list
Reservation detail
Reservation supplies
Guests
```

Suggested caption:

```text
Reservation tracking with guest details and supplies needed for each stay.
```

### 6. Tasks and purchases section

Use:

```text
Task lists
Purchase lists
Inventory items
```

Suggested caption:

```text
Operational checklists and purchase planning for property preparation.
```

### 7. Documents and AI section

Use:

```text
Documents module
Document processing/indexing state
AI assistant answer over uploaded document
```

Suggested caption:

```text
AI-assisted document search using RAG, OpenAI, and Chroma.
```

This should be one of the strongest sections in the portfolio.

### 8. User management section

Use:

```text
Users list
Create user modal
Profile screen
```

Suggested caption:

```text
Administrator user management with role assignment and mandatory password change.
```

### 9. Technical deployment section

Use:

```text
GitHub Actions passing
Docker Hub backend/frontend images
Vercel deployments
Render services
```

Suggested caption:

```text
CI/CD, Docker image publishing, and dual-environment cloud deployment.
```

Avoid showing secrets, environment variable values, private database URLs, private API keys, or private tokens.

## Screenshot selection for a short portfolio card

Use only 3 screenshots:

```text
1. Dashboard overview
2. AI assistant answering from a document
3. GitHub Actions or deployment screenshot
```

## Screenshot selection for a full case study

Use 8 to 12 screenshots:

```text
1. Login
2. Dashboard
3. Reservation calendar
4. Properties with image
5. Maintenance
6. Reservations
7. Task or purchase list
8. Documents
9. AI assistant
10. Users/Profile
11. GitHub Actions
12. Docker Hub / Vercel / Render
```

## Screenshot naming convention

Use descriptive filenames:

```text
tamias-01-login.png
tamias-02-dashboard.png
tamias-03-calendar.png
tamias-04-properties.png
tamias-05-maintenance.png
tamias-06-reservations.png
tamias-07-purchases.png
tamias-08-documents.png
tamias-09-ai-assistant.png
tamias-10-users.png
tamias-11-github-actions.png
tamias-12-docker-hub.png
```

## Portfolio page structure

Suggested order:

```text
Hero
Short description
Problem
Solution
Main features
Architecture
Screenshots
AI/RAG section
Deployment section
Technical challenges
Security considerations
Known limitations
Links
```

## Final recommendation

For the public portfolio, link only:

```text
https://tamias.juantzun.dev
```

Keep the production URL private or mention it only as part of the technical deployment architecture.
