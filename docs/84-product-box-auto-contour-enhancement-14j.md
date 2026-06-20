# 14J — Automatic Contour Detection and Image Enhancement

Status: **Implemented**

## Purpose

Add an assistive OpenCV step to Product Box 3D Textures:

- automatically detect a likely rectangular package-face contour from the uploaded original photo,
- prefill the existing four-corner editor with detected points,
- keep manual adjustment as the fallback/source of truth,
- apply image enhancement during perspective processing.

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

Detection is assistive and best-effort. It may use several OpenCV strategies such as:

- grayscale conversion,
- blur,
- Canny edges,
- dominant horizontal/vertical edges,
- contour approximation,
- fallback corner initialization.

The detection only preloads points. Users can always drag the points before processing.

## Image enhancement

Processing supports enhancement modes:

```text
none
basic
strong
```

Enhancement is applied after `warpPerspective` and before PNG encoding.

14J remains an OpenCV-based faithful processing phase. It should improve brightness, contrast, color and sharpness, but it does not reconstruct missing details like an AI model could.

## Frontend behavior

The Product Box faces modal shows a **Detect contour** button when an original photo exists.

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

## Limitation and next step

OpenCV cannot reliably reconstruct missing detail, fix blurred text perfectly, or create a polished near-commercial texture from a low-quality phone photo.

The next planned step is optional AI texture enhancement after OpenCV processing.

## Out of scope

- No generative AI in 14J.
- No automatic acceptance of detected points.
- No destructive modification of the original image.
- No separate `product_box_textures` table.
- No integration with inventory/purchases.
- No AI awareness yet.

## Next phase

```text
14K — AI Texture Enhancement architecture/design
```
