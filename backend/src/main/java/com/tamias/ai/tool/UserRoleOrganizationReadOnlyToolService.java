package com.tamias.ai.tool;

import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserRoleOrganizationReadOnlyToolService extends AiReadOnlyToolSupport {

    public UserRoleOrganizationReadOnlyToolService(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer activeUsers() {
        return super.activeUsers();
    }

    public AiToolAnswer inactiveUsers() {
        return super.inactiveUsers();
    }

    public AiToolAnswer searchUsers(String userQuestion) {
        return super.searchUsers(userQuestion);
    }

    public AiToolAnswer usersByRole(String userQuestion) {
        return super.usersByRole(userQuestion);
    }

    public AiToolAnswer userAccessSummary(String userQuestion) {
        return super.userAccessSummary(userQuestion);
    }

    public AiToolAnswer roleList() {
        return super.roleList();
    }

    public AiToolAnswer rolePermissionSummary(String userQuestion) {
        return super.rolePermissionSummary(userQuestion);
    }

    public AiToolAnswer organizationUserCount() {
        return super.organizationUserCount();
    }

    public AiToolAnswer organizationModuleUsageSummary() {
        return super.organizationModuleUsageSummary();
    }

}
