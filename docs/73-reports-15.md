# 15 — Reports

Status: **Future / planned after Product Box Models**

## Purpose

Add report generation for TAMIAS after the Product Box Models work is complete.

## Scope candidates

- Operational PDF reports.
- Maintenance cost reports.
- Purchase cost reports.
- Reservation summaries.
- Inventory usage reports.
- Image/file storage summaries if useful.

## Technical direction

Reports should build on existing stable data and avoid changing operational flows. JasperReports remains the likely backend reporting technology, but implementation should be designed in a dedicated report phase instead of being mixed with Product Box Models or AI/RAG phases.

## Non-goals

- Do not add notification delivery in this phase.
- Do not add Blueprint Analysis in this phase.
- Do not make TAMI execute report writes/actions unless a later write-capable AI phase is explicitly approved.
