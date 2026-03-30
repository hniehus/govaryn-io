# Kernel-Only Policy Authorization Integration Note

This note describes the implemented kernel authorization integration and where it is wired in the codebase.

## Implemented components

- Domain model (`io.govaryn.kernel.security.authorization.model`)
  - `AuthorizationRequest`
  - `AuthorizationSubject`
  - `AuthorizationDecision` / `AuthorizationDecisionResult`
  - `PolicySet` / `PolicyRule` / `PolicyEffect`
  - `DecisionReasonCode`
- YAML policy source and validation (`io.govaryn.kernel.security.authorization.policy`)
  - `YamlAuthorizationPolicyParser`
  - `AuthorizationPolicyValidator`
  - `AuthorizationPolicyLifecycleService`
  - `InMemoryActiveAuthorizationPolicyStore`
- Kernel-only PDP
  - `KernelPolicyDecisionPoint`
- Module-facing contract
  - `KernelAuthorizationService`
  - `KernelAuthorizationOperation` / `KernelAuthorizationOperations`
  - `KernelAuthorizationServiceAdapter`
- Decision logging
  - `AuthorizationDecisionLogger`
- Explicit reload hook
  - `AuthorizationPolicyReloadController`
  - endpoint: `POST /api/kernel/internal/authorization/policy/reload`

## Request and decision model

Authorization request input:

- `subject` (required)
- `action` (required)
- `resourceType` (required)
- `resourceId` (optional)
- `context` (optional constrained map, V1 key: `environment`)

Decision semantics:

1. Matching `DENY` rule exists -> final `DENY`
2. Else matching `PERMIT` rule exists -> final `PERMIT`
3. Else -> `DENY` (default deny)
4. Evaluation error -> `DENY` (fail closed)

## Protected execution integration points

- Platform path:
  - `ModuleStatusController` enforces kernel authorization before business logic for `GET /modules/status`.
  - Denied/invalid subject/evaluation failures return `403`.
- Module path reference:
  - `ReferenceFeatureModule#restartProtectedModule(...)` delegates to `KernelAuthorizationService`.

## Policy lifecycle behavior

- Startup (when `govaryn.kernel.authorization.enabled=true`):
  - parse YAML -> validate -> activate snapshot
  - startup fails on invalid policy
- Reload:
  - same parse/validate/activate flow
  - activation occurs only if valid
  - failed reload keeps last known valid policy active
  - revision changes only on successful activation

## Decision logging behavior

- Structured event: `event=authorization_decision`
- Logged fields include result, reason code, matched rule id, action, resource type, policy revision, request reference (if present in MDC), and sanitized context.
- Subject/resource identifiers are hashed references.
- Sensitive context keys/values are redacted.
- Raw tokens/credentials/secrets are not logged.

## Testing coverage summary (current)

- Domain model validation/invariants
- YAML parsing and malformed input rejection
- Semantic policy validation (duplicate ids, invalid context/operators, required fields)
- Active store behavior and atomic snapshot replacement
- PDP decision semantics (permit/deny/conflicts/default deny/fail closed)
- Protected endpoint enforcement (`/modules/status`) including `403` and unauthenticated `401`
- Decision logging field coverage and sanitization
- Reload behavior (successful switch, failed reload retention, revision updates on success only)
