# 9N — Admin-only user, role and organization tools

This phase expands TAMIAS AI Tool Calling with read-only administrative tools for users, roles and organization-level summaries.

## Scope

Implemented tools:

- `user.search`
- `user.activeUsers`
- `user.inactiveUsers`
- `user.byRole`
- `user.accessSummary`
- `role.list`
- `role.permissionSummary`
- `organization.userCount`
- `organization.moduleUsageSummary`

Existing non-admin tools remain available:

- `user.currentProfile`
- `organization.currentSummary`

## Security rules

- Admin-only tools require the authenticated user to have the `ADMINISTRATOR` role in the current organization.
- Non-admin users receive a safe refusal message.
- All queries are scoped by backend-owned `organization_id` resolved from `CurrentUserService`.
- The assistant does not expose passwords, password hashes, JWTs, tokens or internal security secrets.
- The tools are read-only and do not create, update or delete users, roles or organization records.

## Notes

The current schema has organization-level roles (`roles`, `user_organizations`) but does not include a dedicated granular permission table. `role.permissionSummary` therefore summarizes access based on configured role metadata and known role semantics.

## Suggested smoke tests

Admin user:

- ¿Qué usuarios activos tengo?
- ¿Qué usuarios inactivos tengo?
- ¿Qué usuarios son administradores?
- ¿Qué permisos tiene Maintenance Staff?
- ¿Qué roles existen?
- ¿Cuántos usuarios tiene mi organización?
- ¿Qué módulos estamos usando más?

Non-admin user:

- ¿Qué usuarios activos tengo?
- ¿Qué permisos tiene Administrator?

Expected result for non-admin users: a safe admin-only refusal.
