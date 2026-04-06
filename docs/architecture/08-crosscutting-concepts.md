# 8. Crosscutting Concepts

Document concepts relevant across multiple parts of the system.

## Concept Areas

- Domain model
- Security
- Error handling
- Logging and monitoring
- Configuration management
- Test strategy
- Build and release

## Security (Current Implementation Scope)

- Security can be disabled or enabled via `govaryn.kernel.security.enabled`.
- When enabled, kernel startup requires valid `issuer-uri` and `audience`.
- The kernel is configured as a JWT resource server using one configured OIDC issuer.
- Token validation covers issuer, signature, expiration/not-before, and audience.
- Public paths are explicitly configured (`govaryn.kernel.security.public-paths`); non-public paths require authentication.
- Authentication failures return `401` and are logged with sanitized categories (for example issuer mismatch, audience failure, provider/key retrieval failure).
- JWT identity mapping is standardized by the kernel: subject, issuer, username fallback (`preferred_username` -> `username` -> `sub`), and authorities from configured claim/prefix.
- Module code is expected to consume kernel-provided authentication context rather than validating tokens independently.

## Security and Tenant Context (Implemented Behavior)

- The kernel establishes one `KernelSecurityTenantContext` per authenticated request and stores it for request-scoped reuse.
- Token claims are authoritative for tenant scope (`tenant_scope`, `tenantScope`, `tenant_ids`, `tenantIds`, `tenants`, or single-tenant fallbacks `tenant_id`, `tenantId`, `tid`).
- Route data is the only explicit tenant selector in this story (`tenantId`, `tenant_id`, `tid` URI template variables).
- Active tenant resolution rules:
  - explicit route tenant -> validate against token scope before activation
  - no explicit route tenant + exactly one permitted tenant -> that tenant becomes active
  - tenant-protected operation + multi-tenant scope + no explicit route tenant -> deny (`403`)
  - tenant-protected operation + empty scope + no explicit route tenant -> deny (`403`)
- Privileged cross-tenant access is explicit and minimal:
  - requires explicit route tenant
  - requires explicit privileged authority (`tenant_cross_access` suffix, for example `ROLE_tenant_cross_access`)
  - still fails closed when token tenant scope is empty
- Failure semantics:
  - missing/invalid authentication -> `401 Unauthorized`
  - authenticated request with missing/invalid/unauthorized tenant context -> `403 Forbidden`
- Modules/services must read tenant-aware request context from `KernelCurrentSecurityContext` and must not reconstruct tenant selection from raw token claims, headers, or routes.

## Authorization (Kernel Framework + Module Rules)

- Authorization for protected backend paths is split into:
  - kernel-owned framework + enforcement
  - module-owned resource/action/evaluator rules
- Kernel framework responsibilities:
  - build normalized `SecurityContext` from authenticated request context
  - maintain authorization model (`AuthorizationRequest`, `AuthorizationDecision`, `AuthorizationAction`, `DenyReason`)
  - register and resolve module policy contributions (`ModuleSecurityContributor`, `ResourcePolicyRegistry`)
  - orchestrate decisions (`KernelAuthorizationService`)
  - enforce before business access on kernel-managed paths (`KernelAuthorizationEnforcer`)
  - return consistent deny behavior (`KernelAccessDeniedException` -> `403 ACCESS_DENIED`)
  - emit structured deny audit logs (`AuthorizationAuditLogger`)
  - fail startup for missing required protected integration (`KernelProtectedAuthorizationIntegrationGuardrail`)
- Module responsibilities:
  - declare protected resource types
  - declare supported actions
  - implement domain evaluator logic (for example tenant/scope checks)
- Decision behavior is fail-closed:
  - missing authenticated context -> deny
  - missing registration / unsupported action -> deny
  - evaluator error -> deny
- First-cut actions are standardized (`READ`, `LIST`, `CREATE`, `UPDATE`, `DELETE`).
- First-cut excludes policy DSL, database-native row-level security, and dynamic admin authorization configuration.

## Logging and Diagnostics (Authorization)

- Denied authorization attempts are logged as structured events (`event=authorization_deny_audit`).
- Deny audit fields include at least: `timestamp`, `userId`, `tenantId`, `module`, `resourceType`, `action`, `resourceId`, `decision`, `denyReason`, `requestRef`, `errorType`.
- Security-context mapping/preload failures are logged as structured warnings (`event=security_context_mapping_failed`, `event=security_context_preload_failed`).
- Raw tokens, credentials, and sensitive payload data must not be logged.
- Legacy policy-decision logging (`event=authorization_decision`) remains for operation-level policy flows.

## Module Versioning and Contract

- All modules must declare `moduleContractVersion`, `moduleVersion`, and
  `requiredKernelApiVersion`.
- The kernel validates compatibility and rejects incompatible modules early.
- Contract rules and error model are normative and versioned:
  `docs/architecture/module-contract/v1.0.0/specification.md`
  and `docs/architecture/module-validation/v1.0.0/error-model.md`.

## Failure Policy and Resilience

- Failure policy is enforced per module with kernel fallback where applicable.
- Initialization and runtime failures are logged with module context.
- Policies: `FAIL_FAST`, `REJECT_MODULE_CONTINUE`, `MARK_MODULE_DEGRADED`.

## Observability

- Every lifecycle phase emits structured events for module diagnosis.
- Startup summary counters provide a coarse health signal.
- Status can be queried via the module status service.

## Module Governance and Change Control

- API classification (declared/provisional/internal) constrains module usage.
- Deprecation and breaking change rules are documented in:
  `docs/modules/MODULE_GOVERNANCE.md`.
