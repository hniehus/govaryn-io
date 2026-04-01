# ADR-0006: Kernel Authorization Framework and Kernel-Controlled Enforcement

## Status
Accepted

## Context
The kernel already provides centralized authentication, but authorization behavior across modules was not standardized for protected backend paths. Without a shared contract, modules can drift into inconsistent rule modeling, inconsistent deny handling, and bypassable enforcement paths.

We need a first-cut authorization model that is:
- explicit,
- auditable,
- non-bypassable on standard kernel-managed paths,
- small enough for incremental adoption.

## Decision
We split authorization responsibilities into:

- **Kernel-owned framework and enforcement pipeline**
- **Module-owned resource rules**

### Kernel owns
- Security context normalization from authenticated request state (`SecurityContext`).
- Authorization model primitives (`AuthorizationRequest`, `AuthorizationDecision`, `AuthorizationAction`, `DenyReason`).
- Module policy registration infrastructure (`ModuleSecurityContributionRegistrar`, `ResourcePolicyRegistry`).
- Central orchestration (`KernelAuthorizationService`).
- Standard enforcement hook (`KernelAuthorizationEnforcer`) used by kernel-managed backend paths.
- Consistent deny exception/response mapping (`KernelAccessDeniedException`, `403 ACCESS_DENIED` response model).
- Structured deny audit logging (`AuthorizationAuditLogger`).
- Guardrails for required protected integrations (`KernelProtectedAuthorizationIntegrationGuardrail`).

### Modules own
- Protected resource types.
- Supported actions per resource.
- Domain-specific evaluator logic (scope/tenant/record checks).

Modules provide rules through `ModuleSecurityContributor`; modules do **not** own an independent enforcement pipeline for protected standard paths.

## Consequences
- Authorization behavior is consistent across protected kernel-managed paths.
- Deny handling and deny logs are standardized and auditable.
- Module integration stays simple: register resource/action/evaluator, let kernel enforce.
- First cut is intentionally limited; advanced policy features are deferred.

## Alternatives
- Per-module ad hoc authorization filters/interceptors: rejected due to bypass and inconsistency risk.
- Full policy DSL in first cut: rejected as overengineering for current scope.
- Database-native row-level security as primary control: rejected for first-cut portability and rollout complexity.

## Scope
- Standardized module security SPI.
- Central kernel authorization orchestration.
- Kernel-controlled enforcement on standard backend paths.
- Consistent deny handling and structured deny logging.
- Startup guardrails for protected integration contracts.

## Out of Scope
- Generic policy DSL/expression language.
- Database-native row-level security.
- Dynamic admin UI for authorization configuration.
- Exhaustive static detection of every possible custom bypass path in the codebase.
