# Reference Modules and Negative Examples

This document provides ready-to-run module examples for development, testing, and onboarding.

## Available Modules

| Scenario | Class | Profile |
| --- | --- | --- |
| Minimal valid module | `MinimalReferenceModule` | `module-example-minimal` |
| Incompatible kernel API version | `IncompatibleApiVersionModule` | `module-example-incompatible-api` |
| Duplicate module ID pair | `DuplicateIdModuleA` + `DuplicateIdModuleB` | `module-example-duplicate-id` |
| Controlled initialization failure | `FailingInitializationModule` | `module-example-failing-init` |

Source package:
- `io.govaryn.kernel.examples.modules`
Source path:
- `modules/src/main/java/io/govaryn/kernel/examples/modules`

## How To Run A Scenario

Example (minimal valid module):

```bash
mvn -pl kernel spring-boot:run \
  -Dspring-boot.run.profiles=module-example-minimal \
  -Dspring-boot.run.arguments="--spring.config.additional-location=optional:file:./config/"
```

## Notes

- These modules are intentionally simple and are loaded only when the matching profile is active.
- Negative profiles are expected to trigger validation, collision detection, or initialization failure paths.
- Use them to verify startup behavior, structured diagnostics, and failure-policy handling.
