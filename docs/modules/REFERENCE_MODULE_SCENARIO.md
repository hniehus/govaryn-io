# Reference Module Scenario (REF-01)

## Purpose

This document defines one realistic but intentionally small reference module that new module authors can use as a blueprint.

## Selected Reference Scenario

### Module Name

`Reference Feature Module` (`reference-feature`)

### Business Description

The module represents a lightweight feature extension that depends on a platform-level configuration capability and exposes one feature capability for downstream modules.

It models a typical integration pattern:

- consume one shared platform capability
- expose one feature capability
- initialize safely without domain-specific complexity

## Responsibility Boundaries

### In Scope (module responsibility)

- Declare contract-compliant metadata
- Declare and consume required capability dependencies
- Provide one feature capability
- Perform deterministic, minimal initialization

### Out of Scope (explicitly excluded)

- Business workflows
- External system integrations
- Persistence, messaging, or long-running jobs
- Feature-rich runtime behavior

## Capability Model

### Provided Capabilities

- `reference.feature.sample`

### Required Capabilities

- `reference.platform.config`

### Optional Capabilities

- `reference.platform.metrics`

This ensures the reference module demonstrates both producer and consumer behavior in the kernel capability graph.

## Why This Module Is a Good Reference

- It is technically realistic: dependency declaration, capability exposure, lifecycle participation
- It stays small: no project-specific logic, no infrastructure complexity
- It is directly runnable in profile `module-reference`
- It includes a controlled failure profile `module-reference-failing-init` for diagnostics tests
- It is validated by existing startup/discovery/integration tests

## Source References

- Module class: `modules/src/main/java/io/govaryn/modules/examples/ReferenceFeatureModule.java`
- Support provider: `modules/src/main/java/io/govaryn/modules/examples/ReferencePlatformSupportModule.java`
- Spring profile wiring: `modules/src/main/java/io/govaryn/modules/examples/ReferenceModulesConfiguration.java`
- Integration proof:
  - `kernel/src/test/java/io/govaryn/kernel/module/discovery/ReferenceModuleIntegrationTest.java`
  - `kernel/src/test/java/io/govaryn/kernel/module/discovery/ReferenceModuleFailurePathIntegrationTest.java`
