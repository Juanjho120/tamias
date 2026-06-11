# TAMIAS — Cloudflare DNS Records

## Scope

Create DNS records after Vercel gives you the required target values.

Do not guess the CNAME target. Use the value shown by Vercel for each custom domain.

---

## Testing / Portfolio

Domain:

```text
tamias.juantzun.dev
```

Expected setup:

```text
Type: CNAME
Name: tamias
Target: <value-provided-by-vercel-for-tamias-testing>
Proxy status: DNS only first, then proxied if verified and working
```

Vercel project:

```text
tamias-testing
```

---

## Production real

Domain:

```text
tamias-prod.juantzun.dev
```

Expected setup:

```text
Type: CNAME
Name: tamias-prod
Target: <value-provided-by-vercel-for-tamias-production>
Proxy status: DNS only first, then proxied if verified and working
```

Vercel project:

```text
tamias-production
```

---

## Rule

Do not point both domains to the same Vercel project.

Each domain must be connected to its corresponding Vercel project:

```text
tamias.juantzun.dev      -> tamias-testing
tamias-prod.juantzun.dev -> tamias-production
```

---

## Optional backend DNS later

For the MVP, backend can stay on Render URLs:

```text
https://tamias-api-testing.onrender.com
https://tamias-api-prod.onrender.com
```

Optional future backend domains:

```text
api-tamias.juantzun.dev
api-tamias-prod.juantzun.dev
```

If backend domains are added later, also update:

```text
FRONTEND_API_BASE_URL
CORS_ALLOWED_ORIGINS if needed
deployment docs
smoke tests
```
