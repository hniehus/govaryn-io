# Configuration Management Architecture

This document is the canonical architecture/reference for kernel configuration behavior.

For task-oriented usage, see:
- [Configuration Management Quick Reference](./CONFIGURATION_QUICK_REFERENCE.md)
- [Configuration Management Integration Guide](./CONFIGURATION_INTEGRATION_GUIDE.md)
- [Configuration Management Diagrams](./CONFIGURATION_DIAGRAMS.md)

## Scope

The kernel configuration system provides:
- centralized schema-driven configuration validation
- deterministic merge precedence across defaults, property files, and environment variables
- module schema registration and validation at startup
- secret redaction for diagnostic/log-safe views

Primary implementation classes:
- [`ConfigurationManager`](../kernel/src/main/java/io/govaryn/kernel/config/ConfigurationManager.java)
- [`ConfigurationValidator`](../kernel/src/main/java/io/govaryn/kernel/config/ConfigurationValidator.java)
- [`KernelConfigurationSchema`](../kernel/src/main/java/io/govaryn/kernel/config/KernelConfigurationSchema.java)
- [`ModuleConfigurationSchema`](../kernel/src/main/java/io/govaryn/kernel/config/ModuleConfigurationSchema.java)
- [`SecretRedactor`](../kernel/src/main/java/io/govaryn/kernel/config/SecretRedactor.java)

## Startup lifecycle

Configuration initialization happens during kernel startup:
1. Spring Boot starts application context.
2. Module schemas are registered through `ConfigurationManager.registerModule(...)`.
3. `ConfigurationManager.initialize()` runs (see [`KernelApplication`](../kernel/src/main/java/io/govaryn/kernel/KernelApplication.java)).
4. Configuration is loaded, merged, and validated.
5. Startup fails fast (`System.exit(1)`) if validation fails.

## Merge precedence

Configuration values are merged with strict precedence:

1. Schema defaults (lowest precedence)
2. Spring `Environment` property sources (for example `application.properties`)
3. Environment variables (highest precedence)

Environment variable mapping uses upper snake case:
- `govaryn.kernel.id` -> `GOVARYN_KERNEL_ID`
- `govaryn.modulex.api.key` -> `GOVARYN_MODULEX_API_KEY`

## Validation model

Validation is schema driven:
- required key checks
- type checks (`STRING`, `ENUM`)
- strict enum value validation for strict keys
- module namespace key ownership checks
- optional module-specific custom validation (`validateConfiguration(...)`)

Validation failure behavior:
- all detected errors are aggregated
- a `ConfigurationException` is thrown
- kernel startup is aborted

## Namespacing behavior

Modules define their own namespace (for example `govaryn.payments`) via `ModuleConfigurationSchema`.

`ConfigurationManager.getNamespacedConfiguration(namespace)` returns keys matching the requested prefix.

Important:
- this is filtering behavior, not caller-identity enforcement
- modules/services should request only their own namespace by convention
- cross-module access restrictions are not enforced in this layer

## Secret redaction

`SecretRedactor` protects log/diagnostic output:
- `ConfigurationManager.getAll()` returns redacted values
- `ConfigurationManager.getAllUnredacted()` returns raw values (internal use only)
- default secret patterns include keys containing values like `password`, `secret`, `token`, `apikey`, `credential`, and `auth`
- custom patterns can be registered with `SecretRedactor.registerSecretPattern(...)`

## Kernel schema (current keys)

Kernel-level keys are defined in [`KernelConfigurationSchema`](../kernel/src/main/java/io/govaryn/kernel/config/KernelConfigurationSchema.java):
- `govaryn.kernel.id` (required)
- `govaryn.kernel.environment` (required enum: `DEV|STAGE|PROD`)
- `govaryn.kernel.module.mode` (enum; default `CLASSPATH`)
- `govaryn.kernel.module.plugin-directory` (default `./plugins`)
- `govaryn.kernel.module.failure-policy.initialization` (enum; default `REJECT_MODULE_CONTINUE`)

## Design constraints

- Startup-time initialization only (no runtime dynamic reload in this layer).
- Validation fails closed at startup.
- Keep schemas explicit and key ownership clear.
- Do not log unredacted secrets.

## Related docs

- [Configuration Management Quick Reference](./CONFIGURATION_QUICK_REFERENCE.md)
- [Configuration Management Integration Guide](./CONFIGURATION_INTEGRATION_GUIDE.md)
- [Configuration Management Diagrams](./CONFIGURATION_DIAGRAMS.md)
