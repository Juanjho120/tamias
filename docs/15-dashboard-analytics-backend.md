# TAMIAS — Bloque 7A: Dashboard Analytics Backend Endpoint

## Scope

This block adds a backend-only analytics endpoint for the dashboard.

It does not modify:

```text
frontend/src/app/features/dashboard/dashboard.component.ts
frontend/src/app/features/dashboard/dashboard.component.html
frontend/src/app/features/dashboard/*
```

The current dashboard calendar remains untouched.

---

## Endpoint

```http
GET /api/v1/dashboard/analytics
```

Query params:

```text
months       default 6, min 1, max 24
upcomingDays default 30, min 1, max 365
topLimit     default 5, min 1, max 20
```

Example:

```http
GET /api/v1/dashboard/analytics?months=6&upcomingDays=30&topLimit=5
```

---

## Response

```json
{
  "kpis": {
    "activeReservationsToday": 0,
    "pendingMaintenanceRecords": 0,
    "overdueTaskLists": 0,
    "upcomingScheduledMaintenance": 0,
    "openPurchaseLists": 0,
    "estimatedOpenPurchaseTotal": 0
  },
  "maintenanceCostByMonth": [],
  "purchaseCostByMonth": [],
  "reservationsByMonth": [],
  "topReservationSupplies": [],
  "topPurchasedItems": []
}
```

---

## Metrics

### KPIs

```text
activeReservationsToday
pendingMaintenanceRecords
overdueTaskLists
upcomingScheduledMaintenance
openPurchaseLists
estimatedOpenPurchaseTotal
```

### Monthly analytics

```text
maintenanceCostByMonth
purchaseCostByMonth
reservationsByMonth
```

### Top lists

```text
topReservationSupplies
topPurchasedItems
```

---

## Multi-tenant rules

All queries filter by:

```text
organization_id = current authenticated user's organization
```

The endpoint does not accept `organizationId` from the frontend.

---

## Security

The endpoint is available to authenticated roles:

```text
ADMINISTRATOR
PROPERTY_MANAGER
MAINTENANCE_STAFF
READ_ONLY
```

The endpoint is read-only.

---

## Implementation notes

This block uses native SQL through `EntityManager`.

Reason:

```text
The endpoint is reporting-oriented.
Using native SQL avoids touching existing repositories and avoids modifying existing domain services.
```

This keeps the block isolated and reduces risk of breaking current operational modules.

---

## Validation

Backend build:

```bash
cd backend
./mvnw.cmd clean package -DskipTests
```

Manual smoke test:

```http
GET /api/v1/dashboard/analytics
GET /api/v1/dashboard/analytics?months=12&upcomingDays=60&topLimit=10
```

Expected:

```text
200 OK for authenticated users
401 for unauthenticated users
no data leakage across organizations
```
