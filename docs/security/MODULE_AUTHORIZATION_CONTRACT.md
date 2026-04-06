# Module Authorization Contract (Kernel Authorization Framework)

This contract defines how modules integrate domain authorization rules into the kernel-owned enforcement pipeline.

## Ownership split

Kernel owns:
- security context creation from authenticated request state
- authorization orchestration and enforcement (`KernelAuthorizationService`, `KernelAuthorizationEnforcer`)
- deny exception/response mapping (`KernelAccessDeniedException` -> `403 ACCESS_DENIED`)
- structured deny audit logging (`AuthorizationAuditLogger`)
- startup guardrails for protected integrations (`KernelProtectedAuthorizationIntegrationGuardrail`)

Module owns:
- protected `resourceType` values
- supported `AuthorizationAction` set per resource
- evaluator logic for domain/scope/tenant/record checks

Modules provide rules through `ModuleSecurityContributor`. Modules do not own an independent enforcement pipeline for protected standard paths.

## Module registration SPI

1. Provide a Spring bean implementing `ModuleSecurityContributor`.
2. Return the owning `moduleId()` value.
3. Register each protected resource through `ModuleSecurityRegistry.registerResourcePolicy(resourceType, supportedActions, evaluator)`.
4. Return `AuthorizationDecision.allow(...)` or `AuthorizationDecision.deny(...)` from the evaluator.

### Minimal example

```java
@Component
final class ExampleSecurityContributor implements ModuleSecurityContributor {

    @Override
    public String moduleId() {
        return "example-module";
    }

    @Override
    public void contribute(ModuleSecurityRegistry registry) {
        registry.registerResourcePolicy(
            "example-resource",
            EnumSet.of(AuthorizationAction.READ, AuthorizationAction.UPDATE),
            this::evaluate
        );
    }

    private AuthorizationDecision evaluate(AuthorizationRequest request) {
        Set<String> scopes = AuthorizationScopeExtractor.extractScopes(request);
        if (request.action() == AuthorizationAction.READ && scopes.contains("example.read")) {
            return AuthorizationDecision.allow("example.scope-policy");
        }
        return AuthorizationDecision.deny(DenyReason.RESOURCE_ACCESS_DENIED, "example.scope-policy");
    }
}
```

## Security context and request model

Evaluator input comes through `AuthorizationRequest` and includes:
- `securityContext.userId`
- `securityContext.tenantId` (set for tenant-protected operations with a valid active tenant)
- `securityContext.globalRoles`
- `securityContext.claims` (for example `scope`/`scp`)
- `action`, `moduleId`, `resourceType`, optional `resourceId`, optional `attributes`

## Tenant context rules (implemented)

- Token is authoritative for tenant scope.
- Route is the only explicit tenant selector in this story (`tenantId`, `tenant_id`, `tid`).
- If token scope contains exactly one tenant and no explicit route tenant is provided, that tenant becomes active.
- If token scope contains multiple tenants and the operation is tenant-protected, explicit route tenant selection is required.
- Explicit route tenant must be inside token-granted scope, otherwise access is denied.
- Minimal privileged cross-tenant override exists:
  - requires explicit privileged authority (`tenant_cross_access` suffix, for example `ROLE_tenant_cross_access`)
  - requires explicit target tenant in the route
  - does not allow bypass when token tenant scope is empty
- Failure semantics:
  - missing/invalid authentication -> `401`
  - authenticated request with invalid/missing/unauthorized tenant context -> `403`
- Default behavior is deny-by-default for tenant-protected operations.

## Kernel enforcement behavior on standard paths

- Kernel-managed standard paths call `KernelAuthorizationEnforcer` before business access.
- Current protected standard paths include:
  - `KernelStandardRecordController` (`READ`, `LIST`, `CREATE`, `UPDATE`, `DELETE`)
  - `ModuleStatusController` (`READ`)
- Denied decisions are mapped consistently to `403` with `{code=ACCESS_DENIED, reason=ACCESS_DENIED}`.

## Deny audit logging

Denied decisions emit structured `warn` logs with `event=authorization_deny_audit`.

Logged fields include:
- `timestamp`
- `userId`
- `tenantId`
- `module`
- `resourceType`
- `action`
- `resourceId`
- `decision`
- `denyReason`
- request/correlation reference from MDC (when available)
- `errorType` (for evaluator failures)

## Guardrails

- Invalid registrations fail fast (blank identifiers, empty actions, duplicate module/resource registration).
- Protected integration guardrail validates required registrations on startup and fails startup on missing actions/registrations.
- Current required protected contracts are:
  - `kernel-standard-backend` / `kernel-record` with `READ`, `LIST`, `CREATE`, `UPDATE`, `DELETE`
  - `kernel-module-status` / `module-status` with `READ`

## Scope notes

- This contract is the first-cut application-layer framework for protected standard backend paths.
- Policy DSL, database-native row-level security, and dynamic authorization admin configuration are intentionally out of scope.
