# TAMIAS — First Deployment Troubleshooting

## Backend fails on Render: missing environment variable

Check Render logs for:

```text
Could not resolve placeholder
```

Fix:

```text
Add missing variable in Render service environment.
Redeploy.
```

Use templates:

```text
deploy/render/testing.env.example
deploy/render/production.env.example
```

---

## Backend fails: database connection

Common causes:

```text
Wrong DATABASE_URL format
Missing sslmode=require
Wrong username/password
Supabase project paused
Network restriction
```

Expected JDBC format:

```text
jdbc:postgresql://<host>:5432/<database>?sslmode=require
```

---

## Backend fails: Chroma invalid URI scheme

Symptom:

```text
invalid URI scheme chroma
```

Fix:

```text
CHROMA_HOST must include protocol.
```

Correct:

```text
CHROMA_HOST=https://<host>
CHROMA_PORT=8000
```

In Docker Compose:

```text
CHROMA_HOST=http://chroma
CHROMA_PORT=8000
```

---

## Backend fails: CORS

Symptom in browser:

```text
Access to fetch at ... has been blocked by CORS policy
```

Testing backend should have:

```text
CORS_ALLOWED_ORIGINS=https://tamias.juantzun.dev
```

Production backend should have:

```text
CORS_ALLOWED_ORIGINS=https://tamias-prod.juantzun.dev
```

After changing CORS:

```text
Redeploy backend.
Clear browser cache or hard reload.
```

---

## Frontend calls wrong backend

Open browser DevTools:

```text
Network tab
Check API request URL
```

Expected:

Testing frontend:

```text
https://tamias-api-testing.onrender.com/api/v1
```

Production frontend:

```text
https://tamias-api-prod.onrender.com/api/v1
```

If wrong, check Vercel variable:

```text
FRONTEND_API_BASE_URL
```

Then redeploy frontend.

---

## Vercel build fails: FRONTEND_API_BASE_URL is required

Fix:

```text
Add FRONTEND_API_BASE_URL in Vercel Project Settings -> Environment Variables.
Redeploy.
```

Testing:

```text
FRONTEND_API_BASE_URL=https://tamias-api-testing.onrender.com/api/v1
```

Production:

```text
FRONTEND_API_BASE_URL=https://tamias-api-prod.onrender.com/api/v1
```

---

## Vercel output directory issue

Correct output directory:

```text
dist/tamias-frontend/browser
```

Incorrect:

```text
dist/frontend/browser
```

---

## S3 upload fails

Check:

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
AWS_REGION
AWS_S3_BUCKET
IAM permissions
Bucket public access settings
```

Expected buckets:

```text
tamias-testing-files
tamias-prod-files
```

Minimum IAM actions:

```text
s3:PutObject
s3:GetObject
s3:DeleteObject
s3:HeadObject
```

---

## AI/RAG fails

Check:

```text
OPENAI_API_KEY
OPENAI_CHAT_MODEL
OPENAI_EMBEDDING_MODEL
CHROMA_BASE_URL
CHROMA_HOST
CHROMA_PORT
CHROMA_COLLECTION_NAME
```

Also confirm documents were processed before asking questions.

---

## Docker Hub push fails in CI

Check GitHub secrets:

```text
DOCKERHUB_USERNAME
DOCKERHUB_TOKEN
```

The token must have write permission.

---

## Render service keeps sleeping

On free tiers, services may sleep.

First request can be slow.

For production real usage, consider a paid plan or uptime strategy.
