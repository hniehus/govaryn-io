# Module Contract Specification v1.0.0

## 1. Purpose
This specification defines the formal module contract for kernel modules. It is binding for module producers and for kernel-side contract validation.

## 2. Standard Status
`v1.0.0` is the first binding contract standard for Govaryn modules.
- Modules MUST conform to this specification to be accepted by the kernel.
- Kernel implementations MUST validate module manifests against this versioned standard.
- Future versions may extend this contract, but `v1.0.0` remains normative for Increment 1.

## 3. Contract Model
The contract is hybrid and has two mandatory parts:
- Manifest metadata (`module.json`) validated before module load.
- Code entrypoint contract implemented by the module runtime class/function.

## 4. Required Attributes
Every module manifest MUST provide:

| Attribute | Type | Rules |
| --- | --- | --- |
| `moduleContractVersion` | string | MUST be `1.0.0` for this spec version. |
| `moduleId` | string | Unique across all installed modules. Pattern: `^[a-z][a-z0-9-]{2,63}$`. |
| `moduleName` | string | Human-readable name, 3-80 chars. |
| `moduleVersion` | string | Semantic version (`MAJOR.MINOR.PATCH`). |
| `requiredKernelApiVersion` | string | Semantic version range supported by the module (for example `^1.2.0`). |
| `providedCapabilities` | array[string] | Capabilities exposed by this module. MUST contain at least one unique capability ID. |
| `requiredCapabilities` | array[string] | Mandatory capabilities required from other modules/kernel services. MAY be empty. Values MUST be unique. |
| `failurePolicy` | object | Module-declared default failure behavior hints. See semantics in section 5. |
| `moduleType` | string | One of: `core-extension`, `integration`, `feature`, `observability`, `security`. |
| `entryPoint` | string | Fully qualified runtime entrypoint (for example class name or symbol path). |

## 5. Required Field Semantics
- `moduleId`: stable technical identity and primary key in the kernel registry.
- `moduleVersion`: version of the module artifact itself, independent of contract version.
- `requiredKernelApiVersion`: compatibility gate checked before registration.
- `providedCapabilities`: declarations that other modules may depend on.
- `requiredCapabilities`: mandatory dependency surface at capability level, resolved by kernel orchestration.
- `failurePolicy`: module-specific default policy hints used by the kernel failure-policy engine.
  - Required subfields:
    - `onInitializationFailure`
    - `onRuntimeFailure`
  - Allowed values: `FAIL_FAST`, `REJECT_MODULE_CONTINUE`, `MARK_MODULE_DEGRADED`
  - Precedence: explicit kernel failure-policy rules override module-level `failurePolicy` values.

## 6. Optional Attributes

| Attribute | Type | Default | Notes |
| --- | --- | --- | --- |
| `description` | string | empty | Free-text module description (max 500 chars). |
| `author` | string | empty | Team or owner identifier. |
| `license` | string | empty | SPDX expression recommended. |
| `homepage` | string | empty | HTTPS URL. |
| `optionalCapabilities` | array[string] | `[]` | Optional, non-blocking capability dependencies. Missing values do not block initialization. |
| `configSchemaRef` | string | empty | Path/URI to module configuration schema. |
| `healthChecks` | array[string] | `[]` | Named health checks exposed by the module. |

## 7. Uniqueness and Validation Rules
- `moduleId` MUST be globally unique within one kernel instance.
- `moduleName` SHOULD be unique for operational clarity.
- `moduleId` is immutable after first release.
- `moduleVersion` MUST follow semantic versioning.
- `providedCapabilities` entries MUST be unique within a module.
- `requiredCapabilities` entries MUST be unique within a module.
- `optionalCapabilities` entries MUST be unique within a module.
- A capability ID MUST match `^[a-z][a-z0-9.:-]{2,127}$`.
- `providedCapabilities` and `requiredCapabilities` MUST be disjoint sets in `v1.0.0`.
- `requiredCapabilities` and `optionalCapabilities` MUST be disjoint sets in `v1.0.0`.
- Modules MUST declare dependencies via capabilities only. Direct module dependency declarations are not supported in `v1.0.0`.

## 8. Validation Rules
Kernel validation occurs in this order:
1. Manifest exists and is parseable.
2. Required attributes are present and valid.
3. `moduleContractVersion` is supported.
4. `requiredKernelApiVersion` is compatible with the running kernel API version.
5. `moduleId` uniqueness check passes.
6. Capability declarations are valid (`providedCapabilities`/`requiredCapabilities`/`optionalCapabilities` uniqueness, format, disjointness).
7. Mandatory capability dependencies can be resolved.
8. Mandatory capability dependency graph is acyclic.
9. `failurePolicy` structure and values are valid.
10. `entryPoint` exists and implements required lifecycle hooks.

Normative reference:
- [`../../module-validation/v1.0.0/validator-rule-catalog.md`](../../module-validation/v1.0.0/validator-rule-catalog.md)
- [`../../module-validation/v1.0.0/error-model.md`](../../module-validation/v1.0.0/error-model.md)
- [`../../module-failure-policy/v1.0.0/failure-policy.md`](../../module-failure-policy/v1.0.0/failure-policy.md)
- [`../../module-capability/v1.0.0/capability-dependency-model.md`](../../module-capability/v1.0.0/capability-dependency-model.md)

## 9. Entrypoint Contract (Code-Based)
The entrypoint MUST provide lifecycle hooks:
- `initialize(context)`
- `start()`
- `stop()`

The kernel MAY define additional optional hooks in future minor versions.

## 10. Metadata Data Model
Authoritative machine-readable model:
- [`module-metadata.schema.json`](./module-metadata.schema.json)

## 11. Positive Example (Valid)
```json
{
  "moduleContractVersion": "1.0.0",
  "moduleId": "audit-log",
  "moduleName": "Audit Log Module",
  "moduleVersion": "1.3.0",
  "requiredKernelApiVersion": "^1.2.0",
  "providedCapabilities": ["audit.write", "audit.query"],
  "requiredCapabilities": ["event.publish"],
  "optionalCapabilities": ["metrics.counter"],
  "failurePolicy": {
    "onInitializationFailure": "REJECT_MODULE_CONTINUE",
    "onRuntimeFailure": "MARK_MODULE_DEGRADED"
  },
  "moduleType": "observability",
  "entryPoint": "io.govaryn.modules.audit.AuditLogModule",
  "configSchemaRef": "schemas/audit-config.schema.json"
}
```

## 12. Negative Example (Invalid)
```json
{
  "moduleContractVersion": "1.0.0",
  "moduleId": "Audit Log",
  "moduleName": "A",
  "moduleVersion": "1.3",
  "requiredKernelApiVersion": "",
  "providedCapabilities": ["audit.write", "audit.write"],
  "requiredCapabilities": ["audit.write"],
  "optionalCapabilities": ["audit.write"],
  "failurePolicy": {
    "onInitializationFailure": "CONTINUE",
    "onRuntimeFailure": "MARK_MODULE_DEGRADED"
  },
  "moduleType": "observability",
  "entryPoint": ""
}
```

Expected validation failures include:
- `moduleId` pattern violation
- `moduleName` minimum length violation
- `moduleVersion` semantic version format violation
- `requiredKernelApiVersion` empty value
- duplicate capability in `providedCapabilities`
- overlap between `providedCapabilities` and `requiredCapabilities`
- overlap between `requiredCapabilities` and `optionalCapabilities`
- unsupported `failurePolicy.onInitializationFailure` value
- empty `entryPoint`
