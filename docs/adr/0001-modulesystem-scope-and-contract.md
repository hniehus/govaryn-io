# ADR-0001: Module System Scope and Kernel-Module Responsibility Boundaries

## Status
Accepted

## Context
For the Govaryn module system, the first increment requires a binding architectural baseline: what a module is, what the kernel owns, what modules own, and what is explicitly excluded from Increment 1. Without this boundary, implementations diverge, integration behavior becomes inconsistent, and lifecycle orchestration is hard to verify.

## Decision
### 1) Goal of the Module System
The goal of the module system is to enable runtime-composable capabilities with a stable integration contract, so modules can be developed and evolved independently while the kernel preserves platform consistency, safety checks, and deterministic startup behavior.

### 2) Module Definition
A module is a runtime-loadable, logically encapsulated functional unit that:
- provides a clearly defined capability/function,
- interacts with the kernel exclusively through the formal module contract,
- has no direct access to internal kernel implementation details.

The following are not modules:
- internal kernel components,
- pure build/deploy artifacts without a runtime contract,
- technical helper classes without an independent module lifecycle.

### 3) Responsibility Boundaries: Kernel vs. Module
The kernel is responsible for:
- acting as the **gatekeeper** for module admission into the runtime,
- acting as the **orchestrator** of module lifecycle progression,
- module discovery and intake,
- contract validation (compatibility and required metadata),
- module registration into the runtime registry,
- module initialization ordering and execution,
- providing stable kernel services through defined interfaces,
- system-level fault boundaries and observability (logging/metrics).

The module is responsible for:
- business/technical capability within its own scope,
- compliance with the formal module contract,
- local error handling within the module,
- version maintenance of its own contract-facing declarations (for example declared dependencies).

### 4) Binding Lifecycle Phases (Increment 1)
The first increment defines the following phases as mandatory and authoritative:
- **Discovery**
- **Validation**
- **Registration**
- **Initialization**

Each phase is kernel-controlled. A module must not bypass or reorder these phases.

### 5) Formal Module Contract (Mandatory Scope)
The following are part of the formal module contract:
- module identity: unique ID, name, version,
- contract API version (compatibility checks),
- declared capabilities,
- declared dependencies (other modules or kernel services),
- lifecycle hooks (for example `initialize`, `start`, `stop`),
- configuration schema and required configuration keys,
- error and status signals towards the kernel.

The following are not part of the formal contract:
- internal class structure of the module,
- concrete implementation details of the capability,
- optional internal caches or optimizations.

### 6) In Scope (Increment 1)
- binding definition of the module concept,
- binding kernel/module responsibility boundaries,
- kernel-controlled lifecycle phases: Discovery, Validation, Registration, Initialization,
- contract definition as technical reference (required fields, lifecycle hooks),
- kernel-side validation of identity, version, and required metadata,
- baseline startup orchestration for module onboarding.

### 7) Out of Scope (Increment 1)
- advanced sandbox/security isolation for untrusted modules,
- hot reload beyond controlled restart boundaries,
- distributed module registration across process boundaries,
- marketplace/signature and trust-chain mechanisms,
- cross-major-version compatibility guarantees.

## Consequences
- Clear integration boundaries reduce coupling between kernel and modules.
- The gatekeeper/orchestrator role centralizes policy enforcement and startup determinism.
- Contract validation increases short-term effort but reduces long-term integration errors.

## Alternatives
- No formal contract, conventions only: rejected due to high inconsistency and integration risk.
- Very broad contract with many optional fields: rejected because it adds unnecessary complexity for the initial phase.
