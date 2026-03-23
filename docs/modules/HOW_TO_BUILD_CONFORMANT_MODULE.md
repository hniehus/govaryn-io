# How To Build a Conformant Module

## 1. Implement the Module Interface

Create a class implementing `KernelModule` (or `KernelRuntimeModule`) and provide:

- `metadata()`
- `initialize(context)`
- `start()`
- `stop()`

## 2. Define Valid Metadata

In `metadata()`, ensure:

- stable, unique `moduleId`
- valid semantic `moduleVersion`
- realistic `requiredKernelApiVersion`
- correct `moduleType`

If your module is optional, keep behavior safe when services are unavailable.

## 3. Keep Initialization Deterministic

`initialize(context)` should:

- validate required capabilities
- fail fast with meaningful exception messages when mandatory prerequisites are missing
- avoid side effects that cannot be rolled back

## 4. Understand Startup Outcomes

Your module may be:

- discovered
- validated
- rejected
- registered
- initializing
- initialized
- failed
- degraded

These transitions are kernel-controlled:
- [module-lifecycle/v1.0.0/lifecycle-model.md](../architecture/module-lifecycle/v1.0.0/lifecycle-model.md)

## 5. Test Against Negative Cases

Before publishing, test your module against:

- incompatible API requirement
- duplicate `moduleId`
- initialization failure path

Use:
- [REFERENCE_MODULES.md](./REFERENCE_MODULES.md)

## 6. Verify Structured Diagnostics

Check startup logs for:

- module discovery event
- validation report and issues
- registration and initialization events
- startup summary counters

This is the minimum operability baseline for support and platform teams.
