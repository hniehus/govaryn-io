# Module Governance and Change Rules

This document defines mandatory governance rules for Govaryn modules and kernel API evolution.
It applies to both kernel and module teams.

## Scope

- Rules for new kernel APIs and API lifecycle changes.
- Module minimum requirements and release gating.
- API stability classes and allowed usage.

## API Stability Classes

See `docs/architecture/kernel-api/v1.0.0/api-classification.md` for definitions.

Rules:

- `declared`: stable, public APIs. Modules may depend on these.
- `provisional`: usable with caution. Module owners must accept change risk.
- `internal`: not allowed for module usage. Usage fails CI checks.

Release approval:

- `declared`: requires kernel architecture review and test coverage for stability guarantees.
- `provisional`: requires owner sign-off and explicit warning in docs.
- `internal`: never approved for module consumption.

## Introducing New Kernel APIs

1. APIs must be classified as `declared` or `provisional`.
2. Public APIs must be documented with purpose, inputs, outputs, and error behavior.
3. For `declared` APIs, add tests that lock behavior and avoid regressions.
4. Any new API requires an update to the module contract or API classification docs
   if it impacts module-facing behavior.

## Deprecation Rules

- Deprecate before removal. Provide replacement guidance.
- Deprecation period:
  - `declared`: minimum two minor releases.
  - `provisional`: minimum one minor release.
- Deprecations must be announced in release notes and in docs.

## Breaking Change Rules

- Breaking changes are allowed only in a major kernel API version.
- The kernel must reject modules that declare incompatible `requiredKernelApiVersion`.
- Breaking changes require:
  - Migration guidance in docs.
  - Updates to module examples and reference module where applicable.

## Module Minimum Requirements

- Conform to the current module contract.
- Provide unique `moduleId` and valid semantic `moduleVersion`.
- Declare capabilities and any required dependencies.
- Use only `declared` or `provisional` kernel APIs.
- Provide failure policy and meaningful error handling.

## CI Gates for Modules

Modules are accepted only when all gates pass:

- Contract validation (metadata schema and semantic rules).
- Kernel API compatibility check.
- Architecture rule checks (no internal API usage, no forbidden cycles).
- Integration tests for discovery, validation, registration, initialization.

## Enforcement

- Violations of `internal` API usage fail CI.
- Duplicate `moduleId` or incompatible kernel API versions fail startup.
- Unresolved or cyclic capability dependencies fail startup.
