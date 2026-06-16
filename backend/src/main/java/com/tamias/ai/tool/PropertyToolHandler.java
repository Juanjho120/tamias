package com.tamias.ai.tool;

import java.util.List;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(130)
public class PropertyToolHandler extends AiToolRoutingSupport implements AiToolHandler {

    private final AiReadOnlyToolService readOnlyToolService;

    public PropertyToolHandler(AiReadOnlyToolService readOnlyToolService) {
        this.readOnlyToolService = readOnlyToolService;
    }

    @Override
    public Optional<AiToolAnswer> tryHandle(AiToolRequestContext context) {
        return tryHandlePropertyQuestion(context.question(), context.normalizedQuestion());
    }


private Optional<AiToolAnswer> tryHandlePropertyQuestion(String question, String normalized) {
        if (!isPropertyQuestion(normalized)) {
            return Optional.empty();
        }
        if (isActivePropertyQuestion(normalized)) {
            return Optional.of(readOnlyToolService.activeProperties());
        }
        if (isInactivePropertyQuestion(normalized)) {
            return Optional.of(readOnlyToolService.inactiveProperties());
        }
        if (isPropertyImagesQuestion(normalized)) {
            return Optional.of(readOnlyToolService.propertyImagesSummary(question));
        }
        if (isPropertyOperationalOverviewQuestion(normalized)) {
            return Optional.of(readOnlyToolService.propertyOperationalOverview());
        }
        if (isPropertySummaryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.propertySummary(question));
        }
        return Optional.of(readOnlyToolService.searchProperties(question));
    }
}
