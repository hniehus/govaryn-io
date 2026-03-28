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
