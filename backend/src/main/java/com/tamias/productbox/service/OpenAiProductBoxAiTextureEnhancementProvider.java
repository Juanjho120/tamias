package com.tamias.productbox.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tamias.common.exception.BadRequestException;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class OpenAiProductBoxAiTextureEnhancementProvider implements ProductBoxAiTextureEnhancementProvider {

    private static final String PROVIDER_NAME = "openai";
    private static final String DEFAULT_PROMPT_VERSION = "product-box-texture-enhancement-v1";

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final String quality;
    private final String size;
    private final String outputFormat;
    private final boolean enabled;

    public OpenAiProductBoxAiTextureEnhancementProvider(
        RestClient.Builder restClientBuilder,
        @Value("${spring.ai.openai.api-key:}") String apiKey,
        @Value("${tamias.product-box.ai-texture.openai.model:gpt-image-2}") String model,
        @Value("${tamias.product-box.ai-texture.openai.quality:medium}") String quality,
        @Value("${tamias.product-box.ai-texture.openai.size:auto}") String size,
        @Value("${tamias.product-box.ai-texture.openai.output-format:png}") String outputFormat,
        @Value("${tamias.product-box.ai-texture.openai.enabled:true}") boolean enabled
    ) {
        this.restClient = restClientBuilder.baseUrl("https://api.openai.com").build();
        this.apiKey = apiKey;
        this.model = model;
        this.quality = quality;
        this.size = size;
        this.outputFormat = outputFormat;
        this.enabled = enabled;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    @Override
    public ProductBoxAiTextureEnhancementResult enhance(ProductBoxAiTextureEnhancementRequest request) {
        if (!isAvailable()) {
            throw new BadRequestException("OpenAI product box AI texture enhancement is not configured");
        }
        if (request.processedTextureBytes() == null || request.processedTextureBytes().length == 0) {
            throw new BadRequestException("Processed product box texture bytes are required for AI enhancement");
        }

        try {
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("model", model);
            bodyBuilder.part("prompt", buildPrompt(request));
            addOptionalPart(bodyBuilder, "quality", quality);
            addOptionalPart(bodyBuilder, "size", size);
            addOptionalPart(bodyBuilder, "output_format", outputFormat);

            ByteArrayResource imageResource = new ByteArrayResource(request.processedTextureBytes()) {
                @Override
                public String getFilename() {
                    return request.processedTextureFilename() != null && !request.processedTextureFilename().isBlank()
                        ? request.processedTextureFilename()
                        : "processed-texture.png";
                }
            };

            bodyBuilder
                .part("image[]", imageResource)
                .filename(imageResource.getFilename())
                .contentType(parseMediaType(request.processedTextureContentType()));

            MultiValueMap<String, org.springframework.http.HttpEntity<?>> multipartBody = bodyBuilder.build();

            JsonNode response = restClient.post()
                .uri("/v1/images/edits")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .headers(headers -> headers.setBearerAuth(apiKey))
                .body(multipartBody)
                .retrieve()
                .body(JsonNode.class);

            String b64Json = extractBase64Image(response);
            byte[] enhancedBytes = Base64.getDecoder().decode(b64Json);
            String filename = "ai-enhanced-" + request.faceName().getValue() + ".png";

            return new ProductBoxAiTextureEnhancementResult(
                enhancedBytes,
                filename,
                MediaType.IMAGE_PNG_VALUE,
                null,
                null,
                PROVIDER_NAME,
                model,
                request.promptVersion() != null && !request.promptVersion().isBlank()
                    ? request.promptVersion()
                    : DEFAULT_PROMPT_VERSION
            );
        } catch (RestClientResponseException ex) {
            throw new BadRequestException("OpenAI product box AI texture enhancement failed: " + sanitizeProviderError(ex));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("OpenAI product box AI texture enhancement returned invalid image data");
        } catch (RuntimeException ex) {
            throw new BadRequestException("OpenAI product box AI texture enhancement failed: " + ex.getMessage());
        }
    }

    private void addOptionalPart(MultipartBodyBuilder bodyBuilder, String name, String value) {
        if (value != null && !value.isBlank()) {
            bodyBuilder.part(name, value);
        }
    }

    private MediaType parseMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.IMAGE_PNG;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (RuntimeException ignored) {
            return MediaType.IMAGE_PNG;
        }
    }

    private String extractBase64Image(JsonNode response) {
        JsonNode data = response != null ? response.path("data") : null;
        if (data == null || !data.isArray() || data.isEmpty()) {
            throw new BadRequestException("OpenAI product box AI texture enhancement did not return image data");
        }

        String b64Json = data.get(0).path("b64_json").asText(null);
        if (b64Json == null || b64Json.isBlank()) {
            throw new BadRequestException("OpenAI product box AI texture enhancement response did not include b64_json");
        }
        return b64Json;
    }

    private String sanitizeProviderError(RestClientResponseException ex) {
        String responseBody = ex.getResponseBodyAsString();
        if (responseBody == null || responseBody.isBlank()) {
            return ex.getStatusCode() + " " + ex.getStatusText();
        }
        String compact = responseBody.replaceAll("\\s+", " ").trim();
        return compact.length() > 500 ? compact.substring(0, 500) : compact;
    }

    private String buildPrompt(ProductBoxAiTextureEnhancementRequest request) {
        String faceName = request.faceName() != null ? request.faceName().getValue() : "unknown";
        return "Enhance this product box face texture for use on a 3D box model. "
            + "Face: " + faceName + ". "
            + "Preserve the exact package layout, logos, brand names, text content, spelling, barcode, icons, symbols, proportions, and boundaries. "
            + "Do not translate, rewrite, add, remove, replace, invent, or redesign any text or visual elements. "
            + "Improve only visual quality: brightness, contrast, color balance, denoising, mild deblurring, sharpening, and text edge clarity. "
            + "Keep the image as a flat, front-facing rectangular product package texture that fills the full canvas. "
            + "Avoid adding shadows, backgrounds, perspective, 3D effects, mockups, borders, or new objects. "
            + "The output must remain faithful to the input texture and suitable for direct use as a Three.js box face texture.";
    }
}
