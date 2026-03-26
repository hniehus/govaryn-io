# Module Error and Exception Model v1.0.0

## 1. Purpose
This document defines a uniform error payload and exception taxonomy for module validation, registration, initialization, and runtime-state transitions.

## 2. Error Payload (Canonical Structure)
Every module-processing error MUST use this structure:

```json
{
  "errorId": "01JQ9H4C3V7D6A4QW9X8Y2Z1AB",
  "timestamp": "2026-03-22T11:30:00Z",
  "stage": "validation",
  "severity": "ERROR",
  "code": "REQUIRED_FIELD_MISSING",
  "message": "Required field is missing.",
  "moduleId": "audit-log",
  "moduleVersion": "1.3.0",
  "fieldPath": "$.moduleId",
  "details": {
    "missingField": "moduleId"
  },
  "causedBy": null
}
```

## 3. Required Fields

| Field | Type | Description |
| --- | --- | --- |
| `errorId` | string | Unique error identifier for correlation. |
| `timestamp` | string | UTC timestamp in ISO-8601 format. |
| `stage` | enum | One of: `discovery`, `validation`, `registration`, `initialization`, `runtime`, `state-transition`. |
| `severity` | enum | One of: `ERROR`, `WARNING`. |
| `code` | string | Stable machine-readable error code. |
| `message` | string | Human-readable error summary (English). |
| `moduleId` | string/null | Module identity when known. |
| `moduleVersion` | string/null | Module version when known. |
| `fieldPath` | string/null | JSON path or logical field identifier for field-level issues. |
| `details` | object | Structured context for diagnostics. |
| `causedBy` | object/null | Optional nested cause (`code`, `message`). |

## 4. Error Code Set (v1.0.0)

| Code | Stage | Meaning |
| --- | --- | --- |
| `MANIFEST_NOT_FOUND` | validation | Manifest is missing or not readable. |
| `MANIFEST_PARSE_ERROR` | validation | Manifest JSON cannot be parsed. |
| `REQUIRED_FIELD_MISSING` | validation | Required field absent. |
| `INVALID_FIELD_VALUE` | validation | Field value violates format/range/enum rules. |
| `DUPLICATE_MODULE_ID` | validation | `moduleId` already exists in kernel scope. |
| `KERNEL_API_INCOMPATIBLE` | validation | Required kernel API range does not match runtime API version. |
| `CONTRACT_VERSION_UNSUPPORTED` | validation | Module contract version not supported. |
| `ENTRYPOINT_INVALID` | validation, initialization | Entrypoint cannot be resolved or fails lifecycle contract checks. |
| `REGISTRATION_FAILED` | registration | Registry write or registration constraint failure. |
| `INITIALIZATION_FAILED` | initialization | Module initialization failed. |
| `STATE_TRANSITION_INVALID` | state-transition | Requested lifecycle transition is not allowed. |
| `RUNTIME_FAILURE` | runtime | Runtime operation failed in initialized module. |

## 5. Exception Taxonomy
Kernel-side exception model:

| Exception Type | Typical Stage | Mapped Code |
| --- | --- | --- |
| `ModuleValidationException` | validation | Validation codes listed above |
| `ModuleRegistrationException` | registration | `REGISTRATION_FAILED` |
| `ModuleInitializationException` | initialization | `INITIALIZATION_FAILED` |
| `ModuleStateTransitionException` | state-transition | `STATE_TRANSITION_INVALID` |
| `ModuleRuntimeException` | runtime | `RUNTIME_FAILURE` |

Rules:
- Exceptions MUST be mapped to one canonical error payload.
- Exceptions MUST NOT leak raw internal stack traces into user-facing messages.
- Internal traces MAY be logged in secure diagnostic channels.

## 6. Lifecycle Interaction
- Validation errors result in lifecycle state `rejected`.
- Initialization/runtime errors resolve according to failure policy:
  - default: lifecycle state `failed`,
  - policy override: module may be marked degraded without transitioning to `failed`.
- Invalid transition attempts keep current state unchanged and emit `STATE_TRANSITION_INVALID`.

## 7. Logging and Observability Requirements
- Emit one structured log event per canonical error payload.
- Include `errorId`, `moduleId`, `code`, and `stage` in log index fields.
- Emit metrics at least for:
  - validation failures by `code`,
  - initialization failures by module,
  - invalid transition attempts.

## 8. Compatibility and Versioning
- Error codes are append-only within `v1.x`.
- Removing or changing semantic meaning of an existing code requires major version increment.

## 9. Failure Policy Integration
How errors translate to kernel behavior is defined in:
- [`../../module-failure-policy/v1.0.0/failure-policy.md`](../../module-failure-policy/v1.0.0/failure-policy.md)
