# 9. Architecture Decisions

Capture significant architecture decisions and their rationale.

## Decision Records

- Link to ADRs in [`docs/adr`](../adr/)
- Include status (`proposed`, `accepted`, `deprecated`, `superseded`)
- Reference decision drivers and consequences
- Current decision: [ADR-0001: Module Definition, Scope, and Formal Module Contract](../adr/0001-modulesystem-scope-and-contract.md)
- Current decision: [ADR-0002: Module Contract Specification and Format Strategy](../adr/0002-module-contract-specification-and-format.md)
- Current decision: [ADR-0003: Module Lifecycle and State Model](../adr/0003-module-lifecycle-and-state-model.md)
- Current decision: [ADR-0004: Module Validation Rules and Error Model](../adr/0004-module-validation-rules-and-error-model.md)
- Current decision: [ADR-0005: Module Failure Policy](../adr/0005-module-failure-policy.md)

## Current Gaps

- The JWT/OIDC security foundation is implemented in `kernel/src/main/java/io/govaryn/kernel/security`, but no dedicated ADR exists yet in `docs/adr`.
- The kernel-only policy authorization implementation is active in `kernel/src/main/java/io/govaryn/kernel/security/authorization`, but no dedicated ADR exists yet in `docs/adr`.
