# TAMIAS — Roadmap

This roadmap reflects the current state after the Inventory Items, Reservation Supplies, Users Management, My Profile and Mandatory Password Change updates.

---

## Phase 0 — Project Setup

Status: Completed.

- Create repository.
- Add documentation.
- Define project structure.
- Prepare backend and frontend foundations.
- Configure initial local development.

---

## Phase 1 — Security and SaaS Foundation

Status: Completed / MVP-ready.

- Organizations.
- Users.
- Roles.
- Login.
- JWT.
- Protected routes.
- Current user context.
- Organization-based data isolation.
- User management for administrators.
- Self-service profile.
- Mandatory password change for temporary passwords.

---

## Phase 2 — Properties and Catalogs

Status: Completed / MVP-ready.

- Property management.
- Property images.
- Maintenance categories.
- Maintenance types.
- Maintenance people.
- Platforms.
- Suppliers.
- Cities.
- Brands.
- Task templates.
- Inventory Items.

---

## Phase 3 — Maintenance

Status: Completed / MVP-ready.

- Maintenance records.
- Responsible people.
- Maintenance record items.
- Images.
- Costs.
- Scheduled maintenance.
- Rescheduling.
- Cancellation.
- History.
- Dashboard/calendar integration.

---

## Phase 4 — Reservations and Tasks

Status: Completed / MVP-ready.

- Reservations.
- Guests.
- Reservation supplies.
- Supplies modal.
- Reservation calendar/dashboard integration.
- Task lists.
- Checklists.
- Task completion.
- Related task lists modal.

---

## Phase 5 — Purchase Lists

Status: Completed / MVP-ready.

- Purchase lists.
- Purchase items.
- Suppliers.
- Cities.
- Brands.
- Inventory Items.
- Estimated prices.
- Purchased status.
- Items modal.
- Purchase list form item editing.

---

## Phase 6 — Documents

Status: Completed / MVP-ready.

- Document upload.
- AWS S3 storage.
- Secure file URLs.
- Document types.
- Processing status.
- Text extraction.
- Chunking.

---

## Phase 7 — AI Document Search

Status: Completed / MVP-ready.

- Embeddings.
- Chroma vector store.
- RAG search.
- AI answers with sources.
- AI chat sessions.
- Basic quality improvements.

---

## Phase 8 — MVP Hardening

Status: Next recommended phase.

Goals:

- Review role-based access by endpoint.
- Review multi-tenant filtering.
- Verify `/users` is administrator-only.
- Verify `/profile` is accessible to every authenticated role.
- Verify temporary password changes are mandatory.
- Improve backend tests for critical services.
- Improve frontend tests for critical services/components.
- Validate build from clean clone.
- Review `.env.example`.
- Review local development instructions.
- Improve error handling consistency.
- Improve empty/loading states.

---

## Phase 9 — Dashboard Analytics

Status: Planned.

Goals:

- Maintenance cost by month.
- Purchases by month.
- Active reservations by month.
- Upcoming scheduled maintenance.
- Overdue tasks.
- Most used reservation supplies.
- Most purchased inventory items.
- Basic analytics cards and charts.

---

## Phase 10 — Production-like Deployment

Status: Planned.

Targets:

- Frontend on Vercel.
- Backend on Render.
- Database on Supabase PostgreSQL.
- Files on AWS S3.
- Chroma / AI services on Railway or equivalent.
- Domain: `tamias.juantzun.dev`.
- GitHub Actions validation.

---

## Phase 11 — AI Tool Calling

Status: Planned advanced phase.

Start read-only.

Suggested tools:

```text
findLastPurchaseByInventoryItem(itemName)
getMaintenanceCostByYear(year)
findOverdueTasks()
findLastMaintenanceByCategory(categoryName)
getUpcomingScheduledMaintenance(days)
getReservationSuppliesByReservation(reservationCode)
```

Rules:

- No free SQL execution.
- Tools must enforce organization isolation.
- Tools must expose controlled domain queries.
- Start with read-only operations before allowing actions.

---

## Phase 12 — Reports, Notifications and Advanced AI

Status: Future.

Possible features:

- JasperReports PDF reports.
- Email notifications.
- Advanced reminders.
- Blueprint analysis with OCR and vision models.
- AI agents by business domain.
- Formal inventory stock control.
- Platform integrations.
- Billing and subscriptions.
