# ADR-0007: Consistent Security and Tenant Context Establishment and Enforcement

## Status
Accepted

## Context
The kernel already validates JWT authentication and provides authorization enforcement, but tenant context establishment needed explicit, deterministic rules across request handling, module code, and tenant-protected persistence access.

Without one kernel-managed model, modules/services risk:
- inferring tenant context inconsistently,
- introducing bypass paths for tenant boundaries,
- mixing authentication failures and tenant-authorization failures.

## Decision
The kernel establishes a single request-scoped `KernelSecurityTenantContext` for authenticated requests and exposes it through `KernelCurrentSecurityContext`.

### Canonical model
- `KernelSecurityIdentity`: authenticated principal (`subject`, optional issuer/username, authorities).
- `KernelTenantScope`: token-derived permitted tenants.
- `KernelActiveTenantContext`: active tenant for current request.
- `KernelSecurityTenantContext`: combined principal + tenant scope + active tenant (+ metadata).

### Binding rules
- Token is authoritative for tenant scope.
- Route is the only explicit tenant selector in this story.
- If token scope has exactly one tenant and no explicit route tenant is given, that tenant becomes active.
- If token scope has multiple tenants and operation is tenant-protected, explicit route tenant selection is required.
- Explicit route tenant must be validated against token-granted scope before becoming active.
- If explicit route tenant is outside granted scope, request is denied.
- Minimal privileged cross-tenant access is allowed only with:
  - explicit privileged authority (`tenant_cross_access` suffix, for example `ROLE_tenant_cross_access`), and
  - explicit target tenant in route.
- Privileged cross-tenant access does not bypass empty token scope.

### Failure semantics
- Missing/invalid authentication -> `401 Unauthorized`.
- Authenticated request with missing, invalid, or unauthorized tenant context on tenant-protected operation -> `403 Forbidden`.
- Tenant resolution is fail-closed.

### Module/service consumption
- Modules/services read current context via `KernelCurrentSecurityContext` (or `KernelContext.currentSecurityContext()`).
- Modules/services must not parse raw tokens, headers, or routes to re-implement tenant resolution.

## Consequences
- Tenant context is deterministic, reviewable, and reusable across request flow and business logic.
- `401` vs `403` behavior is explicit and test-covered.
- Default behavior for tenant-protected access is deny-by-default.
- Cross-tenant access remains narrow and explicit.

## Alternatives
- Header-based tenant selection: rejected for this story scope.
- Module-local tenant inference/parsing: rejected due to inconsistency and bypass risk.
- Broad policy engine for cross-tenant authorization: rejected as unnecessary for the current scope.
