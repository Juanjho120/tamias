# 14K — Product Box AI Texture Enhancement

Status: **Design ready / implementation next only if approved**

## Purpose

Add an optional AI enhancement step for Product Box face textures without replacing the existing OpenCV pipeline.

The current Product Box texture pipeline already provides the faithful baseline:

```text
original photo
→ manual/automatic corner selection
→ OpenCV perspective correction
→ OpenCV processed preview
→ user accept
→ active texture used by Three.js
```

This phase adds an optional enhanced output:

```text
OpenCV processed texture
→ AI texture enhancement
→ side-by-side preview
→ user chooses faithful OpenCV texture or AI-enhanced texture
→ accepted texture remains product_box_model_faces.s3_key
```

## Why this is separate from 14J

OpenCV is good for geometry and faithful image operations:

- perspective correction,
- cropping,
- resizing to the real face aspect ratio,
- conservative brightness/contrast/color correction,
- automatic contour assistance.

OpenCV does not reconstruct missing detail, improve text continuity, deblur aggressively, or create a near-commercial texture from a phone photo. AI enhancement can improve visual quality, but it can also alter product text, logos, edges, colors, symbols, and small details.

For that reason, AI enhancement must be optional, explicit, non-destructive and user-accepted.

## Key decision

Do not replace the existing OpenCV result.

Use this model:

```text
OpenCV processed texture = faithful baseline
AI-enhanced texture      = optional visual enhancement
active texture s3_key    = whichever result the user explicitly accepts
```

The active texture used by Three.js remains:

```text
product_box_model_faces.s3_key
```

This keeps compatibility with the existing Product Box Viewer.

## Non-goals

This phase must not:

- remove or weaken the OpenCV workflow,
- auto-accept AI-generated output,
- overwrite the original photo,
- overwrite the OpenCV processed texture,
- use AI output as the only source of truth,
- expose private S3 objects directly,
- bypass organization scoping,
- create a separate canonical product box texture module unless the face lifecycle can no longer be represented by `product_box_model_faces`.

## Recommended data model extension

Extend `product_box_model_faces` instead of creating a new canonical texture table.

Potential new columns:

```text
ai_enhanced_s3_key VARCHAR(500)
ai_enhanced_filepath VARCHAR(300)
ai_enhanced_filename VARCHAR(255)
ai_enhanced_content_type VARCHAR(100)
ai_enhanced_size_bytes BIGINT
ai_enhanced_width_px INTEGER
ai_enhanced_height_px INTEGER
ai_enhancement_status VARCHAR(30) NOT NULL DEFAULT 'NONE'
ai_enhancement_provider VARCHAR(80)
ai_enhancement_model VARCHAR(120)
ai_enhancement_prompt_version VARCHAR(50)
ai_enhancement_error TEXT
ai_enhanced_at TIMESTAMP
active_texture_source VARCHAR(30) NOT NULL DEFAULT 'DIRECT_OR_OPENCV'
```

Suggested `ai_enhancement_status` values:

```text
NONE
REQUESTED
PROCESSING
ENHANCED
ACCEPTED
FAILED
```

Suggested `active_texture_source` values:

```text
DIRECT_UPLOAD
OPENCV_PROCESSED
AI_ENHANCED
```

`product_box_model_faces.s3_key` remains the active accepted texture key used by the viewer.

## S3 structure

Organization id remains the first path segment.

AI enhanced texture draft:

```text
{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}/enhanced/{filename}
```

The active accepted texture remains whatever is stored in:

```text
product_box_model_faces.s3_key
```

If the user accepts the AI-enhanced texture, `s3_key` may point to the enhanced object. If the user keeps the OpenCV texture, `s3_key` continues pointing to the processed/accepted OpenCV object.

## Hard delete rules

Product box face deletes must delete all face-related S3 objects before deleting the DB row:

```text
s3_key
original_s3_key
processed_s3_key
ai_enhanced_s3_key
```

If S3 delete fails for any required object, do not delete the DB row.

When replacing or regenerating an unaccepted AI-enhanced draft, delete the previous `ai_enhanced_s3_key` first or clean it up transactionally after successful replacement.

## Backend service design

Add a provider-neutral service layer, for example:

```text
ProductBoxAiTextureEnhancementService
ProductBoxAiTextureProvider
```

The domain service should not be tied directly to one vendor SDK.

Recommended input:

- Product box model id,
- face name,
- current OpenCV processed texture key,
- face dimensions/aspect ratio,
- optional enhancement mode.

Recommended output:

- enhanced S3 key,
- metadata,
- status,
- presigned preview URL.

The AI enhancement input should usually be the OpenCV processed texture, not the raw phone photo. OpenCV already performs the geometric correction; AI should focus on visual enhancement, denoising, deblurring, sharpening, color/contrast and text legibility.

## API design

Suggested endpoints:

```text
POST /api/v1/product-box-models/{id}/faces/{faceName}/texture/ai-enhance
POST /api/v1/product-box-models/{id}/faces/{faceName}/texture/ai-enhance/accept
DELETE /api/v1/product-box-models/{id}/faces/{faceName}/texture/ai-enhance
```

`ai-enhance` should require an existing processed OpenCV texture.

`ai-enhance/accept` should promote the AI-enhanced texture to the active `s3_key` only after user confirmation.

Delete should remove only the AI-enhanced draft unless that same object is currently active. If it is active, use the face delete workflow or a dedicated replacement workflow to avoid breaking viewer state.

## Frontend UX

The Product Box face modal should show three visual states:

```text
Original photo
OpenCV processed texture
AI-enhanced texture
```

The UI should let the user compare OpenCV vs AI side by side before accepting.

Recommended buttons:

```text
Generate enhanced texture
Use OpenCV texture
Use AI-enhanced texture
Discard AI enhancement
```

The viewer should continue using only the active accepted texture from `imageUrl` / `s3_key`.

## Safety and fidelity rules

AI-enhanced output must be labeled as enhanced.

The UI should not present AI-enhanced textures as a perfect copy of the original package. The user should decide whether the enhanced output is acceptable.

Important risks:

- text may be altered,
- small legal disclaimers may change,
- logos may be distorted,
- barcode/QR details may become inaccurate,
- product claims may be modified visually,
- colors may become more attractive but less faithful.

For operational visual reconstruction, this is acceptable only if the user explicitly accepts the enhanced version.

## Cost and performance controls

Add guardrails before implementation:

- only authenticated users can request enhancement,
- organization scoping is mandatory,
- limit image dimensions sent to the AI provider,
- restrict file types to PNG/JPG/WEBP,
- reject oversized files,
- log enhancement failures without exposing provider internals,
- avoid repeated enhancement calls without user action,
- consider future per-organization usage limits.

## Suggested implementation split

### 14K — AI Texture Enhancement architecture/design

Status: this document.

### 14L — AI Texture metadata and backend provider abstraction

Add DB metadata, enums, DTOs, provider-neutral service interfaces and no-op/mock implementation if needed.

No external AI call yet.

### 14M — AI Texture enhancement backend

Implement the actual backend enhancement endpoint and S3 storage.

The provider must receive the OpenCV processed texture and produce an enhanced image draft.

### 14N — Angular AI enhanced preview and accept workflow

Add side-by-side preview, generate/discard/accept actions and make the active viewer texture update only after acceptance.

### 14O — Integration with Inventory/Purchases

Move the previous Product Box integration phase here.

### 14P — AI awareness for Product Box Models

Move the previous metadata-only TAMI awareness phase here. TAMI may answer which models/faces exist and whether they have OpenCV/AI-enhanced textures, but it must not interpret image contents unless a later vision phase explicitly allows it.

## Acceptance criteria

- Existing OpenCV workflow still works unchanged.
- User can generate an optional AI-enhanced texture only after an OpenCV processed texture exists.
- AI-enhanced texture is stored separately in S3.
- User can preview OpenCV and AI-enhanced textures side by side.
- User can choose which texture becomes active.
- Active texture remains `product_box_model_faces.s3_key`.
- Deleting a face deletes original, processed, AI-enhanced and active S3 objects safely.
- No AI output is auto-accepted.
- All operations are scoped by organization.
- The feature is documented as optional visual enhancement, not as faithful reconstruction.

## Next phase

```text
14L — AI Texture metadata and backend provider abstraction
```
