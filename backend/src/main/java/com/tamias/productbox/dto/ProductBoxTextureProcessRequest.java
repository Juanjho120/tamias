package com.tamias.productbox.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ProductBoxTextureProcessRequest(
    @Valid
    @NotNull(message = "Top-left point is required")
    ProductBoxTexturePointRequest topLeft,

    @Valid
    @NotNull(message = "Top-right point is required")
    ProductBoxTexturePointRequest topRight,

    @Valid
    @NotNull(message = "Bottom-right point is required")
    ProductBoxTexturePointRequest bottomRight,

    @Valid
    @NotNull(message = "Bottom-left point is required")
    ProductBoxTexturePointRequest bottomLeft
) { }
