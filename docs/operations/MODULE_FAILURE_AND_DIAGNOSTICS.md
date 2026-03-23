# Module Failure, Policy, and Diagnostics Runbook

## Scope

This runbook explains module failure handling, policy behavior, and diagnostics interpretation.

## Failure Policies

Supported actions:

- `FAIL_FAST`
- `REJECT_MODULE_CONTINUE`
- `MARK_MODULE_DEGRADED`

Policy configuration controls kernel reaction to initialization/runtime failures.

Reference:
- [failure-policy.md](../architecture/module-failure-policy/v1.0.0/failure-policy.md)

## Typical Failure Cases

- `KERNEL_API_INCOMPATIBLE`: module rejected during validation
- `DUPLICATE_MODULE_ID`: startup aborted before registration
- `INITIALIZATION_FAILED`: policy-driven response during initialization
- `STATE_TRANSITION_INVALID`: lifecycle transition blocked

Kernel-stop vs module-only:
- `FAIL_FAST`: kernel stops
- `REJECT_MODULE_CONTINUE`: only affected module is rejected/failed
- `MARK_MODULE_DEGRADED`: only affected module transitions to `degraded`

## Structured Log Keys

Core keys for diagnosis:

- `moduleId`
- `moduleVersion`
- `requiredKernelApiVersion`
- `currentModuleStatus`
- `errorType`
- `errorCause`

## Event Names to Monitor

- `module_validation_issue`
- `module_initialization_failed`
- `module_transition_failed`
- `module_mark_degraded_failed`
- `module_startup_summary`

## Operator Playbook

1. Read `module_startup_summary`.
2. If `rejected > 0`, inspect `module_validation_issue` events first.
3. If `failed > 0`, inspect `module_initialization_failed` with `policy` and `errorCause`.
4. For duplicate IDs, resolve `moduleId` ownership conflict before restart.
5. Document recurring `errorType` patterns and feed back to module owners.

## Support Handoff Template

Capture:

- startup timestamp/environment
- affected `moduleId` and `moduleVersion`
- `requiredKernelApiVersion` vs running kernel version
- `currentModuleStatus`
- `errorType` / `errorCause`
- active initialization failure policy
