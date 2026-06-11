# TAMIAS — Environment Variables Checklist

## Purpose

Use this checklist to configure the two deployed TAMIAS environments.

Do not commit real values.

---

# GitHub Actions secrets

| Secret | Required | Notes |
|---|---:|---|
| `DOCKERHUB_USERNAME` | Yes | Docker Hub username |
| `DOCKERHUB_TOKEN` | Yes | Docker Hub access token |

---

# Render backend variables — Testing

Service:

```text
tamias-api-testing
```

| Variable | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `PORT` | `8080` |
| `DATABASE_URL` | `<testing-jdbc-url>` |
| `DATABASE_USERNAME` | `<testing-db-username>` |
| `DATABASE_PASSWORD` | `<testing-db-password>` |
| `JWT_SECRET` | `<testing-secret>` |
| `JWT_EXPIRATION_MINUTES` | `60` |
| `CORS_ALLOWED_ORIGINS` | `https://tamias.juantzun.dev` |
| `AWS_ACCESS_KEY_ID` | `<testing-aws-access-key>` |
| `AWS_SECRET_ACCESS_KEY` | `<testing-aws-secret-key>` |
| `AWS_REGION` | `us-east-2` |
| `AWS_S3_BUCKET` | `tamias-testing-files` |
| `S3_PRESIGNED_URL_EXPIRATION_SECONDS` | `300` |
| `OPENAI_API_KEY` | `<openai-api-key>` |
| `OPENAI_CHAT_MODEL` | `gpt-4o-mini` |
| `OPENAI_EMBEDDING_MODEL` | `text-embedding-3-small` |
| `OPENAI_TEMPERATURE` | `0.2` |
| `CHROMA_HOST` | `<testing-chroma-host-with-protocol-no-port>` |
| `CHROMA_PORT` | `8000` |
| `CHROMA_BASE_URL` | `<testing-chroma-base-url>` |
| `CHROMA_COLLECTION_NAME` | `tamias_testing_documents` |
| `AI_DEFAULT_TOP_K` | `10` |
| `AI_DEFAULT_SIMILARITY_THRESHOLD` | `0.30` |
| `MAX_FILE_SIZE` | `8MB` |
| `MAX_REQUEST_SIZE` | `8MB` |

---

# Render backend variables — Production

Service:

```text
tamias-api-prod
```

| Variable | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `PORT` | `8080` |
| `DATABASE_URL` | `<production-jdbc-url>` |
| `DATABASE_USERNAME` | `<production-db-username>` |
| `DATABASE_PASSWORD` | `<production-db-password>` |
| `JWT_SECRET` | `<production-secret>` |
| `JWT_EXPIRATION_MINUTES` | `60` |
| `CORS_ALLOWED_ORIGINS` | `https://tamias-prod.juantzun.dev` |
| `AWS_ACCESS_KEY_ID` | `<production-aws-access-key>` |
| `AWS_SECRET_ACCESS_KEY` | `<production-aws-secret-key>` |
| `AWS_REGION` | `us-east-2` |
| `AWS_S3_BUCKET` | `tamias-prod-files` |
| `S3_PRESIGNED_URL_EXPIRATION_SECONDS` | `300` |
| `OPENAI_API_KEY` | `<openai-api-key>` |
| `OPENAI_CHAT_MODEL` | `gpt-4o-mini` |
| `OPENAI_EMBEDDING_MODEL` | `text-embedding-3-small` |
| `OPENAI_TEMPERATURE` | `0.2` |
| `CHROMA_HOST` | `<production-chroma-host-with-protocol-no-port>` |
| `CHROMA_PORT` | `8000` |
| `CHROMA_BASE_URL` | `<production-chroma-base-url>` |
| `CHROMA_COLLECTION_NAME` | `tamias_prod_documents` |
| `AI_DEFAULT_TOP_K` | `10` |
| `AI_DEFAULT_SIMILARITY_THRESHOLD` | `0.30` |
| `MAX_FILE_SIZE` | `8MB` |
| `MAX_REQUEST_SIZE` | `8MB` |

---

# Vercel variables — Testing

Project:

```text
tamias-testing
```

| Variable | Value |
|---|---|
| `FRONTEND_API_BASE_URL` | `https://tamias-api-testing.onrender.com/api/v1` |

Build settings:

| Setting | Value |
|---|---|
| Root Directory | `frontend` |
| Build Command | `npm run build:prod` |
| Output Directory | `dist/tamias-frontend/browser` |

---

# Vercel variables — Production

Project:

```text
tamias-production
```

| Variable | Value |
|---|---|
| `FRONTEND_API_BASE_URL` | `https://tamias-api-prod.onrender.com/api/v1` |

Build settings:

| Setting | Value |
|---|---|
| Root Directory | `frontend` |
| Build Command | `npm run build:prod` |
| Output Directory | `dist/tamias-frontend/browser` |

---

# Chroma variable rule

Correct:

```text
CHROMA_HOST=https://example.com
CHROMA_PORT=8000
CHROMA_BASE_URL=https://example.com
```

or in Docker Compose:

```text
CHROMA_HOST=http://chroma
CHROMA_PORT=8000
CHROMA_BASE_URL=http://chroma:8000
```

Incorrect:

```text
CHROMA_HOST=chroma
```

because Spring AI needs a URI with protocol.

---

# CORS rule

Testing backend:

```text
CORS_ALLOWED_ORIGINS=https://tamias.juantzun.dev
```

Production backend:

```text
CORS_ALLOWED_ORIGINS=https://tamias-prod.juantzun.dev
```

Do not put both frontend domains in production unless intentionally needed.

---

# Data isolation rule

Testing and production must not share:

```text
DATABASE_URL
AWS_S3_BUCKET
CHROMA_COLLECTION_NAME
JWT_SECRET
```

OpenAI API key can be shared for MVP, but usage must be monitored.
