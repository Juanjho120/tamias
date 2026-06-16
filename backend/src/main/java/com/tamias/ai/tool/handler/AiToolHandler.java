package com.tamias.ai.tool.handler;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.context.AiToolRequestContext;
import java.util.Optional;

public interface AiToolHandler {
    Optional<AiToolAnswer> tryHandle(AiToolRequestContext context);
}
