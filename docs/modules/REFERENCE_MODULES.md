# Reference Modules and Negative Examples

This document provides ready-to-run module examples for development, testing, and onboarding.

## Available Modules

| Scenario | Class | Profile |
| --- | --- | --- |
| Minimal valid module | `MinimalReferenceModule` | `module-example-minimal` |
| Production-like reference module | `ReferenceFeatureModule` + `ReferencePlatformSupportModule` | `module-reference` |
| Production-like reference module (controlled init failure path) | `ReferenceFeatureModule` + `ReferencePlatformSupportModule` | `module-reference-failing-init` |
| Reusable module template (with required/provided capability examples) | `ModuleTemplateModule` + `ModuleTemplateSupportModule` | `module-template` |
| Missing required field (`moduleName` blank) | `MissingRequiredFieldModule` | `module-example-missing-required-field` |
| Incompatible kernel API version | `IncompatibleApiVersionModule` | `module-example-incompatible-api` |
| Duplicate module ID pair | `DuplicateIdModuleA` + `DuplicateIdModuleB` | `module-example-duplicate-id` |
| Controlled initialization failure | `FailingInitializationModule` | `module-example-failing-init` |
| Forbidden internal API usage (negative sample for verifier rule) | `ForbiddenInternalApiUsageModule.java.sample` | n/a (documentation sample) |

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

Example (reusable module template):

```bash
mvn -pl kernel spring-boot:run \
  -Dspring-boot.run.profiles=module-template \
  -Dspring-boot.run.arguments="--spring.config.additional-location=optional:file:./config/"
```

Example (production-like reference module):

```bash
mvn -pl kernel spring-boot:run \
  -Dspring-boot.run.profiles=module-reference \
  -Dspring-boot.run.arguments="--spring.config.additional-location=optional:file:./config/"
```

## Notes

- These modules are intentionally simple and are loaded only when the matching profile is active.
- Negative profiles are expected to trigger validation, collision detection, or initialization failure paths.
- Use them to verify startup behavior, structured diagnostics, and failure-policy handling.

## Security Context Usage Pattern

Current reference modules focus on lifecycle and contract validation; they do not ship a standalone web endpoint.
For module endpoint implementations, use this pattern:

```java
@GetMapping("/api/modules/example/whoami")
Map<String, Object> whoAmI(Authentication authentication) {
    return Map.of(
        "subject", authentication.getName(),
        "authorities", authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList()
    );
}
```

This pattern consumes kernel-provided authentication context and avoids independent module token validation.

## Automated References

Each negative example is referenced in automated tests or verifier checks:

- Missing required field: `NegativeModuleExamplesTest.missingRequiredFieldExampleFailsMetadataConstruction`
- Incompatible kernel API version: `NegativeModuleExamplesTest.incompatibleApiVersionExampleIsRejected` and `ModuleStartupScenariosIntegrationTest.incompatibleApiVersionRejected`
- Duplicate module ID: `NegativeModuleExamplesTest.duplicateModuleIdExamplesAreFlagged` and `ModuleOrchestratorCollisionTest.shouldAbortStartupOnModuleIdCollision`
- Initialization failure: `NegativeModuleExamplesTest.initializationFailureExampleThrows` and `ModuleInitializationFailurePolicyTest`
- Forbidden internal API usage: `KernelArchitectureVerifierTest.internalApiUsageIsForbiddenForModules` and `KernelArchitectureVerifierTest.forbiddenInternalApiSampleIsDetected`
