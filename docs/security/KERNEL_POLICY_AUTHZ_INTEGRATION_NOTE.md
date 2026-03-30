# Kernel-Only Policy Authorization Integration Note

## Current integration points (from repository scan)

- Authenticated subject already exists at HTTP boundary:
  - `KernelHttpSecurityConfiguration` enforces authenticated vs public routes.
  - `KernelSecurityIdentityResolver` maps `Authentication` -> `KernelSecurityIdentity`.
  - `KernelWhoAmIController` shows current normalized subject model.
- Kernel-level services and orchestration live in:
  - `io.govaryn.kernel.internal` (`ModuleOrchestrator`, `ModuleInitializationExecutor`)
  - `io.govaryn.kernel.health` (service/controller pattern)
  - `io.govaryn.kernel.config` (startup validation and config abstractions)
- External configuration loading pattern:
  - Spring config import (`config/application.properties`, optional additional locations)
  - `@ConfigurationProperties` for typed kernel config (`GovarynKernelProperties`, `GovarynKernelSecurityProperties`)
  - `ConfigurationManager` merges defaults + external + env and validates via `ConfigurationValidator`
- Protected operations currently execute in controllers behind `anyRequest().authenticated()` (except configured public paths).
- Structured diagnostics/logging already implemented with stable patterns:
  - auth failures: `KernelAuthenticationFailureEntryPoint` (sanitized categories)
  - lifecycle events: `ModuleOrchestrator`/`ModuleInitializationExecutor` (`event=...` style)

## Proposed minimal package locations

- `io.govaryn.kernel.security.authorization.model`
  - `AuthorizationPolicySet`, `AuthorizationRule`, `AuthorizationEffect`, `AuthorizationRequest`, `AuthorizationDecision`
- `io.govaryn.kernel.security.authorization.config`
  - `GovarynKernelAuthorizationProperties` (`@ConfigurationProperties(prefix = "govaryn.kernel.authorization")`)
- `io.govaryn.kernel.security.authorization.source`
  - `AuthorizationPolicySource` (interface), `YamlAuthorizationPolicySource`
- `io.govaryn.kernel.security.authorization.validation`
  - `AuthorizationPolicyValidator`
- `io.govaryn.kernel.security.authorization.store`
  - `ActiveAuthorizationPolicyStore` (interface), `InMemoryActiveAuthorizationPolicyStore`
- `io.govaryn.kernel.security.authorization.pdp`
  - `KernelPolicyDecisionPoint`
- `io.govaryn.kernel.security.authorization.logging`
  - `AuthorizationDecisionLogger`
- `io.govaryn.kernel.api`
  - `KernelAuthorizationService` (module contract)

## Affected existing components (minimal change)

- `KernelContext`
  - Add optional `KernelAuthorizationService` reference so modules consume kernel authorization contract instead of implementing token/policy logic.
- `KernelSecurityIdentityResolver`
  - Reuse as the canonical subject source for PDP requests.
- `KernelHttpSecurityConfiguration`
  - Keep existing authn setup; no provider/model change needed.
- `KernelWhoAmIController` and `ModuleStatusController`
  - First consumers for explicit PDP checks (smallest controllable boundary).
- `GovarynKernelSecurityProperties` pattern
  - Mirror style for authorization properties (`enabled`, `policy-path`, reload settings).
- `ConfigurationValidator` / startup fail-fast pattern
  - Reuse behavior: invalid policy should fail startup when authorization is enabled.

## Building blocks: minimal implementation approach

- Authorization domain model:
  - Small immutable records with explicit fields; default decision is deny.
  - Rule matching initially on: subject authorities, operation id, resource pattern.
- YAML policy source:
  - Load one kernel-owned YAML policy file from configured local path.
  - No external backend, no multi-provider support.
- Policy validator:
  - Validate schema + semantic checks (unique rule ids, valid effects, non-empty match criteria).
  - Fail fast with key/rule-path specific messages.
- Active policy store:
  - In-memory atomic snapshot (`AtomicReference<AuthorizationPolicySet>`).
- PDP:
  - Deterministic rule evaluation order.
  - Return decision object (`ALLOW`/`DENY`, ruleId, reason).
  - Deny by default if no rule matches or policy unavailable.
- Module authorization contract:
  - `KernelAuthorizationService` in kernel API for module consumption.
  - Modules pass operation/resource + current kernel subject; module code does not parse tokens.
- Decision logging:
  - Structured `event=authorization_decision` logs with sanitized fields (`subject`, `operation`, `resource`, `decision`, `ruleId`).
  - Never log raw token or sensitive claim payloads.
- Policy reload hook:
  - Start with explicit kernel hook method `reload()` on a coordinator service.
  - Optional first trigger: protected kernel admin endpoint (later if needed), reusing current HTTP security.

## Recommended implementation sequence

1. Add authorization properties + domain model + YAML source + validator.
2. Add active store + startup loader (load and validate once; fail closed).
3. Add PDP + decision logger and unit tests.
4. Add `KernelAuthorizationService` contract and wire into `KernelContext`.
5. Add first guarded operation checks in kernel controllers (`/modules/status` first).
6. Add reload hook service and tests; optionally expose via protected endpoint.
7. Update docs/config examples with minimal new keys and sample policy file.

## Testing alignment with repository conventions

- Unit tests (JUnit 5) for model/validator/PDP/store.
- Spring tests with `@SpringBootTest` + `MockMvc` for endpoint allow/deny behavior.
- Log assertions using `OutputCaptureExtension` or logback `ListAppender` (existing pattern in security tests).
- Dynamic config tests with `@DynamicPropertySource` for policy path and enablement flags.

## Open technical assumptions

- Policy decisions are kernel-owned and request-scoped for HTTP operations.
- Initial subject attributes for policy matching are limited to normalized authorities + subject/username from `KernelSecurityIdentity`.
- Current scope does not require per-tenant, per-module persisted policy state, or distributed policy sync.
- Reload semantics are local-process only (single node), atomic snapshot replace.

## Endpoints and how to test (current + planned touchpoints)

- Current public endpoint: `GET /health`
  - Test without token: `200`.
- Current protected endpoint: `GET /api/kernel/whoami`
  - Test without token: `401`.
  - Test with valid JWT: `200` and identity payload.
- First planned policy-enforced endpoint: `GET /modules/status`
  - Test with authenticated subject that has matching policy rule: `200`.
  - Test with authenticated subject without matching rule: `403`.
  - Test with missing/invalid policy when authz enabled: startup failure or deny-by-default (as configured).
