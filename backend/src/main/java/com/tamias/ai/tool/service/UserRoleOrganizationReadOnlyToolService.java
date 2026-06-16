package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.repository.UserRoleOrganizationToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserRoleOrganizationReadOnlyToolService {

    private final UserRoleOrganizationToolRepository repository;

    public UserRoleOrganizationReadOnlyToolService(UserRoleOrganizationToolRepository repository) {
        this.repository = repository;
    }

    public AiToolAnswer activeUsers() {
        return repository.activeUsers();
    }

    public AiToolAnswer inactiveUsers() {
        return repository.inactiveUsers();
    }

    public AiToolAnswer searchUsers(String userQuestion) {
        return repository.searchUsers(userQuestion);
    }

    public AiToolAnswer usersByRole(String userQuestion) {
        return repository.usersByRole(userQuestion);
    }

    public AiToolAnswer userAccessSummary(String userQuestion) {
        return repository.userAccessSummary(userQuestion);
    }

    public AiToolAnswer roleList() {
        return repository.roleList();
    }

    public AiToolAnswer rolePermissionSummary(String userQuestion) {
        return repository.rolePermissionSummary(userQuestion);
    }

    public AiToolAnswer organizationUserCount() {
        return repository.organizationUserCount();
    }

    public AiToolAnswer organizationModuleUsageSummary() {
        return repository.organizationModuleUsageSummary();
    }
}
