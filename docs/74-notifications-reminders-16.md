# 16 — Notifications and reminders

Status: **Future / planned after Reports**

## Purpose

Add notification and reminder capabilities for TAMIAS after the Reports phase is designed/implemented.

## Scope candidates

- Email notifications.
- Scheduled maintenance reminders.
- Reservation check-in/check-out reminders.
- Task due reminders.
- Purchase/list follow-up reminders.
- Admin-configurable notification preferences.

## Technical direction

Notifications should be separated from Reports because they involve delivery channels, schedules, retries, user preferences and potentially background jobs.

## Non-goals

- Do not combine notification delivery with report rendering in the same phase.
- Do not use TAMI for write/send actions until write-capable AI features are explicitly designed and approved.
