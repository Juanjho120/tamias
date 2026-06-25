package com.tamias.ai.tool.handler;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.context.AiToolRequestContext;
import com.tamias.ai.tool.service.MaintenanceLastByPersonReadOnlyToolService;
import com.tamias.ai.tool.support.AiToolRoutingSupport;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(55)
public class MaintenanceLastByPersonToolHandler extends AiToolRoutingSupport implements AiToolHandler {

    private final MaintenanceLastByPersonReadOnlyToolService service;

    public MaintenanceLastByPersonToolHandler(MaintenanceLastByPersonReadOnlyToolService service) {
        this.service = service;
    }

    @Override
    public Optional<AiToolAnswer> tryHandle(AiToolRequestContext context) {
        String normalized = context.normalizedQuestion();

        if (!isLastMaintenanceByPersonQuestion(normalized)) {
            return Optional.empty();
        }

        return Optional.of(service.lastMaintenanceByPerson(context.question()));
    }

    private boolean isLastMaintenanceByPersonQuestion(String normalized) {
        if (!containsAny(normalized, "mantenimiento", "mantenimientos")) {
            return false;
        }

        if (!containsAny(normalized, "ultimo", "ultima", "último", "última", "reciente", "cuando", "cuándo")) {
            return false;
        }

        return containsAny(
                normalized,
                "hizo",
                "realizo",
                "realizó",
                "ejecuto",
                "ejecutó",
                "atendio",
                "atendió",
                "persona",
                "responsable",
                "involucrado",
                "involucrada",
                "por"
        );
    }
}
