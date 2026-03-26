# Kernel API Classification Model v1.0.0

## 1. Purpose
This document defines how kernel interfaces are classified for module consumption.  
It is a binding standard for Increment 1 and separates public from non-public kernel interfaces.

## 2. Scope
This model applies to all Java packages exposed by the kernel and to all module implementations integrating with the kernel runtime.

## 3. Stability Classes

| Class | Usability | Stability Promise | Breaking-Change Rule |
| --- | --- | --- | --- |
| `declared` | Allowed for all modules in production. | Stable API with backward-compatible evolution inside a major version. | Breaking changes are only allowed with a major version bump of the kernel API and explicit migration notes. |
| `provisional` | Allowed for modules that explicitly accept early-adoption risk. Not recommended for critical production dependencies. | Best-effort stability only; semantics may change based on feedback. | Breaking changes are allowed in minor releases. Any change MUST be documented in release notes. |
| `internal` | Not allowed for module use. Kernel implementation only. | No stability promise. | Breaking changes are allowed at any time without compatibility guarantees. |

## 4. Package / Namespace Convention
Kernel Java packages MUST be classified using these namespace conventions:

- `io.govaryn.kernel.api.*` -> `declared`  
  Example: `io.govaryn.kernel.api.KernelModule`
- `io.govaryn.kernel.api.provisional.*` -> `provisional`
- `io.govaryn.kernel.internal.*` -> `internal`
- Any package outside `io.govaryn.kernel.api.*` is `internal` by default unless explicitly documented otherwise.

## 5. Module Access Policy
- Modules MUST only depend on `declared` APIs for stable integrations.
- Modules MAY depend on `provisional` APIs only with explicit acceptance of compatibility risk.
- Modules MUST NOT use `internal` APIs.

Using `internal` APIs is a contract violation and non-conformant with Govaryn module standards.

## 6. Governance and Evolution Rules
- New module-facing APIs SHOULD start as `provisional` unless stability is proven.
- Promotion from `provisional` to `declared` requires:
  - at least one release cycle of usage feedback,
  - documented behavior and constraints,
  - no unresolved critical compatibility issues.
- Demotion from `declared` to `provisional` is not allowed in `v1.x`.
- Deprecation of `declared` APIs MUST include a migration path and removal timeline.

## 7. Compliance Notes
- The kernel is the gatekeeper for module conformity and MUST enforce the API classification policy in review and validation tooling.
- Module author guidance MUST treat `internal` API usage as forbidden.
