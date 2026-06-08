package com.tamias.task.dto;

import jakarta.validation.constraints.NotNull;

public record TaskItemCompletionRequest(
        @NotNull(message = "Completed is required")
        Boolean completed
) {
}
