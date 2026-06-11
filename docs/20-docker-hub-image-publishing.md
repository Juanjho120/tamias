# TAMIAS — Bloque 8B-2: Docker Hub Image Publishing

## Scope

This block adds Docker Hub image publishing as part of the main CI workflow.

It does not use a separate workflow.

It does not change application runtime code.

It does not deploy to Render or Vercel.

It only publishes Docker images to Docker Hub after the Docker image build step succeeds.

---

## Workflow updated

Docker Hub publishing is part of:

```text
.github/workflows/ci.yml
```

The previous separated workflow must not be used:

```text
.github/workflows/docker-publish.yml
```

If that file exists, delete it:

```bash
git rm .github/workflows/docker-publish.yml
```

---

## CI flow

The CI flow is:

```text
backend-build
frontend-build
docker-build
  - Build backend Docker image
  - Build frontend Docker image
  - Login to Docker Hub
  - Tag Docker images
  - Push backend Docker image
  - Push frontend Docker image
```

Publishing only runs on:

```text
push to main
```

Publishing does not run on:

```text
pull_request
```

This keeps PR validation safe while still publishing real images after merges/pushes to `main`.

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

Use your real Docker Hub username in the GitHub secret.

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

Required permission:

```text
Read & Write
```

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

The backend image is first built locally inside CI as:

```text
tamias-backend:ci
```

Then tagged as:

```text
<DOCKERHUB_USERNAME>/tamias-backend:latest
<DOCKERHUB_USERNAME>/tamias-backend:sha-<short-git-sha>
```

Then pushed to Docker Hub.

---

## Frontend image

The frontend image is first built locally inside CI as:

```text
tamias-frontend:ci
```

The build uses:

```text
FRONTEND_API_BASE_URL=/api/v1
```

Reason:

```text
The Docker frontend is designed to run behind Nginx and proxy /api/* to the backend container.
```

Then it is tagged as:

```text
<DOCKERHUB_USERNAME>/tamias-frontend:latest
<DOCKERHUB_USERNAME>/tamias-frontend:sha-<short-git-sha>
```

Then pushed to Docker Hub.

---

## Why Docker Hub is optional for current deployment

TAMIAS current target deployment is:

```text
Frontend -> Vercel from GitHub source
Backend  -> Render from backend Dockerfile or GitHub source
```

So Docker Hub is not strictly required for the initial deployment.

However, publishing images is useful for:

```text
portfolio credibility
portable releases
future hosting options
manual rollback to a known SHA image
CI/CD maturity
```

---

## Validation

After pushing to `main`, check:

```text
GitHub Actions -> TAMIAS CI
Docker Hub -> Repositories
```

Expected Docker Hub repositories:

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

### Docker Hub login fails

Check:

```text
DOCKERHUB_USERNAME
DOCKERHUB_TOKEN
```

Make sure the token has read/write permission.

### Publishing runs on PR

It should not.

The publish steps have this condition:

```text
github.event_name == 'push' && github.ref == 'refs/heads/main'
```

### Separate docker-publish workflow still runs

Delete it:

```bash
git rm .github/workflows/docker-publish.yml
```

The Docker Hub publishing flow now belongs to the main CI workflow.

---

## Next step

Recommended next block:

```text
Bloque 8C: Deployment documentation with dual environments
```
