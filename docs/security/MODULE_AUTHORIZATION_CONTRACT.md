# Module Authorization Contract (Kernel-Owned Decisions)

Modules must delegate authorization decisions to the kernel through `KernelAuthorizationService`.

## Required inputs

- `subject` (who is requesting)
- `action` (required)
- `resourceType` (required)

## Optional inputs

- `resourceId` (object-level authorization)
- `context` (constrained key-value attributes)

## Input validation guidance

`KernelAuthorizationOperation` validates:
- `action` format: `^[a-z][a-z0-9._:-]{1,63}$`
- `resourceType` format: `^[a-z][a-z0-9._:-]{1,63}$`
- supported context keys (V1): `environment`

Invalid or missing required input is rejected safely and results in a deny decision when routed via `KernelAuthorizationService`.

## Reference usage

See `ReferenceFeatureModule#restartProtectedModule(...)` for delegation before business execution.
