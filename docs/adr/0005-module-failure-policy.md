# ADR-0005: Module Failure Policy

## Status
Accepted

## Context
Validation and error models are defined, but kernel reactions to module failures are not yet standardized. We need configurable and predictable policies for startup/runtime failures, especially initialization failures, while preserving operational resilience.

## Decision
We define three policy actions:
- `FAIL_FAST`
- `REJECT_MODULE_CONTINUE`
- `MARK_MODULE_DEGRADED`

The kernel MUST evaluate failure-policy rules based on error code, lifecycle stage, and module criticality (`mandatory` vs `optional`).

Initialization failure behavior:
- Mandatory module + strict policy mapping may trigger `FAIL_FAST`.
- Otherwise initialization failure leads to module state `failed` and kernel continues.

Canonical reference:
- [`docs/architecture/module-failure-policy/v1.0.0/failure-policy.md`](../architecture/module-failure-policy/v1.0.0/failure-policy.md)

## Consequences
- Operators can choose between resilience and strict correctness.
- Failure handling is deterministic and auditable.
- Policy tuning can be done through configuration without changing module code.

## Alternatives
- Global single behavior only (always fail fast or always continue): rejected due to lack of operational flexibility.
- Ad hoc error handling per module: rejected due to inconsistency and poor maintainability.

## Scope
- Policy action definitions.
- Mapping matrix from failure types to kernel behavior.
- Initialization failure handling rules.
- Logging/observability requirements for policy decisions.

## Out of Scope
- Automated remediation workflows (auto-repair, remote restart orchestration).
- Multi-node consensus on policy outcomes.
