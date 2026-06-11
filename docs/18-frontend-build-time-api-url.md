# TAMIAS — Bloque 8A-3: Frontend Build-time API URL Configuration

## Scope

This block configures the Angular frontend so each Vercel project can build with its own backend API URL.

This is required because TAMIAS will have two deployed frontend environments:

```text
Testing / Portfolio -> https://tamias.juantzun.dev
Production real     -> https://tamias-prod.juantzun.dev
```

Each frontend must call a different backend:

```text
Testing frontend    -> https://tamias-api-testing.onrender.com/api/v1
Production frontend -> https://tamias-api-prod.onrender.com/api/v1
```

---

## Why this is needed

Angular environment files are build-time.

That means this file:

```text
frontend/src/environments/environment.prod.ts
```

is baked into the compiled frontend bundle during build.

If both Vercel projects deploy from the same branch, both need a way to generate different production API URLs during their own build.

---

## Implemented strategy

This block adds:

```text
frontend/scripts/write-prod-env.js
```

The script reads:

```text
FRONTEND_API_BASE_URL
```

and writes:

```text
frontend/src/environments/environment.prod.ts
```

Example generated file:

```ts
export const environment = {
  production: true,
  apiBaseUrl: 'https://tamias-api-testing.onrender.com/api/v1'
};
```

---

## New npm script

The frontend package now includes:

```json
"build:prod": "node scripts/write-prod-env.js && ng build --configuration production"
```

The existing script remains unchanged:

```json
"build": "ng build"
```

This keeps local build behavior simple while giving Vercel an environment-aware production build.

---

## Vercel configuration

### Testing / Portfolio Vercel project

Project:

```text
tamias-testing
```

Domain:

```text
https://tamias.juantzun.dev
```

Environment variable:

```text
FRONTEND_API_BASE_URL=https://tamias-api-testing.onrender.com/api/v1
```

Build command:

```bash
npm run build:prod
```

Output directory:

```text
dist/frontend/browser
```

---

### Production Vercel project

Project:

```text
tamias-production
```

Domain:

```text
https://tamias-prod.juantzun.dev
```

Environment variable:

```text
FRONTEND_API_BASE_URL=https://tamias-api-prod.onrender.com/api/v1
```

Build command:

```bash
npm run build:prod
```

Output directory:

```text
dist/frontend/browser
```

---

## Local validation

### Testing API URL build

On PowerShell:

```powershell
cd frontend
$env:FRONTEND_API_BASE_URL="https://tamias-api-testing.onrender.com/api/v1"
npm run build:prod
```

On Git Bash:

```bash
cd frontend
FRONTEND_API_BASE_URL=https://tamias-api-testing.onrender.com/api/v1 npm run build:prod
```

Expected output:

```text
[write-prod-env] environment.prod.ts generated with apiBaseUrl=https://tamias-api-testing.onrender.com/api/v1
```

### Production API URL build

On PowerShell:

```powershell
cd frontend
$env:FRONTEND_API_BASE_URL="https://tamias-api-prod.onrender.com/api/v1"
npm run build:prod
```

On Git Bash:

```bash
cd frontend
FRONTEND_API_BASE_URL=https://tamias-api-prod.onrender.com/api/v1 npm run build:prod
```

Expected output:

```text
[write-prod-env] environment.prod.ts generated with apiBaseUrl=https://tamias-api-prod.onrender.com/api/v1
```

---

## Important local note

The script overwrites:

```text
frontend/src/environments/environment.prod.ts
```

If you run `npm run build:prod` locally, Git may show this file as modified.

Before committing, verify whether you want to keep that generated URL or reset it.

Recommended committed fallback:

```text
https://tamias-api-testing.onrender.com/api/v1
```

because testing/portfolio is the safer default.

---

## What happens if the env var is missing

If `FRONTEND_API_BASE_URL` is missing, the script fails the build.

This is intentional.

A production deployment should never silently build with the wrong backend URL.

---

## Files changed

```text
frontend/scripts/write-prod-env.js
frontend/package.json
frontend/src/environments/environment.prod.ts
docs/18-frontend-build-time-api-url.md
```

---

## Next step

This completes the frontend API URL strategy for two Vercel projects.

Recommended next block:

```text
Bloque 8B: Docker production readiness
```
