# ADR-0003: Module Lifecycle and State Model

## Status
Accepted

## Context
The module contract is defined (ADR-0002), but kernel behavior across module processing phases is not yet formalized. We need a clear lifecycle model to provide deterministic registry behavior, safe action gating, and consistent error handling.

## Decision
We define a kernel-managed module lifecycle with explicit states:
- `discovered`
- `validated`
- `rejected`
- `registered`
- `initializing`
- `initialized`
- `failed`

The kernel MUST enforce transition rules and allowed actions per state. Invalid transitions are rejected and recorded as lifecycle errors.

The canonical lifecycle model is specified in:
- [`docs/architecture/module-lifecycle/v1.0.0/lifecycle-model.md`](../architecture/module-lifecycle/v1.0.0/lifecycle-model.md)

## Consequences
- Registry behavior becomes deterministic and auditable.
- Error handling can be mapped to state transitions (`rejected`, `failed`).
- Kernel actions are easier to reason about and test through explicit state gating.

## Alternatives
- Implicit lifecycle with internal flags only: rejected because it creates ambiguous behavior and weak observability.
- More granular initial state set in `v1.0.0`: rejected to keep first implementation focused and operationally clear.

## Scope
- Formal state definitions.
- Transition rules and invalid transition policy.
- Action-per-state policy for kernel operations.
- Baseline model for registry and failure handling.

## Out of Scope
- Distributed lifecycle synchronization across multiple kernel nodes.
- Automatic self-healing/retry orchestration policy beyond basic transitions.
- Tenant-specific lifecycle customizations.
