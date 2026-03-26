# Capability and Dependency Model v1.0.0

## 1. Purpose
This specification defines how modules declare provided and required capabilities and how the kernel resolves dependencies.

The model is binding for Increment 1 and is designed to avoid direct module-to-module coupling.

## 2. Core Principles
- Dependencies are expressed as capability dependencies, not direct module dependencies.
- Modules declare **what they provide** and **what they need**, without naming a concrete provider module.
- The kernel resolves providers at runtime and acts as the authoritative dependency resolver.

## 3. Capability Declaration Model

Every module declares:
- `providedCapabilities` (required): capabilities this module exposes.
- `requiredCapabilities` (required): mandatory capabilities needed for successful initialization.
- `optionalCapabilities` (optional): non-blocking capabilities that enhance behavior when available.

Capability identifier format:
- MUST match `^[a-z][a-z0-9.:-]{2,127}$`
- SHOULD use domain-like names for clarity (for example `event.publish`, `audit.query`).

## 4. Dependency Resolution Semantics
Kernel resolution order:
1. Build capability index from all `providedCapabilities`.
2. For each module, resolve each entry in `requiredCapabilities`.
3. If at least one provider exists for every required capability, module remains eligible for registration/initialization.
4. Missing required capability -> module is rejected.
5. Resolve `optionalCapabilities` best-effort; missing optional capabilities do not block startup.

Provider selection:
- If exactly one provider exists, bind deterministically to that provider.
- If multiple providers exist, apply kernel selection policy (for example priority/configured preference) and record the decision.

## 5. Mandatory vs Optional Rules
- `requiredCapabilities`:
  - MUST be satisfied before initialization.
  - Any unresolved required capability is a blocking validation/resolution error.
- `optionalCapabilities`:
  - MAY be unresolved.
  - Unresolved optional capabilities MUST be recorded as diagnostics but MUST NOT block module startup.
- `requiredCapabilities` and `optionalCapabilities` MUST be disjoint.

## 6. Cycle Rule
Cycles in the resolved mandatory dependency graph are not allowed.

Formal rule:
- Build a directed graph with modules as nodes.
- Add edge `A -> B` when module `A` requires capability `c` and `B` is the selected provider of `c`, for mandatory capabilities only.
- If the graph contains a cycle, involved modules MUST be rejected with a cycle-detected error.

`v1.0.0` does not permit cyclic mandatory dependency chains.

## 7. Capability Matching Example

### 7.1 Valid Matching
`event-bus` module:
- `providedCapabilities`: `["event.publish"]`

`audit-log` module:
- `providedCapabilities`: `["audit.write"]`
- `requiredCapabilities`: `["event.publish"]`
- `optionalCapabilities`: `["metrics.counter"]`

Resolution result:
- `audit-log` mandatory capability `event.publish` is matched to `event-bus` -> valid.
- `metrics.counter` unresolved -> warning only, module still allowed to initialize.

### 7.2 Invalid (Cycle)
`module-a`:
- `requiredCapabilities`: `["cap.b"]`
- `providedCapabilities`: `["cap.a"]`

`module-b`:
- `requiredCapabilities`: `["cap.a"]`
- `providedCapabilities`: `["cap.b"]`

Mandatory graph:
- `module-a -> module-b`
- `module-b -> module-a`

Result:
- cycle detected -> both modules are invalid for initialization in `v1.0.0`.

## 8. Conformance
A kernel implementation is conformant with this model if it:
1. resolves dependencies only through capabilities,
2. enforces mandatory vs optional semantics,
3. rejects unresolved mandatory capabilities,
4. rejects cyclic mandatory dependency graphs.
