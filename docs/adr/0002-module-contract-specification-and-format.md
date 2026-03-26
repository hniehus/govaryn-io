# ADR-0002: Module Contract Specification and Format Strategy

## Status
Accepted

## Context
ADR-0001 established that modules must integrate with the kernel through a formal contract. The next step is to define a binding technical contract format with required attributes, optional attributes, naming rules, and versioning rules.

We need a clear decision on the contract representation:
- code-based only,
- manifest-based only,
- or a hybrid approach.

## Decision
We adopt a **hybrid contract model**:
- **Manifest-based metadata contract** is mandatory for every module.
- **Code-based entrypoint contract** is mandatory and referenced by the manifest.

Rationale:
- Manifest metadata enables pre-load validation and tooling.
- Code-based entrypoints preserve type safety and runtime integration clarity.
- Hybrid gives stronger compatibility guarantees than either approach alone.

The first formal contract version is:
- `moduleContractVersion: 1.0.0`

## Consequences
- Kernel can validate metadata before loading module code.
- Module packaging must include a valid manifest and a compatible entrypoint.
- Contract evolution requires explicit versioning and compatibility rules.

## Alternatives
- Code-based only: rejected because metadata would be harder to inspect/validate before load.
- Manifest-based only: rejected because runtime behavior contract would be too implicit.

## Scope
- Define required and optional metadata attributes.
- Define uniqueness and naming conventions.
- Define contract versioning and compatibility checks.
- Provide a machine-readable metadata model (JSON Schema).

## Out of Scope
- Digital signatures and trust-chain validation.
- Remote module registry and distribution protocol.
- Multi-major compatibility policy beyond `1.x` baseline.
