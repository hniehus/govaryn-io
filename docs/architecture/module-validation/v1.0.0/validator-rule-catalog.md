# Module Validator Rule Catalog v1.0.0

## 1. Purpose
This catalog defines validation rules that determine whether a module is valid or invalid before registration and initialization.

## 2. Evaluation Order
Validation MUST be executed in this order:
1. Parse and schema validation.
2. Required field validation.
3. Value and format validation.
4. Uniqueness validation.
5. Kernel API compatibility validation.
6. Capability declaration validation.
7. Required capability resolution.
8. Mandatory capability cycle detection.
9. Entrypoint resolvability validation.

Validation stops at first fatal parsing/schema error. Otherwise, rule violations are aggregated.

## 3. Rule Severity
- `ERROR`: module is invalid and MUST be rejected.
- `WARNING`: module remains valid but warning is recorded.

`v1.0.0` defines only blocking validation rules (`ERROR`).

## 4. Rule Catalog

| Rule ID | Condition | Severity | Error Code |
| --- | --- | --- | --- |
| `VR-001` | Manifest missing or unreadable | ERROR | `MANIFEST_NOT_FOUND` |
| `VR-002` | Manifest is not parseable JSON | ERROR | `MANIFEST_PARSE_ERROR` |
| `VR-003` | Required attribute missing (`moduleId`, `moduleName`, `moduleVersion`, `requiredKernelApiVersion`, `providedCapabilities`, `requiredCapabilities`, `failurePolicy`, `moduleType`, `entryPoint`, `moduleContractVersion`) | ERROR | `REQUIRED_FIELD_MISSING` |
| `VR-004` | Required attribute has invalid value or format (including invalid enum/pattern/version format) | ERROR | `INVALID_FIELD_VALUE` |
| `VR-005` | `moduleId` duplicates another discovered/registered module | ERROR | `DUPLICATE_MODULE_ID` |
| `VR-006` | `requiredKernelApiVersion` is incompatible with running kernel API | ERROR | `KERNEL_API_INCOMPATIBLE` |
| `VR-007` | `moduleContractVersion` unsupported by kernel | ERROR | `CONTRACT_VERSION_UNSUPPORTED` |
| `VR-008` | `entryPoint` cannot be resolved or does not satisfy required lifecycle hooks | ERROR | `ENTRYPOINT_INVALID` |
| `VR-009` | Capability declarations invalid (`providedCapabilities`/`requiredCapabilities`/`optionalCapabilities` duplicates, invalid IDs, or overlap) | ERROR | `CAPABILITY_DECLARATION_INVALID` |
| `VR-010` | `failurePolicy` missing required subfields or contains unsupported policy values | ERROR | `FAILURE_POLICY_INVALID` |
| `VR-011` | One or more `requiredCapabilities` cannot be resolved to any provider | ERROR | `CAPABILITY_UNRESOLVED` |
| `VR-012` | Mandatory capability dependency graph contains a cycle | ERROR | `CAPABILITY_CYCLE_DETECTED` |

## 5. Rule Details

### VR-003 Required Fields
- Missing required fields MUST produce one error per field.
- `fieldPath` MUST identify each missing field, for example `$.moduleId`.

### VR-004 Invalid Values
Examples:
- `moduleId` does not match `^[a-z][a-z0-9-]{2,63}$`
- `moduleVersion` is not semantic version compliant
- `moduleType` outside allowed enum
- capability identifier does not match `^[a-z][a-z0-9.:-]{2,127}$`
- unsupported `failurePolicy` action value

### VR-005 Duplicate moduleId
- Duplicate check scope is one kernel instance.
- Comparison is case-sensitive in `v1.0.0`.

### VR-006 Kernel API Compatibility
- `requiredKernelApiVersion` MUST match running kernel API version using semantic version range evaluation.

### VR-009 Capability Declarations
- `providedCapabilities` MUST contain unique values.
- `requiredCapabilities` MUST contain unique values.
- `optionalCapabilities` MUST contain unique values.
- `providedCapabilities` and `requiredCapabilities` MUST be disjoint in `v1.0.0`.
- `requiredCapabilities` and `optionalCapabilities` MUST be disjoint in `v1.0.0`.

### VR-010 Failure Policy
- `failurePolicy.onInitializationFailure` and `failurePolicy.onRuntimeFailure` are mandatory.
- Allowed values: `FAIL_FAST`, `REJECT_MODULE_CONTINUE`, `MARK_MODULE_DEGRADED`.

### VR-011 Unresolved Required Capability
- Every entry in `requiredCapabilities` MUST have at least one provider among discovered modules/kernel-provided capabilities.
- Missing provider is a blocking error.

### VR-012 Capability Cycle
- Build mandatory capability dependency graph after provider selection.
- Any cycle in the graph is invalid in `v1.0.0`.

## 6. Validation Outcome Mapping
- No `ERROR` findings: module transitions to `validated`.
- One or more `ERROR` findings: module transitions to `rejected`.

## 7. Startup Policy
### Default (`module.validation.startupPolicy=continue-on-reject`)
- Kernel rejects invalid modules and continues startup.

### Strict (`module.validation.startupPolicy=fail-on-reject`)
- Kernel startup fails if any **mandatory** module is rejected.
- Optional modules may still be rejected without aborting startup.

Mandatory/optional classification is provided by deployment configuration, not module manifest (`v1.0.0`).

Precedence:
- Failure behavior is ultimately resolved by the failure-policy engine.
- `module.validation.startupPolicy` is a compatibility shorthand and is translated to failure-policy rules.
- If explicit failure-policy rules exist, they override `module.validation.startupPolicy`.

Normative reference:
- [`../../module-failure-policy/v1.0.0/failure-policy.md`](../../module-failure-policy/v1.0.0/failure-policy.md)
