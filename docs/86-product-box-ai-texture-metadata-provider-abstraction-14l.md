# 14L — AI Texture Metadata and Backend Provider Abstraction

Status: **Implemented / backend foundation ready for 14M**

## Purpose

Prepare TAMIAS for optional AI-assisted Product Box texture enhancement without calling a real AI provider yet.

This phase adds the database metadata, enums and provider-neutral backend abstraction needed by the next phase:

```text
14M — AI Texture enhancement backend
```

The existing OpenCV pipeline remains unchanged and remains the faithful baseline.

## Scope

Implemented in this phase:

- Extend `product_box_model_faces` with AI-enhancement metadata.
- Track which texture source is currently active in `s3_key`.
- Add Java enums/converters for AI enhancement status and active texture source.
- Expose AI enhancement metadata in `ProductBoxModelFaceResponse`.
- Update hard-delete cleanup to include future `ai_enhanced_s3_key`.
- Add provider-neutral backend interfaces and a no-op provider.

Not implemented in this phase:

- No real AI API call.
- No AI-generated image output.
- No frontend AI preview.
- No accept AI workflow.
- No provider credentials.

## Data model changes

Migration:

```text
V36__add_product_box_ai_texture_metadata.sql
```

New columns in `product_box_model_faces`:

```text
ai_enhanced_s3_key VARCHAR(500)
ai_enhanced_filepath VARCHAR(300)
ai_enhanced_filename VARCHAR(255)
ai_enhanced_content_type VARCHAR(100)
ai_enhanced_size_bytes BIGINT
ai_enhanced_width_px INTEGER
ai_enhanced_height_px INTEGER
ai_enhancement_status VARCHAR(30) NOT NULL DEFAULT 'NOT_REQUESTED'
ai_enhancement_provider VARCHAR(80)
ai_enhancement_model VARCHAR(120)
ai_enhancement_prompt_version VARCHAR(80)
ai_enhancement_error TEXT
ai_enhanced_at TIMESTAMP
active_texture_source VARCHAR(30) NOT NULL DEFAULT 'unknown'
```

Indexes:

```text
idx_product_box_model_faces_ai_status
idx_product_box_model_faces_active_texture_source
```

## Status values

`ProductBoxAiEnhancementStatus`:

```text
NOT_REQUESTED
REQUESTED
PROCESSING
GENERATED
ACCEPTED
FAILED
```

`ProductBoxActiveTextureSource`:

```text
unknown
direct_upload
opencv_processed
ai_enhanced
```

## Active texture rule

The viewer still uses:

```text
product_box_model_faces.s3_key
```

`active_texture_source` only explains where the active `s3_key` came from:

- `direct_upload`: uploaded directly through the original face image upload endpoint.
- `opencv_processed`: accepted from OpenCV processed texture.
- `ai_enhanced`: accepted from the AI-enhanced texture flow in a future phase.
- `unknown`: legacy/default value for rows created before source tracking.

## S3 structure

Future AI-enhanced drafts use:

```text
{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}/enhanced/{filename}
```

This phase does not upload enhanced files yet, but it prepares metadata and cleanup for that path.

## Hard-delete behavior

Face/model deletes must delete all S3 objects before removing DB rows or soft-deleting parent models:

```text
s3_key
original_s3_key
processed_s3_key
ai_enhanced_s3_key
```

If S3 delete fails, the DB mutation must not proceed.

## Backend provider abstraction

Added provider-neutral types:

```text
ProductBoxAiTextureEnhancementProvider
ProductBoxAiTextureEnhancementRequest
ProductBoxAiTextureEnhancementResult
NoopProductBoxAiTextureEnhancementProvider
```

The no-op provider intentionally reports unavailable and throws a clear error if called.

A real provider must be added in 14M behind the same interface so Product Box domain services do not depend directly on a vendor SDK.

## Next phase

```text
14M — AI Texture enhancement backend
```

14M should:

- require an existing `processed_s3_key`,
- call the configured AI provider,
- upload the enhanced result to S3,
- populate `ai_enhanced_*` metadata,
- set `ai_enhancement_status = GENERATED`,
- never auto-accept the AI-enhanced output.
