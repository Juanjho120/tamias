# TAMIAS — Deployment Preparation Kit

This folder contains copy/paste templates for the first real deployment.

## Files

```text
deploy/render/testing.env.example
deploy/render/production.env.example
deploy/vercel/testing.env.example
deploy/vercel/production.env.example
deploy/cloudflare/dns-records.md
deploy/smoke-tests/tamias-smoke-tests.http
deploy/smoke-tests/smoke-test-urls.md
```

## Important

These files must not contain real secrets.

Use them as templates only.

Real values belong in:

```text
Render environment variables
Vercel environment variables
GitHub Actions secrets
AWS IAM/S3
Supabase
Railway
```

## Recommended deployment order

```text
1. Confirm GitHub CI and Docker Hub publishing works.
2. Create Supabase testing and production databases.
3. Create S3 testing and production buckets.
4. Create Chroma testing and production resources.
5. Create Render testing backend.
6. Create Render production backend.
7. Create Vercel testing frontend.
8. Create Vercel production frontend.
9. Configure Cloudflare DNS.
10. Run smoke tests.
```
