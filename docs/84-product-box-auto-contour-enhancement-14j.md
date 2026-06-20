# 14J — Automatic Contour Detection and Image Enhancement

Status: **Implemented**

## Purpose

Add an assistive OpenCV step to Product Box 3D Textures:

- automatically detect a likely rectangular package-face contour from the uploaded original photo,
- prefill the existing four-corner editor with detected points,
- keep manual adjustment as the fallback/source of truth,
- apply conservative image enhancement during perspective processing.

This phase builds on the completed manual workflow:

```text
upload original → adjust 4 points → process OpenCV texture → preview → accept → use in Three.js
```

## Backend changes

### Migration

```text
V35__add_product_box_texture_detection_enhancement.sql
```

Adds metadata to `product_box_model_faces`:

```text
auto_detected_points BOOLEAN NOT NULL DEFAULT false
contour_confidence NUMERIC(5,4)
enhancement_mode VARCHAR(20) NOT NULL DEFAULT 'basic'
```

### Endpoint

```text
POST /api/v1/product-box-models/{id}/faces/{faceName}/texture/detect-contour
```

Response:

```json
{
  "detected": true,
  "confidence": 0.8600,
  "points": {
    "topLeft": { "x": 123, "y": 456 },
    "topRight": { "x": 789, "y": 450 },
    "bottomRight": { "x": 800, "y": 1200 },
    "bottomLeft": { "x": 110, "y": 1210 }
  },
  "message": "Contour detected successfully"
}
```

If no reliable contour is found, the endpoint returns `detected = false` and the frontend opens the manual editor with default points.

## OpenCV detection strategy

Detection is intentionally conservative:

1. Decode original S3 image with OpenCV.
2. Convert to grayscale.
3. Blur slightly.
4. Detect edges with Canny.
5. Dilate edges.
6. Find external contours.
7. Approximate polygons.
8. Choose the largest valid four-point contour above the minimum area threshold.
9. Order points as:
   - top-left,
   - top-right,
   - bottom-right,
   - bottom-left.
10. Convert OpenCV-source coordinates back to the original image coordinate system used by the Angular editor.

The detection only preloads points. Users can always drag the points before processing.

## Image enhancement

Processing now supports enhancement modes:

```text
none
basic
strong
```

Default mode is:

```text
basic
```

Enhancement is applied after `warpPerspective` and before PNG encoding. It uses conservative CLAHE-based contrast improvement, avoiding aggressive color changes. `strong` applies a slightly stronger CLAHE/brightness adjustment, but the UI currently sends/uses the default `basic` mode.

## Frontend behavior

The Product Box faces modal now shows a **Detect contour** button when an original photo exists.

Flow:

1. User uploads original photo.
2. User clicks **Detect contour**.
3. If detection succeeds:
   - points are loaded into the corner editor,
   - the editor opens,
   - user can adjust points manually.
4. If detection fails:
   - the editor opens with default/manual points,
   - the user adjusts corners manually.
5. Processing continues through the existing 14H/14I workflow.

## Storage and hard delete

No new S3 locations are introduced in 14J.

The existing hard-delete rules still apply for:

- active accepted texture `s3_key`,
- original upload `original_s3_key`,
- processed preview `processed_s3_key`.

## Out of scope

- No generative AI.
- No automatic acceptance of detected points.
- No destructive modification of the original image.
- No separate `product_box_textures` table.
- No integration with inventory/purchases.
- No AI awareness yet.

## Next phase

```text
14K — Integration with Inventory/Purchases
```
