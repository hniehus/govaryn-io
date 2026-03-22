# Module Failure Policy v1.0.0

## 1. Purpose
This document defines configurable kernel behavior when module failures occur during validation, registration, initialization, runtime, and state transitions.

## 2. Policy Actions

| Policy | Meaning |
| --- | --- |
| `FAIL_FAST` | Abort kernel startup (or trigger controlled shutdown in runtime mode if configured). |
| `REJECT_MODULE_CONTINUE` | Reject/fail the module and continue kernel operation. |
| `MARK_MODULE_DEGRADED` | Keep kernel running, mark module degraded for routing/health semantics. |

`MARK_MODULE_DEGRADED` in `v1.0.0` is an operational flag (`degraded=true`) and does not introduce a new lifecycle state.

## 3. Inputs for Policy Resolution
Policy engine inputs:
- `errorCode` (from module error model),
- `stage` (`validation`, `registration`, `initialization`, `runtime`, `state-transition`),
- `moduleCriticality` (`mandatory`, `optional`),
- `kernelMode` (`startup`, `runtime`),
- configured rules with priority order.

## 4. Default Policy Rules (Configurable)
Evaluation is first-match-wins.

| Priority | Match | Action |
| --- | --- | --- |
| 100 | `kernelMode=startup AND moduleCriticality=mandatory AND stage=initialization` | `FAIL_FAST` |
| 90 | `kernelMode=startup AND moduleCriticality=mandatory AND errorCode in {CONTRACT_VERSION_UNSUPPORTED, KERNEL_API_INCOMPATIBLE}` | `FAIL_FAST` |
| 80 | `stage=validation` | `REJECT_MODULE_CONTINUE` |
| 70 | `stage=registration` | `REJECT_MODULE_CONTINUE` |
| 60 | `stage=initialization AND moduleCriticality=optional` | `REJECT_MODULE_CONTINUE` |
| 50 | `stage=runtime AND errorCode=RUNTIME_FAILURE` | `MARK_MODULE_DEGRADED` |
| 10 | `*` (fallback) | `REJECT_MODULE_CONTINUE` |

### 4.1 Precedence and Compatibility
- The failure policy rule engine is the authoritative source for kernel behavior decisions.
- `module.validation.startupPolicy` is treated as a compatibility alias and translated into effective failure-policy rules:
  - `continue-on-reject`: keep default validation behavior (`REJECT_MODULE_CONTINUE`).
  - `fail-on-reject`: add an effective high-priority rule for startup validation failures of mandatory modules -> `FAIL_FAST`.
- If both are configured and conflict, explicit `module.failurePolicy.rules` take precedence.

## 5. Decision Matrix: Failure -> Kernel Behavior

| Failure Type / Error Code | Typical Stage | Mandatory Module | Optional Module |
| --- | --- | --- | --- |
| `REQUIRED_FIELD_MISSING` | validation | `FAIL_FAST` (strict startup) or `REJECT_MODULE_CONTINUE` | `REJECT_MODULE_CONTINUE` |
| `INVALID_FIELD_VALUE` | validation | `FAIL_FAST` (strict startup) or `REJECT_MODULE_CONTINUE` | `REJECT_MODULE_CONTINUE` |
| `DUPLICATE_MODULE_ID` | validation | `FAIL_FAST` (strict startup) or `REJECT_MODULE_CONTINUE` | `REJECT_MODULE_CONTINUE` |
| `KERNEL_API_INCOMPATIBLE` | validation | `FAIL_FAST` | `REJECT_MODULE_CONTINUE` |
| `CONTRACT_VERSION_UNSUPPORTED` | validation | `FAIL_FAST` | `REJECT_MODULE_CONTINUE` |
| `ENTRYPOINT_INVALID` | validation/initialization | `FAIL_FAST` (startup) or `REJECT_MODULE_CONTINUE` (runtime load) | `REJECT_MODULE_CONTINUE` |
| `REGISTRATION_FAILED` | registration | `FAIL_FAST` (startup) or `REJECT_MODULE_CONTINUE` | `REJECT_MODULE_CONTINUE` |
| `INITIALIZATION_FAILED` | initialization | `FAIL_FAST` (startup) or `REJECT_MODULE_CONTINUE` | `REJECT_MODULE_CONTINUE` |
| `RUNTIME_FAILURE` | runtime | `MARK_MODULE_DEGRADED` or `REJECT_MODULE_CONTINUE` per rule | `MARK_MODULE_DEGRADED` |
| `STATE_TRANSITION_INVALID` | state-transition | `REJECT_MODULE_CONTINUE` + reject transition | `REJECT_MODULE_CONTINUE` + reject transition |

## 6. Initialization Failure Behavior
- If failure policy resolves to `FAIL_FAST` during startup:
  - kernel startup MUST fail with clear root-cause error payload.
- If failure policy resolves to `REJECT_MODULE_CONTINUE`:
  - module transitions to `failed`,
  - module is not routable,
  - kernel startup/runtime continues.
- If failure policy resolves to `MARK_MODULE_DEGRADED`:
  - module stays available only for operations allowed by degraded-mode gate,
  - health endpoint MUST report degraded status.

## 7. Configuration Model
Example configuration:

```yaml
module:
  failurePolicy:
    rules:
      - priority: 100
        when:
          kernelMode: startup
          moduleCriticality: mandatory
          stage: initialization
        action: FAIL_FAST
      - priority: 50
        when:
          stage: runtime
          errorCode: RUNTIME_FAILURE
        action: MARK_MODULE_DEGRADED
      - priority: 10
        when: {}
        action: REJECT_MODULE_CONTINUE
```

## 8. Logging and Operational Requirements
For every policy decision, kernel MUST emit a structured log with:
- `eventType=module.failure.policy.decision`
- `policyAction`
- `errorCode`
- `stage`
- `moduleId`
- `moduleCriticality`
- `kernelMode`
- `decisionRuleId` (or priority)
- `errorId` (from canonical error payload)

Minimum metrics:
- `module_failure_policy_decisions_total{policyAction,errorCode,stage}`
- `module_fail_fast_total{reason}`
- `module_degraded_total{moduleId}`

Alerting baseline:
- Alert on any `FAIL_FAST`.
- Alert on repeated `MARK_MODULE_DEGRADED` for the same module within a configurable window.

## 9. Conformance
An implementation is conformant with `v1.0.0` if it:
1. supports the three policy actions,
2. resolves policy by deterministic rule order,
3. applies matrix-consistent behavior for initialization failures and runtime failures,
4. emits required logs and metrics.
