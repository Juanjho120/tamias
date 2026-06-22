# 14N.1 — Product Box OpenCV runtime controls and testing profile

Status: **Implemented**

## Purpose

Keep Product Box OpenCV features available in production while allowing low-memory testing/staging environments to disable them safely.

OpenCV Java processing is useful for Product Box textures, but it is memory-heavy compared with the rest of the backend. On Render, a 512 MB service can run out of memory when detecting contours or processing textures. Production currently requires the 2 GB instance for reliable OpenCV execution.

## Decision

Add runtime capability flags for Product Box image processing.

Production:

```text
TAMIAS_PRODUCT_BOX_OPENCV_ENABLED=true
```

Testing/staging when running on a 512 MB Render service:

```text
TAMIAS_PRODUCT_BOX_OPENCV_ENABLED=false
TAMIAS_PRODUCT_BOX_AI_TEXTURE_OPENAI_ENABLED=false
```

The backend default remains enabled for OpenCV unless the environment disables it explicitly.

## Backend behavior

New capability endpoint:

```http
GET /api/v1/product-box-models/capabilities
```

Example response when enabled:

```json
{
  "opencvEnabled": true,
  "aiTextureEnhancementEnabled": true,
  "opencvDisabledMessage": null,
  "aiTextureEnhancementDisabledMessage": null
}
```

Example response for low-cost testing:

```json
{
  "opencvEnabled": false,
  "aiTextureEnhancementEnabled": false,
  "opencvDisabledMessage": "Product Box OpenCV texture processing is disabled in this environment.",
  "aiTextureEnhancementDisabledMessage": "Product Box AI texture enhancement is disabled or not configured in this environment."
}
```

When OpenCV is disabled, these endpoints fail fast with a controlled `BadRequestException` before loading native OpenCV or reading large image data:

```http
POST /api/v1/product-box-models/{id}/faces/{faceName}/texture/detect-contour
POST /api/v1/product-box-models/{id}/faces/{faceName}/texture/process
```

This prevents low-memory environments from crashing due to OpenCV native/off-heap memory usage.

## Frontend behavior

The Product Box faces modal reads the runtime capabilities endpoint and disables expensive actions when needed:

```text
Detect contour
Process texture
Generate AI enhancement
```

The buttons remain visible where possible, but disabled, so QA can clearly see that the feature is unavailable in that environment rather than thinking the UI is broken.

## What testing can still validate with OpenCV disabled

```text
Product Box CRUD
Face direct upload
Original texture upload
S3 storage and presigned previews
Face delete hard-delete behavior
Three.js viewer with accepted/direct textures
AI metadata visibility when data already exists
```

## What testing should not validate on 512 MB

```text
OpenCV contour detection
OpenCV perspective correction
OpenCV texture enhancement
AI enhancement flow that requires a freshly processed OpenCV texture
```

Use production or a temporary 2 GB environment for those cases.

## Render notes

For production, keep the backend on the Debian/Ubuntu-family Docker runtime documented in `docs/89-backend-render-opencv-glibc-runtime.md` and use at least 2 GB RAM for OpenCV processing.

For testing, keep the lower-cost service and disable OpenCV through environment variables instead of scaling to another 2 GB instance.
