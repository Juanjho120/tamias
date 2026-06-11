# TAMIAS — Known Limitations and Follow-ups

## Purpose

This document records known limitations after the first successful deployment.

These are not blockers for the MVP, but they should be considered before broader real usage.

---

## 1. Render free-tier cold starts

### Status

```text
KNOWN / ACCEPTED
```

### Description

The backend can sleep after inactivity when hosted on a free-tier Render instance.

The first request after sleep can take time while the service wakes up.

### Impact

```text
Testing / Portfolio:
Acceptable for now.

Production real:
Should be upgraded before relying on it for day-to-day operational use.
```

### Follow-up

```text
[ ] Consider paid Render instance for production.
[ ] Add a small note in portfolio/demo if needed.
[ ] Consider uptime monitoring only if it does not conflict with hosting limits/costs.
```

---

## 2. Production auto-deploys from main

### Status

```text
KNOWN / ACCEPTED FOR MVP
```

### Description

Both testing and production currently stay updated from the latest changes.

### Impact

```text
Fast iteration, but every main update can reach production.
```

### Follow-up

```text
[ ] Move production to release tags later.
[ ] Or create a production branch.
[ ] Or deploy production from explicit Docker image SHA tags.
```

---

## 3. Initial admin bootstrap is manual

### Status

```text
KNOWN
```

### Description

The first organization/admin user was created manually during deployment.

### Impact

```text
Acceptable for first deployment.
Should be formalized later.
```

### Follow-up

```text
[ ] Create a controlled admin bootstrap command.
[ ] Or create a one-time startup bootstrap profile disabled by default.
[ ] Or document an official SQL bootstrap script with placeholders.
```

---

## 4. Automated test coverage needs expansion

### Status

```text
KNOWN
```

### Description

The project validates builds and Docker images through CI, but deeper automated coverage should be expanded.

### Follow-up

```text
[ ] Add service-level backend tests for core modules.
[ ] Add security/authorization tests for organization isolation.
[ ] Add frontend component tests for critical flows.
[ ] Add smoke/e2e tests for login and core navigation.
```

---

## 5. AI tool calling is planned but not implemented

### Status

```text
PLANNED
```

### Description

The current AI functionality supports document-based RAG.

Future AI tool calling over controlled PostgreSQL read-only domain tools is planned.

### Follow-up

```text
[ ] Define safe read-only AI tools.
[ ] Add strict organization scoping.
[ ] Add audit logs for AI tool calls.
[ ] Prevent unrestricted SQL execution.
```

---

## 6. Monitoring and alerting are minimal

### Status

```text
KNOWN
```

### Description

The first deployment validates health manually, but formal monitoring/alerting is not yet complete.

### Follow-up

```text
[ ] Add uptime checks.
[ ] Add production error monitoring.
[ ] Add structured logging review.
[ ] Add Render/Supabase/S3 usage monitoring notes.
```

---

## 7. Data backup and restore process is not formalized

### Status

```text
KNOWN
```

### Description

Supabase provides database capabilities, but TAMIAS should document the actual backup/restore process before heavy production usage.

### Follow-up

```text
[ ] Document Supabase backup strategy.
[ ] Document S3 backup/lifecycle strategy.
[ ] Document restore test procedure.
```

---

## 8. Public demo user is not defined

### Status

```text
OPTIONAL
```

### Description

The testing/portfolio environment is live, but a restricted public demo account has not been formally defined.

### Follow-up

```text
[ ] Decide whether to create a public read-only demo user.
[ ] If created, restrict role and data.
[ ] Avoid exposing real contact/property data.
```

---

## Final note

These limitations do not invalidate the MVP.

They provide a clear roadmap for hardening TAMIAS before broader real operational use.
