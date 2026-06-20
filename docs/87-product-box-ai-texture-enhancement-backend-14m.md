# 14M — AI Texture Enhancement Backend

Status: **Implemented / backend ready for frontend integration**

## Purpose

Implement the backend flow that generates an optional AI-enhanced Product Box face texture from the existing OpenCV processed texture.

The OpenCV processed texture remains the faithful baseline. The AI-enhanced texture is a separate optional draft and is not accepted automatically.

## Scope

Implemented in this phase:

- Add a real `OpenAiProductBoxAiTextureEnhancementProvider` behind the 14L provider abstraction.
- Add backend endpoint to generate an AI-enhanced draft from `processed_s3_key`.
- Store the enhanced draft under the existing AI metadata columns from 14L.
- Add backend endpoint to accept the AI-enhanced texture as the active `s3_key`.
- Add backend endpoint to discard a non-active AI-enhanced draft.
- Keep OpenCV processed texture metadata intact for comparison.
- Keep hard-delete cleanup rules intact.

Not implemented in this phase:

- No frontend buttons or preview UI.
- No side-by-side OpenCV vs AI comparison screen.
- No AI awareness tools for TAMI.
- No automatic acceptance of AI output.

## API endpoints

```text
POST   /api/v1/product-box-models/{id}/faces/{faceName}/texture/ai-enhance
POST   /api/v1/product-box-models/{id}/faces/{faceName}/texture/ai-enhance/accept
DELETE /api/v1/product-box-models/{id}/faces/{faceName}/texture/ai-enhance
```

## Generate enhanced texture

`ai-enhance` requires an existing OpenCV processed texture:

```text
product_box_model_faces.processed_s3_key
```

The backend loads the processed texture from storage, sends it to the configured AI provider, normalizes the result back to the OpenCV processed texture dimensions, uploads the AI-enhanced draft to S3, and stores metadata.

S3 path:

```text
{organizationId}/catalogs/product_box_models/{productBoxModelId}/faces/{faceName}/enhanced/{filename}
```

Updated fields:

```text
ai_enhanced_s3_key
ai_enhanced_filepath
ai_enhanced_filename
ai_enhanced_content_type
ai_enhanced_size_bytes
ai_enhanced_width_px
ai_enhanced_height_px
ai_enhancement_status = GENERATED
ai_enhancement_provider
ai_enhancement_model
ai_enhancement_prompt_version
ai_enhancement_error = null
ai_enhanced_at
```

## Accept enhanced texture

`ai-enhance/accept` promotes the AI-enhanced draft to the active viewer texture:

```text
product_box_model_faces.s3_key = ai_enhanced_s3_key
product_box_model_faces.active_texture_source = ai_enhanced
product_box_model_faces.ai_enhancement_status = ACCEPTED
product_box_model_faces.texture_status = ACCEPTED
```

The existing OpenCV `processed_s3_key` is not deleted. It remains available for comparison and fallback.

If the previous active texture is an orphan direct upload, it may be deleted. If it is the OpenCV processed texture, it is retained.

## Discard enhanced draft

`DELETE /texture/ai-enhance` removes only a non-active AI-enhanced draft.

If the AI-enhanced texture is currently active, the delete endpoint rejects the request to avoid breaking the viewer. The user must accept another texture or delete the whole face instead.

## Provider configuration

The implemented provider uses OpenAI Image API edits and reads the existing Spring AI OpenAI key:

```text
spring.ai.openai.api-key
```

Optional configuration:

```text
tamias.product-box.ai-texture.openai.enabled=true
tamias.product-box.ai-texture.openai.model=gpt-image-2
tamias.product-box.ai-texture.openai.quality=medium
tamias.product-box.ai-texture.openai.size=auto
tamias.product-box.ai-texture.openai.output-format=png
```

The provider prompt instructs the model to preserve layout, logos, text, barcode, icons, proportions and boundaries, and to improve only brightness, contrast, color, denoising, deblurring, sharpening and text edge clarity.

## Safety and fidelity

The AI-enhanced output is not considered the canonical source of truth. It is an optional visual enhancement.

Rules:

- Do not overwrite the original uploaded photo.
- Do not overwrite the OpenCV processed texture.
- Do not auto-accept the AI output.
- Always require explicit user acceptance.
- Preserve `s3_key` as the active texture used by Three.js.
- Keep hard-delete behavior for active/original/processed/enhanced S3 objects.

## Next phase

```text
14N — Angular AI enhanced preview and accept workflow
```

14N should expose UI to generate, preview, compare, accept and discard AI-enhanced textures.
