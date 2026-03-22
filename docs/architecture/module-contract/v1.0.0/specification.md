# Module Contract Specification v1.0.0

## 1. Purpose
This specification defines the formal module contract for kernel modules. It is binding for module producers and for kernel-side contract validation.

## 2. Contract Model
The contract is hybrid and has two mandatory parts:
- Manifest metadata (`module.json`) validated before module load.
- Code entrypoint contract implemented by the module runtime class/function.

## 3. Required Attributes
Every module manifest MUST provide:

| Attribute | Type | Rules |
| --- | --- | --- |
| `moduleContractVersion` | string | MUST be `1.0.0` for this spec version. |
| `moduleId` | string | Unique across all installed modules. Pattern: `^[a-z][a-z0-9-]{2,63}$`. |
| `moduleName` | string | Human-readable name, 3-80 chars. |
| `moduleVersion` | string | Semantic version (`MAJOR.MINOR.PATCH`). |
| `requiredKernelApiVersion` | string | Semantic version range supported by the module (for example `^1.2.0`). |
| `moduleType` | string | One of: `core-extension`, `integration`, `feature`, `observability`, `security`. |
| `entryPoint` | string | Fully qualified runtime entrypoint (for example class name or symbol path). |

## 4. Optional Attributes

| Attribute | Type | Default | Notes |
| --- | --- | --- | --- |
| `description` | string | empty | Free-text module description (max 500 chars). |
| `author` | string | empty | Team or owner identifier. |
| `license` | string | empty | SPDX expression recommended. |
| `homepage` | string | empty | HTTPS URL. |
| `capabilities` | array[string] | `[]` | Declared capabilities provided by this module. |
| `dependencies` | array[object] | `[]` | Required module dependencies with version ranges. |
| `configSchemaRef` | string | empty | Path/URI to module configuration schema. |
| `healthChecks` | array[string] | `[]` | Named health checks exposed by the module. |

## 5. Uniqueness and Naming Rules
- `moduleId` MUST be globally unique within one kernel instance.
- `moduleName` SHOULD be unique for operational clarity.
- `moduleId` is immutable after first release.
- `moduleVersion` MUST follow semantic versioning.
- Dependency identifiers MUST reference `moduleId`, not `moduleName`.

## 6. Validation Rules
Kernel validation occurs in this order:
1. Manifest exists and is parseable.
2. Required attributes are present and valid.
3. `moduleContractVersion` is supported.
4. `requiredKernelApiVersion` is compatible with the running kernel API version.
5. `moduleId` uniqueness check passes.
6. `entryPoint` exists and implements required lifecycle hooks.

Normative reference:
- [`../../module-validation/v1.0.0/validator-rule-catalog.md`](../../module-validation/v1.0.0/validator-rule-catalog.md)
- [`../../module-validation/v1.0.0/error-model.md`](../../module-validation/v1.0.0/error-model.md)

## 7. Entrypoint Contract (Code-Based)
The entrypoint MUST provide lifecycle hooks:
- `initialize(context)`
- `start()`
- `stop()`

The kernel MAY define additional optional hooks in future minor versions.

## 8. Metadata Data Model
Authoritative machine-readable model:
- [`module-metadata.schema.json`](./module-metadata.schema.json)

## 9. Example Manifest
```json
{
  "moduleContractVersion": "1.0.0",
  "moduleId": "audit-log",
  "moduleName": "Audit Log Module",
  "moduleVersion": "1.3.0",
  "requiredKernelApiVersion": "^1.2.0",
  "moduleType": "observability",
  "entryPoint": "io.govaryn.modules.audit.AuditLogModule",
  "capabilities": ["audit.write", "audit.query"],
  "dependencies": [
    { "moduleId": "event-bus", "versionRange": "^1.0.0" }
  ],
  "configSchemaRef": "schemas/audit-config.schema.json"
}
```
