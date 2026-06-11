# TAMIAS — Bloque 8B-2: Docker Hub Image Publishing

## Scope

This block adds Docker Hub image publishing to GitHub Actions.

It does not change application runtime code.

It does not deploy to Render or Vercel.

It only publishes Docker images to Docker Hub.

---

## Workflow added

```text
.github/workflows/docker-publish.yml
```

This workflow runs on:

```text
push to main
manual workflow_dispatch
```

---

## Images published

The workflow publishes:

```text
<DOCKERHUB_USERNAME>/tamias-backend
<DOCKERHUB_USERNAME>/tamias-frontend
```

Examples:

```text
juantzun/tamias-backend
juantzun/tamias-frontend
```

Use your real Docker Hub username.

---

## Tags

Each image receives:

```text
latest
sha-<short-git-sha>
```

Example:

```text
juantzun/tamias-backend:latest
juantzun/tamias-backend:sha-a1b2c3d
juantzun/tamias-frontend:latest
juantzun/tamias-frontend:sha-a1b2c3d
```

---

## Required GitHub secrets

Create these repository secrets:

```text
DOCKERHUB_USERNAME
DOCKERHUB_TOKEN
```

Do not use your Docker Hub password.

Use a Docker Hub access token.

---

## How to create Docker Hub token

In Docker Hub:

```text
Account Settings
Security
Personal access tokens
Generate new token
```

Recommended permissions:

```text
Read, Write, Delete
```

For this workflow, write permission is required to push images.

---

## How to add GitHub secrets

In GitHub repository:

```text
Settings
Secrets and variables
Actions
Repository secrets
New repository secret
```

Add:

```text
DOCKERHUB_USERNAME=<your-dockerhub-username>
DOCKERHUB_TOKEN=<your-dockerhub-access-token>
```

---

## Backend image

The backend image uses:

```text
backend/Dockerfile
```

Build context:

```text
./backend
```

Published as:

```text
<DOCKERHUB_USERNAME>/tamias-backend
```

---

## Frontend image

The frontend image uses:

```text
frontend/Dockerfile
```

Build context:

```text
./frontend
```

Build arg:

```text
FRONTEND_API_BASE_URL=/api/v1
```

Reason:

```text
The Docker frontend is designed to run behind Nginx and proxy /api/* to the backend container.
```

Vercel still uses:

```text
FRONTEND_API_BASE_URL=https://tamias-api-testing.onrender.com/api/v1
FRONTEND_API_BASE_URL=https://tamias-api-prod.onrender.com/api/v1
```

depending on the project.

---

## Why Docker Hub is optional for current deployment

TAMIAS current target deployment is:

```text
Frontend -> Vercel from GitHub source
Backend  -> Render from backend Dockerfile or GitHub source
```

So Docker Hub is not strictly required.

However, publishing images is useful for:

```text
portfolio credibility
portable releases
future hosting options
manual rollback to a known SHA image
CI/CD maturity
```

---

## Pull images locally

Backend:

```bash
docker pull <DOCKERHUB_USERNAME>/tamias-backend:latest
```

Frontend:

```bash
docker pull <DOCKERHUB_USERNAME>/tamias-frontend:latest
```

Run frontend image manually:

```bash
docker run --rm -p 8088:80 \
  -e BACKEND_API_URL=http://host.docker.internal:8080 \
  <DOCKERHUB_USERNAME>/tamias-frontend:latest
```

---

## Render deployment note

You can keep Render building from the repository Dockerfile.

Later, if desired, Render can also deploy from the Docker Hub image:

```text
<DOCKERHUB_USERNAME>/tamias-backend:latest
```

For production real environments, prefer SHA tags for reproducibility:

```text
<DOCKERHUB_USERNAME>/tamias-backend:sha-a1b2c3d
```

---

## Validation

After pushing to `main`, check:

```text
GitHub Actions -> Publish Docker images
Docker Hub -> Repositories
```

Expected repositories:

```text
tamias-backend
tamias-frontend
```

Expected tags:

```text
latest
sha-...
```

---

## Troubleshooting

### Unauthorized

Check:

```text
DOCKERHUB_USERNAME
DOCKERHUB_TOKEN
```

Make sure the token has write permission.

### Repository name issues

Docker Hub repository names must be lowercase.

The workflow uses:

```text
tamias-backend
tamias-frontend
```

which are valid lowercase names.

### Frontend build arg missing

The workflow passes:

```text
FRONTEND_API_BASE_URL=/api/v1
```

This is required by:

```text
frontend/scripts/write-prod-env.js
```

---

## Next step

Recommended next block:

```text
Bloque 8C: Deployment documentation with dual environments
```
