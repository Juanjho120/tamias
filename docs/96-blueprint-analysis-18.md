# 18 — Blueprint Analysis

## Status

Future / planned after Reports and Notifications.

## Purpose

Add blueprint/plan analysis capabilities after the operational/reporting/notification roadmap is stable.

## Scope candidates

- Upload floor plans or blueprints.
- Extract measurements when possible.
- Search blueprint metadata through TAMI.
- Use OCR and/or vision models only after a dedicated design phase.

## Technical direction

Blueprint Analysis should be handled separately because it may require OCR, vision models, image/PDF processing, confidence scoring and careful UX for uncertain measurements.

## Non-goals

- Do not mix Blueprint Analysis with Product Box Models.
- Product Box Models render boxes from user-provided dimensions and images; they do not analyze images.
- Do not assume AI can accurately infer dimensions from an image without a dedicated validation workflow.
