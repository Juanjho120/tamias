# 14N.1 — Product Box OpenCV Runtime Controls and Testing Profile

## Status

Completed.

## Goal

Add runtime controls for Product Box OpenCV-dependent texture processing so TAMIAS can run safely in environments where the native OpenCV runtime is disabled or unavailable.

This phase prevents Product Box texture processing requests from failing with raw native runtime errors such as:

- `UnsatisfiedLinkError`
- `NoClassDefFoundError`
- `ExceptionInInitializerError`

## Runtime configuration

Backend property:

```yaml
tamias:
  product-box:
    opencv:
      enabled: ${PRODUCT_BOX_OPENCV_ENABLED:true}
```

Environment variable:

```env
PRODUCT_BOX_OPENCV_ENABLED=true
```

## Behavior

### When OpenCV is enabled and available

- Texture contour detection is available.
- Perspective correction is available.
- `/api/v1/product-box-models/capabilities` returns `opencvEnabled=true`.

### When OpenCV is disabled

- OpenCV-dependent operations return a controlled bad request response.
- `/api/v1/product-box-models/capabilities` returns `opencvEnabled=false`.
- The frontend can hide or disable OpenCV-dependent actions.
- Product Box original face upload remains available because it does not require OpenCV.

### When OpenCV is configured but native loading fails

- The backend does not crash with a raw native runtime error.
- The failure is logged by `ProductBoxOpenCvRuntimeService`.
- `/api/v1/product-box-models/capabilities` returns `opencvEnabled=false`.
- OpenCV-dependent operations return a controlled bad request response.

## Backend implementation

The runtime guard is centralized in:

```text
backend/src/main/java/com/tamias/productbox/service/ProductBoxOpenCvRuntimeService.java
```

The capabilities service delegates OpenCV availability checks to that runtime guard:

```text
backend/src/main/java/com/tamias/productbox/service/ProductBoxRuntimeCapabilitiesService.java
```

The Product Box face workflow already calls `requireOpenCvEnabled()` before running OpenCV-dependent operations such as contour detection and texture processing.

## Profiles

### local

OpenCV is enabled by default:

```env
PRODUCT_BOX_OPENCV_ENABLED=true
```

### prod

OpenCV is enabled by default, but can be disabled per deployment:

```env
PRODUCT_BOX_OPENCV_ENABLED=false
```

This is useful for Render if native OpenCV loading becomes unstable in a specific runtime.

### ci / test

OpenCV is disabled by default:

```env
PRODUCT_BOX_OPENCV_ENABLED=false
```

This avoids test failures in environments where native OpenCV libraries are unavailable.

## Capabilities endpoint

Endpoint:

```http
GET /api/v1/product-box-models/capabilities
```

Expected response when OpenCV is available:

```json
{
  "opencvEnabled": true,
  "aiTextureEnhancementEnabled": false,
  "opencvDisabledMessage": null,
  "aiTextureEnhancementDisabledMessage": "Product Box AI texture enhancement is disabled or not configured in this environment."
}
```

Expected response when OpenCV is disabled:

```json
{
  "opencvEnabled": false,
  "aiTextureEnhancementEnabled": false,
  "opencvDisabledMessage": "Product Box OpenCV texture processing is disabled in this environment.",
  "aiTextureEnhancementDisabledMessage": "Product Box AI texture enhancement is disabled or not configured in this environment."
}
```

Expected response when OpenCV is configured but unavailable:

```json
{
  "opencvEnabled": false,
  "aiTextureEnhancementEnabled": false,
  "opencvDisabledMessage": "Product Box OpenCV texture processing is not available in this runtime.",
  "aiTextureEnhancementDisabledMessage": "Product Box AI texture enhancement is disabled or not configured in this environment."
}
```

## Frontend behavior

When `opencvEnabled=false`, the UI should disable or hide:

- automatic contour detection
- perspective correction
- processed texture generation

Original image upload and accepted texture viewing should remain available.

## Verification checklist

- Backend starts with `PRODUCT_BOX_OPENCV_ENABLED=false`.
- Backend starts with `PRODUCT_BOX_OPENCV_ENABLED=true`.
- CI/test profile does not require native OpenCV.
- `/api/v1/product-box-models/capabilities` returns the expected flags.
- OpenCV processing returns controlled errors when disabled.
- Product Box original face upload still works when OpenCV is disabled.
- Render deployment can disable OpenCV with `PRODUCT_BOX_OPENCV_ENABLED=false` if native loading becomes unstable.

## Suggested commands

Backend tests:

```bash
cd backend
./mvnw test
```

Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd test
```

Run backend with OpenCV disabled:

```powershell
$env:PRODUCT_BOX_OPENCV_ENABLED="false"
.\mvnw.cmd spring-boot:run
```

Prod-like with OpenCV disabled:

```bash
PRODUCT_BOX_OPENCV_ENABLED=false docker compose -f docker-compose.prod-like.yml up --build
```
