package com.tamias.ai.tool.handler;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.context.AiToolRequestContext;
import com.tamias.ai.tool.service.AiReadOnlyToolService;
import com.tamias.ai.tool.support.AiToolRoutingSupport;
import java.util.List;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class UserRoleOrganizationToolHandler extends AiToolRoutingSupport implements AiToolHandler {

    private final AiReadOnlyToolService readOnlyToolService;

    public UserRoleOrganizationToolHandler(AiReadOnlyToolService readOnlyToolService) {
        this.readOnlyToolService = readOnlyToolService;
    }

    @Override
    public Optional<AiToolAnswer> tryHandle(AiToolRequestContext context) {
        return tryHandleAdminRoleOrganizationQuestion(context.question(), context.normalizedQuestion());
    }


private Optional<AiToolAnswer> tryHandleAdminRoleOrganizationQuestion(String question, String normalized) {
        if (isOrganizationAdminToolQuestion(normalized)) {
            if (isOrganizationModuleUsageQuestion(normalized)) {
                return Optional.of(readOnlyToolService.organizationModuleUsageSummary());
            }
            if (isOrganizationUserCountQuestion(normalized)) {
                return Optional.of(readOnlyToolService.organizationUserCount());
            }
        }

        if (isRolePermissionSummaryQuestion(normalized)) {
            return Optional.of(readOnlyToolService.rolePermissionSummary(question));
        }

        if (isUserAdminToolQuestion(normalized)) {
            if (isUserAccessSummaryQuestion(normalized)) {
                return Optional.of(readOnlyToolService.userAccessSummary(question));
            }
            if (isUsersByRoleQuestion(normalized)) {
                return Optional.of(readOnlyToolService.usersByRole(question));
            }
            if (isActiveUsersQuestion(normalized)) {
                return Optional.of(readOnlyToolService.activeUsers());
            }
            if (isInactiveUsersQuestion(normalized)) {
                return Optional.of(readOnlyToolService.inactiveUsers());
            }
            return Optional.of(readOnlyToolService.searchUsers(question));
        }

        if (isRoleAdminToolQuestion(normalized)) {
            return Optional.of(readOnlyToolService.roleList());
        }

        return Optional.empty();
    }
}
