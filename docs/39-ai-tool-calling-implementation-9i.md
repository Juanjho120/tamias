# Fase 9I — Scheduled maintenance, reservation and guest read-only tools

## Objetivo

Expandir Tool Calling read-only para cubrir consultas operativas sobre mantenimientos programados, reservaciones y huéspedes, manteniendo las reglas de seguridad de TAMIAS:

- Read-only first.
- No free SQL generado por IA.
- No autonomous writes.
- `organization_id` resuelto por backend.
- Respuestas con `toolEvidence`.
- No exposición innecesaria de PII de huéspedes.

## Tools agregadas

### Scheduled Maintenance Tools

- `scheduledMaintenance.search`
- `scheduledMaintenance.upcoming`
- `scheduledMaintenance.overdue` *(ya existente, preservada)*
- `scheduledMaintenance.dueToday`
- `scheduledMaintenance.dueThisWeek`
- `scheduledMaintenance.byProperty`
- `scheduledMaintenance.byType`
- `scheduledMaintenance.byStatus`
- `scheduledMaintenance.nextDue`
- `scheduledMaintenance.frequencySummary`
- `scheduledMaintenance.history`
- `scheduledMaintenance.complianceSummary`

### Reservation Tools

- `reservation.search`
- `reservation.upcoming` *(ya existente, preservada)*
- `reservation.current`
- `reservation.today`
- `reservation.thisWeek`
- `reservation.thisMonth`
- `reservation.byProperty`
- `reservation.byGuest`
- `reservation.byStatus`
- `reservation.byPlatform`
- `reservation.occupancySummary`
- `reservation.revenueSummary`
- `reservation.nightsSummary`
- `reservation.guestCountSummary`
- `reservation.calendarEvents`
- `reservation.nextCheckIn`
- `reservation.nextCheckOut`
- `reservation.gapsBetweenReservations`

### Guest Tools

- `guest.search`
- `guest.byReservation`
- `guest.recent`
- `guest.returningGuests`
- `guest.upcomingGuests`
- `guest.countByDateRange`

## PII / privacidad

Las respuestas de huéspedes solo muestran datos operativos mínimos:

- Nombre del huésped.
- Propiedad.
- Fechas de reserva.
- Código de reservación si existe.

No se muestra teléfono, notas internas ni otros datos sensibles salvo que una fase futura defina reglas explícitas de permisos.

## Smoke tests sugeridos

```text
¿Qué mantenimientos están vencidos?
¿Qué mantenimiento toca esta semana?
¿Cuál es el próximo mantenimiento del pozo?
¿Qué historial tiene este mantenimiento programado?
¿Qué reservas tengo esta semana?
¿Quién llega mañana?
¿Cuál es la próxima salida?
¿Cuántas noches tengo reservadas este mes?
¿Qué días tengo libres entre reservas?
¿Qué propiedad tiene más ocupación?
¿Qué huéspedes llegan esta semana?
¿Este huésped ya se había quedado antes?
¿Cuántos huéspedes tendré este mes?
```

## Notas técnicas

- Se usa la tabla puente `reservation_guests` para relacionar huéspedes y reservaciones.
- `guests.full_name` es el campo usado para mostrar nombres de huéspedes.
- `scheduled_maintenance.next_due_date` se usa para vencimientos, próximos mantenimientos y resúmenes.
- Las consultas siguen limitadas por `organization_id` del usuario autenticado.
