# Module Contract (Author View)

## Purpose

This guide summarizes the module contract from a module-author perspective.

For normative details, use:
- [module-contract/v1.0.0/specification.md](../architecture/module-contract/v1.0.0/specification.md)

## Required Metadata

Every module must define:

- `moduleContractVersion` (must be `1.0.0`)
- `moduleId` (globally unique in one kernel instance)
- `moduleName`
- `moduleVersion` (semantic version)
- `requiredKernelApiVersion` (for example `^1.2.0`)
- `providedCapabilities` (capabilities this module exposes)
- `requiredCapabilities` (mandatory capabilities this module needs)
- `optionalCapabilities` (non-blocking capabilities used when available)
- `failurePolicy` (module-level default failure behavior hints)
- `moduleType`
- `entryPoint`

## Runtime Interface

Runtime entrypoint hooks:

- `initialize(context)`
- `start()`
- `stop()`

The module must expose metadata consistent with the formal contract.

Module dependencies must be expressed as capabilities, not direct module references.

## Authentication Ownership Model

Modules may expose protected HTTP endpoints, but authentication ownership is always kernel-side.

- Modules **must** consume the authenticated principal and authorities provided by the kernel security context.
- Modules **must not** perform independent JWT parsing, signature checks, issuer checks, or audience checks.
- Modules **must not** register their own token validation pipeline (for example custom `JwtDecoder` or introspection flow).

The kernel is the single source of truth for token validation and authentication state.

## Authorization Ownership Model

Authorization decisions for protected capabilities are kernel-owned.

- Modules must delegate decisions to `KernelAuthorizationService`.
- Modules provide authorization input (`subject`, `action`, `resourceType`, optional `resourceId` and `context`).
- Modules must not implement their own primary policy decision engine.

Reference:
- [Module Authorization Contract](../security/MODULE_AUTHORIZATION_CONTRACT.md)

## API Usage Boundary

Modules may use only kernel APIs that are classified as `declared` (or `provisional` with explicit risk acceptance).  
Modules must not use `internal` kernel APIs.

Reference:
- [Kernel API Classification Model v1.0.0](../architecture/kernel-api/v1.0.0/api-classification.md)

## Validation Rules You Must Pass

Validation checks include:

- required field presence
- value/format checks (ID pattern, semantic version, enum constraints)
- duplicate `moduleId` detection
- kernel API compatibility
- contract version support

Reference:
- [validator-rule-catalog.md](../architecture/module-validation/v1.0.0/validator-rule-catalog.md)
- [specification.md](../architecture/module-contract/v1.0.0/specification.md) (includes positive and negative manifest examples)
- [capability-dependency-model.md](../architecture/module-capability/v1.0.0/capability-dependency-model.md)

## Error Model Expectations

Validation and runtime issues are reported via structured error codes and payloads.

Reference:
- [error-model.md](../architecture/module-validation/v1.0.0/error-model.md)
