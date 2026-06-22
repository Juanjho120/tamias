# Backend Render OpenCV glibc Runtime

Status: **Implemented**

## Purpose

Document the backend Docker runtime decision required by OpenCV Java in Render.

Product Box texture features use OpenCV Java for:

```text
perspective correction
automatic contour detection
image enhancement
```

These features load native OpenCV libraries at runtime.

## Problem observed in Render

When the backend runtime image is Alpine-based, invoking OpenCV functionality can fail with:

```text
NoClassDefFoundError: Could not initialize class nu.pattern.OpenCV$LocalLoader$Holder
UnsatisfiedLinkError: libopencv_java490.so: Error loading shared library libstdc++.so.6: No such file or directory
```

The error appears when the OpenCV native loader extracts and loads:

```text
libopencv_java490.so
```

## Decision

Use Ubuntu/Debian-family Eclipse Temurin images for the backend Dockerfile:

```dockerfile
FROM eclipse-temurin:21-jdk-jammy AS build
...
FROM eclipse-temurin:21-jre-jammy
```

Do not use Alpine for the backend runtime while OpenCV Java native processing is part of TAMIAS.

## Required runtime packages

The runtime image installs:

```text
ca-certificates
libstdc++6
libgomp1
```

Why:

- `libstdc++6` provides `libstdc++.so.6`, required by OpenCV native libraries.
- `libgomp1` supports OpenMP-linked native workloads used by some OpenCV builds.
- `ca-certificates` is kept for HTTPS calls from the backend.

## Affected features

This decision protects:

```text
Product Box OpenCV texture processing
Product Box automatic contour detection
Product Box OpenCV image enhancement
Future OpenCV-based image utilities
```

## Render deployment notes

After changing the Dockerfile base image or native runtime packages, deploy with:

```text
Manual Deploy -> Clear build cache & deploy
```

This avoids Render reusing old Alpine-based layers.

## Validation checklist

```text
[ ] Backend builds successfully in Render.
[ ] /actuator/health returns UP.
[ ] Product Box original image upload works.
[ ] Detect contour runs without OpenCV native loader error.
[ ] Process texture runs without OpenCV native loader error.
[ ] Render logs no longer show libstdc++.so.6 missing.
```

## Related docs

```text
docs/19-docker-production-readiness.md
docs/22-deployment-runbook.md
docs/27-first-deployment-troubleshooting.md
docs/81-product-box-opencv-perspective-correction-14g.md
docs/84-product-box-auto-contour-enhancement-14j.md
```
