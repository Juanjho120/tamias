# 17 — Reports

## Status

Future / planned after Payments.

## Purpose

Add report generation for TAMIAS after the Payments module is designed/implemented and operational data is stable.

Reports should summarize operational data from properties, maintenance, reservations, inventory, purchases, files/images and payments.

## Scope candidates

- Operational PDF reports.
- Maintenance cost reports.
- Purchase cost reports.
- Payment/expense reports.
- Reservation summaries.
- Inventory usage reports.
- Image/file storage summaries if useful.
- Property-level summaries.
- Date-range summaries.

## Technical direction

Reports should build on existing stable data and avoid changing operational flows.

JasperReports remains the likely backend reporting technology, but implementation should be designed in a dedicated report phase instead of being mixed with Payments, organization administration, Product Box Models or AI/RAG phases.

Suggested implementation direction:

```text
17A Reports backend foundation
17B Operational summary JSON reports
17C PDF export / JasperReports integration
17D Angular reports page
17E AI awareness for read-only report summaries
```

## Non-goals

- Do not add notification delivery in this phase.
- Do not add Blueprint Analysis in this phase.
- Do not process payments in this phase.
- Do not make TAMI execute report writes/actions unless a later write-capable AI phase is explicitly approved.
