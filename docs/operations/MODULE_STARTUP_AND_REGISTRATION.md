# Module Startup and Registration Runbook

## Scope

This runbook describes how module startup proceeds and how registration decisions are made.

## Startup Pipeline

1. Discover module candidates (`classpath`, `manifest scan`, plugin source)
2. Detect identity collisions (`same moduleId`)
3. Validate module contract
4. Register valid modules
5. Initialize registered/loadable modules
6. Start initialized modules
7. Emit startup summary counters

## Registration Rules

- Only valid modules are registered.
- Invalid modules are rejected and excluded from registry.
- Registered modules are retrievable by `moduleId`.
- Registration transitions module state to `REGISTERED`.

## Startup Summary Counters

The kernel emits:

- `found`
- `validated`
- `rejected`
- `registered`
- `failed`
- `degraded` (when degradation policy is applied during startup/runtime checks)

Use these counters as first-level health indicators during deployments.

## Required Operator Checks

After startup:

1. Confirm `module_startup_summary` is present.
2. Verify `rejected` and `failed` counts are expected.
3. Investigate every `module_validation_issue` and `module_initialization_failed` event.

## Escalation Guidance

- `rejected > 0` in production: investigate contract/version mismatch before promoting.
- `failed > 0` for mandatory modules: treat as deployment-blocking unless policy explicitly allows degraded service.
