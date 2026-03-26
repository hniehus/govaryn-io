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

- Validation errors:
  - `REQUIRED_FIELD_MISSING`
  - `INVALID_FIELD_VALUE`
  - `CAPABILITY_DECLARATION_INVALID`
- `KERNEL_API_INCOMPATIBLE`: module rejected during validation
- `DUPLICATE_MODULE_ID`: startup aborted before registration
- `INITIALIZATION_FAILED`: policy-driven response during initialization
- `START_FAILED`: failure during `start()` execution
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
- `policy`
- `errorType`
- `errorCause`

## Event Names to Monitor

- `module_validation_issue`
- `module_initialization_failed`
- `module_transition_failed`
- `module_mark_degraded_failed`
- `module_start_failed`
- `module_startup_summary`

## Operator Playbook

1. Read `module_startup_summary`.
2. If `rejected > 0`, inspect `module_validation_issue` events first.
3. If `failed > 0`, inspect `module_initialization_failed` or `module_start_failed` with `policy` and `errorCause`.
4. For `KERNEL_API_INCOMPATIBLE`, compare `requiredKernelApiVersion` to kernel version.
5. For `DUPLICATE_MODULE_ID`, resolve module ownership conflict before restart.
6. Document recurring `errorType` patterns and feed back to module owners.

## Troubleshooting Steps

1. Identify the failing `moduleId` and `moduleVersion` from logs.
2. Check `module_validation_issue` for contract errors or invalid values.
3. Verify `requiredKernelApiVersion` against the running kernel version.
4. Confirm no duplicate `moduleId` exists in discovery sources.
5. Review failure policy and whether the module should degrade or fail fast.
6. Re-run with increased log level if the root cause is unclear.

## Support Handoff Template

Capture:

- startup timestamp/environment
- affected `moduleId` and `moduleVersion`
- `requiredKernelApiVersion` vs running kernel version
- `currentModuleStatus`
- `errorType` / `errorCause`
- active initialization failure policy
