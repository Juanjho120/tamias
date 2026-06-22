# TAMIAS — Bloque 8B: Docker Production Readiness

## Chroma host fix

For Spring AI Chroma, the backend configuration uses:

```yaml
spring:
  ai:
    vectorstore:
      chroma:
        client:
          host: ${CHROMA_HOST}
          port: ${CHROMA_PORT:8000}
```

`CHROMA_HOST` must include the protocol but not the port.

Correct:

```text
CHROMA_HOST=http://chroma
CHROMA_PORT=8000
```

Incorrect:

```text
CHROMA_HOST=chroma
CHROMA_PORT=8000
```

The incorrect value can produce:

```text
invalid URI scheme chroma
```

because Java/Spring tries to use `chroma` as the URI scheme.

---

## Backend Docker runtime image

The backend Dockerfile uses Ubuntu/Debian-family Eclipse Temurin Jammy images instead of Alpine:

```dockerfile
FROM eclipse-temurin:21-jdk-jammy AS build
...
FROM eclipse-temurin:21-jre-jammy
```

Reason:

```text
TAMIAS uses OpenCV Java through org.openpnp:opencv.
OpenCV's native Linux library requires glibc-compatible runtime libraries such as libstdc++.so.6.
Alpine uses musl libc and does not provide this runtime compatibility by default.
```

The runtime image must install at least:

```text
ca-certificates
libstdc++6
libgomp1
```

This is required for Product Box texture processing features such as:

```text
OpenCV perspective correction
Automatic contour detection
OpenCV image enhancement
```

Do not switch the backend runtime image back to Alpine unless OpenCV native runtime compatibility is explicitly retested.

If Render still uses an older Alpine-based layer after this change, run:

```text
Manual Deploy -> Clear build cache & deploy
```

---

## Frontend Docker relative API URL

The frontend Docker image builds with:

```bash
docker build --build-arg FRONTEND_API_BASE_URL=/api/v1 -t tamias-frontend:local ./frontend
```

This is intentional.

Inside the frontend Docker container, Angular calls:

```text
/api/v1
```

and Nginx proxies:

```text
/api/*
```

to:

```text
BACKEND_API_URL
```

In `docker-compose.prod-like.yml`:

```text
BACKEND_API_URL=http://backend:8080
```

---

## Angular output path

The frontend Dockerfile copies:

```text
/app/dist/tamias-frontend/browser
```

because the Angular project output is:

```text
dist/tamias-frontend/browser
```

---

## Useful Docker commands

### Build images

Backend:

```bash
docker build -t tamias-backend:local ./backend
```

Frontend:

```bash
docker build --build-arg FRONTEND_API_BASE_URL=/api/v1 -t tamias-frontend:local ./frontend
```

Important:

```text
docker build creates images, not containers.
```

In Docker Desktop:

```text
Images tab -> after docker build
Containers tab -> after docker run or docker compose up
```

### Run prod-like stack

```bash
docker compose -f docker-compose.prod-like.yml up --build
```

Open:

```text
http://localhost:8088
```

### Stop prod-like stack

```bash
docker compose -f docker-compose.prod-like.yml down
```

### Clean volumes

```bash
docker compose -f docker-compose.prod-like.yml down -v
```

---

## CI Docker validation and publishing

The CI workflow validates both images:

```bash
docker build -t tamias-backend:ci ./backend
docker build --build-arg FRONTEND_API_BASE_URL=/api/v1 -t tamias-frontend:ci ./frontend
```

After those builds succeed, the same CI workflow publishes images to Docker Hub on `push` to `main`.

Published images:

```text
/tamias-backend
/tamias-frontend
```

Tags:

```text
latest
sha-
```

The publishing steps are documented in:

```text
docs/20-docker-hub-image-publishing.md
```

There should not be a separate Docker publishing workflow.
