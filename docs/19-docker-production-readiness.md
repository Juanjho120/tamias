# TAMIAS — Bloque 8B: Docker Production Readiness

## Fix note: frontend Docker relative API URL

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

Therefore, `write-prod-env.js` must accept both:

```text
https://tamias-api-testing.onrender.com/api/v1
/api/v1
```

Absolute URLs are used by Vercel builds.
Relative `/api/v1` is used by the Docker/Nginx prod-like setup.

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
Images tab      -> after docker build
Containers tab  -> after docker run or docker compose up
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
