# 4. Solution Strategy

Summarize the fundamental strategic decisions and architecture approaches.

## Strategy Items

- Technology and platform choices
- Decomposition strategy
- Integration strategy
- Security and compliance strategy
- Scalability and availability strategy

## Current Strategy Notes

- Authentication is kernel-owned and implemented as a JWT resource-server foundation (single configured OIDC-compatible issuer).
- Authorization for protected backend paths is split:
  - kernel owns the authorization framework (`SecurityContext`, request/decision model, registry, orchestration, deny handling, audit logging, guardrails)
  - modules own resource/action/evaluator rules through `ModuleSecurityContributor`
  - enforcement is kernel-controlled through `KernelAuthorizationEnforcer` on standard paths
- First cut is intentionally small: no policy DSL, no database-native row-level security, no dynamic authorization admin UI.
- Legacy YAML policy authorization for operation-level checks remains available in `io.govaryn.kernel.security.authorization` and is outside this first-cut framework scope.
