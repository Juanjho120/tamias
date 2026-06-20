# 14N — Angular AI Enhanced Preview and Accept Workflow

Status: **Implemented**

## Purpose

Expose the 14M AI texture enhancement backend in the Product Box face modal so users can generate, preview, compare, accept and discard an optional AI-enhanced texture.

OpenCV remains the faithful baseline. AI enhancement is optional and must be explicitly accepted by the user before it becomes the active Three.js texture.

## Scope

Implemented in this phase:

- Extend the Product Box frontend model with AI-enhanced texture metadata.
- Add Product Box service methods for AI-enhanced texture generation, acceptance and discard.
- Add an AI-enhanced preview panel to the Product Box faces modal.
- Allow generating/regenerating an AI-enhanced draft from the OpenCV processed texture.
- Allow accepting the AI-enhanced draft as the active face texture.
- Allow discarding a non-active AI-enhanced draft.
- Keep the OpenCV processed texture visible for comparison.
- Keep the active texture preview driven by `imageUrl`, which maps to `product_box_model_faces.s3_key`.

Not implemented in this phase:

- No backend changes.
- No new migrations.
- No direct AI prompt editing from the UI.
- No TAMI tools for Product Box Models yet.
- No integration with Inventory/Purchases yet.

## Frontend workflow

The face modal now shows up to four image states per face:

```text
Active texture   -> product_box_model_faces.s3_key / imageUrl
Original photo   -> original_s3_key / originalImageUrl
OpenCV texture   -> processed_s3_key / processedImageUrl
AI-enhanced      -> ai_enhanced_s3_key / aiEnhancedImageUrl
```

User flow:

```text
Upload original
→ Adjust/detect corners
→ Process OpenCV texture
→ Generate AI-enhanced draft
→ Compare OpenCV vs AI-enhanced preview
→ Accept AI-enhanced texture or keep OpenCV
```

## API endpoints used

```text
POST   /api/v1/product-box-models/{id}/faces/{faceName}/texture/ai-enhance
POST   /api/v1/product-box-models/{id}/faces/{faceName}/texture/ai-enhance/accept
DELETE /api/v1/product-box-models/{id}/faces/{faceName}/texture/ai-enhance
```

## Acceptance rules

- The generate button requires an existing OpenCV processed texture.
- AI output is never auto-accepted.
- Accepting AI promotes `ai_enhanced_s3_key` to `s3_key` through the backend.
- Discarding AI is only allowed when the AI-enhanced draft is not the active texture.
- The viewer continues using `imageUrl` only, so the viewer does not need to know which source is active.

## UI notes

The AI preview panel uses the same CORS-safe image loading pattern as the other Product Box previews:

```html
crossorigin="anonymous"
```

The modal disables conflicting actions while AI generation, acceptance or discard is running.

## Validation checklist

- Open a Product Box face with an OpenCV processed texture.
- Generate an AI-enhanced texture.
- Confirm the AI preview appears with filename, size, provider/model metadata and timestamp.
- Accept the AI texture.
- Confirm active texture preview updates.
- Confirm the 3D viewer uses the accepted AI texture.
- Generate another AI draft only when it is safe to do so.
- Discard a non-active AI draft.
- Confirm backend keeps OpenCV processed texture available for comparison/fallback.

## Next phase

```text
14O — Integration with Inventory/Purchases
```
