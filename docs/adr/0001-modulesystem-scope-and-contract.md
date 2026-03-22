# ADR-0001: Module Definition, Responsibility Boundaries, and Formal Module Contract

## Status
Accepted

## Context
For the planned module system, we currently lack a binding definition of what a module is within the kernel, which responsibilities belong to the kernel, and which belong to the module. Without this boundary, implementations become inconsistent, integration points diverge, and behavior is harder to test.

## Decision
### 1) Module Definition
A module is a runtime-loadable, logically encapsulated functional unit that:
- provides a clearly defined capability/function,
- interacts with the kernel exclusively through the formal module contract,
- has no direct access to internal kernel implementation details.

The following are not modules:
- internal kernel components,
- pure build/deploy artifacts without a runtime contract,
- technical helper classes without an independent module lifecycle.

### 2) Responsibility Boundaries: Kernel vs. Module
The kernel is responsible for:
- module lifecycle (discovery, loading, initialization, stop/unload),
- module contract validation (compatibility, required metadata),
- providing stable kernel services through defined interfaces,
- isolation, fault boundaries, and observability (system-level logging/metrics).

The module is responsible for:
- business/technical capability within its own scope,
- compliance with the formal module contract,
- local error handling within the module,
- version maintenance of its own contract-facing declarations (for example declared dependencies).

### 3) Formal Module Contract (Mandatory Scope)
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

### 4) Implementation Boundary (Now vs. Later)
Implemented now:
- contract definition as technical reference (required fields, lifecycle hooks),
- kernel-side validation of identity, version, and required metadata,
- base lifecycle in the kernel,
- documented scope/out-of-scope rules.

Implemented later:
- advanced sandbox/security isolation,
- hot reload beyond restart boundaries,
- distributed module registration across process boundaries,
- marketplace/signature and trust-chain mechanisms.

## Consequences
- Clear integration boundaries reduce coupling between kernel and modules.
- The kernel remains evolvable as long as the formal contract is versioned in a stable manner.
- Contract validation increases short-term effort but reduces long-term integration errors.

## Alternatives
- No formal contract, conventions only: rejected due to high inconsistency and integration risk.
- Very broad contract with many optional fields: rejected because it adds unnecessary complexity for the initial phase.

## Scope
- Definition and documentation of the module concept.
- Binding responsibility boundaries between kernel and module.
- Definition of the formal module contract elements.
- Minimal kernel lifecycle and baseline validation for modules.

## Out of Scope
- Full security sandbox for untrusted modules.
- Dynamic distributed module orchestration across multiple nodes.
- Module marketplace, signing, certificate, and trust-chain features.
- Full backward compatibility across multiple major contract versions.
