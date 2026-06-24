# 18 — Notifications and reminders

## Status

Future / planned after Reports.

## Purpose

Add notification and reminder capabilities for TAMIAS after Payments and Reports are designed/implemented.

## Scope candidates

- Email notifications.
- Scheduled maintenance reminders.
- Reservation check-in/check-out reminders.
- Task due reminders.
- Purchase/list follow-up reminders.
- Payment due/follow-up reminders if a future payment scheduling field is added.
- Admin-configurable notification preferences.

## Technical direction

Notifications should be separated from Reports because they involve delivery channels, schedules, retries, user preferences and potentially background jobs.

Suggested implementation direction:

```text
18A Notification preferences foundation
18B Reminder rules and scheduler
18C Email delivery
18D Angular notification settings
18E AI awareness for read-only reminder summaries
```

## Non-goals

- Do not combine notification delivery with report rendering in the same phase.
- Do not add Blueprint Analysis in this phase.
- Do not use TAMI for write/send actions until write-capable AI features are explicitly designed and approved.
