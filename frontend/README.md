# TAMIAS Frontend

Angular frontend for TAMIAS.

## Stack

- Angular
- TypeScript
- Bootstrap
- Angular Reactive Forms
- FullCalendar later for maintenance calendar

## Local setup

```bash
cd frontend
npm install
npm start
```

Open:

```text
http://localhost:4200
```

## Local backend

The local environment points to:

```text
http://localhost:8080/api/v1
```

Configured in:

```text
src/environments/environment.ts
```

## Implemented in this block

- Angular app inside `/frontend`
- Routing base
- Main layout
- Login page
- Auth service
- JWT interceptor
- Auth guard
- Dashboard shell
- Placeholder routes for MVP modules

## Routes

```text
/login
/dashboard
/properties
/catalogs
/maintenance
/scheduled-maintenance
/reservations
/tasks
/purchases
/documents
/ai-assistant
```

## Suggested commit

```bash
git add .
git commit -m "feat: add Angular frontend base setup"
git push
```
