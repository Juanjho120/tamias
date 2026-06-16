package com.tamias.ai.tool;

import java.util.Optional;

public interface AiToolHandler {
    Optional<AiToolAnswer> tryHandle(AiToolRequestContext context);
}
