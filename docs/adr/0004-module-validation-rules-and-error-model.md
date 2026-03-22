# ADR-0004: Module Validation Rules and Error Model

## Status
Accepted

## Context
The module contract (`v1.0.0`) and lifecycle model are defined, but validation outcomes and error semantics are not yet standardized. We need deterministic rules to classify modules as valid/invalid and a uniform error structure for diagnostics, registry behavior, and operational decisions.

## Decision
We define:
- a versioned validator rule catalog,
- a unified module error/exception model,
- and a startup policy for invalid modules.

Startup policy decision:
- **Default mode**: reject invalid modules and continue kernel startup.
- **Strict mode (optional configuration)**: fail startup if at least one mandatory module is rejected.

Canonical documents:
- [`validator-rule-catalog.md`](../architecture/module-validation/v1.0.0/validator-rule-catalog.md)
- [`error-model.md`](../architecture/module-validation/v1.0.0/error-model.md)

## Consequences
- Validation behavior is consistent across environments.
- Error reporting becomes machine-readable and suitable for observability.
- Kernel startup remains resilient by default, while strict environments can enforce fail-fast behavior.

## Alternatives
- Always fail startup on first invalid module: rejected because it reduces platform resilience for optional modules.
- Always continue startup with no strict option: rejected because some environments require hard guarantees.

## Scope
- Missing required fields, invalid values, duplicate `moduleId`, and incompatible kernel API checks.
- Structured validation error taxonomy and payload.
- Baseline exception model for module-processing stages.

## Out of Scope
- Cross-node distributed validation consensus.
- Signature trust-chain validation policy.
- UI representation of validation and error details.
