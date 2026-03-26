# Module Template Author Guide

This guide shows how to create a new runnable Govaryn dummy module from the reusable template, with no project-specific business logic.

## Prerequisites

- Java 21
- Maven
- Repository checked out locally

## Step 1: Create module classes

Create a new package under:

- `modules/src/main/java/io/govaryn/kernel/examples/modules`

For a dummy module, create two classes:

- a support provider module
- your new module

Why two modules?  
The new module demonstrates `requiredCapabilities`. The support module provides that required capability so startup succeeds.

## Step 2: Add complete metadata

Copy/paste this support module example and adjust IDs/names:

```java
package io.govaryn.kernel.examples.modules;

import io.govaryn.kernel.api.KernelContext;
import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.module.ModuleCapabilities;
import io.govaryn.kernel.module.ModuleFailurePolicy;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;

import java.util.List;

public class MyDummySupportModule implements KernelModule {

    @Override
    public ModuleMetadata metadata() {
        return new ModuleMetadata(
            "1.0.0",
            "my-dummy-support",
            "My Dummy Support Module",
            "1.0.0",
            "^1.0.0",
            ModuleType.CORE_EXTENSION,
            getClass().getName(),
            "Support capability provider for dummy module",
            "Your Team",
            "Apache-2.0",
            "https://example.org/modules/my-dummy-support",
            new ModuleCapabilities(
                List.of("dummy.platform.config"),
                List.of(),
                List.of()
            ),
            ModuleFailurePolicy.defaults(),
            null,
            List.of()
        );
    }

    @Override
    public void initialize(KernelContext context) {
        // no-op
    }
}
```

## Step 3: Declare capabilities

Copy/paste this dummy module example and adjust IDs/names:

```java
package io.govaryn.kernel.examples.modules;

import io.govaryn.kernel.api.KernelContext;
import io.govaryn.kernel.api.KernelModule;
import io.govaryn.kernel.module.ModuleCapabilities;
import io.govaryn.kernel.module.ModuleFailurePolicy;
import io.govaryn.kernel.module.ModuleMetadata;
import io.govaryn.kernel.module.ModuleType;

import java.util.List;

public class MyDummyModule implements KernelModule {

    @Override
    public ModuleMetadata metadata() {
        return new ModuleMetadata(
            "1.0.0",
            "my-dummy-module",
            "My Dummy Module",
            "1.0.0",
            "^1.0.0",
            ModuleType.FEATURE,
            getClass().getName(),
            "Dummy module created from template",
            "Your Team",
            "Apache-2.0",
            "https://example.org/modules/my-dummy-module",
            new ModuleCapabilities(
                List.of("dummy.feature.sample"),
                List.of("dummy.platform.config"),
                List.of("dummy.platform.metrics")
            ),
            ModuleFailurePolicy.defaults(),
            null,
            List.of()
        );
    }

    @Override
    public void initialize(KernelContext context) {
        // keep deterministic and side-effect free in template stage
    }
}
```

Capability rules:

- `providedCapabilities`: what your module offers
- `requiredCapabilities`: mandatory dependencies (must be resolvable)
- `optionalCapabilities`: non-blocking dependencies

## Step 4: Wire modules to a Spring profile

Add beans to `ReferenceModulesConfiguration` (or your own configuration class):

```java
@Bean
@Profile("module-example-dummy")
KernelModule myDummySupportModule() {
    return new MyDummySupportModule();
}

@Bean
@Profile("module-example-dummy")
KernelModule myDummyModule() {
    return new MyDummyModule();
}
```

## Step 5: Implement initialization safely

Initialization in template phase should be:

- deterministic
- fast
- no irreversible side effects
- explicit errors for missing prerequisites

Minimal pattern:

```java
@Override
public void initialize(KernelContext context) {
    // validate lightweight preconditions only
}
```

## Step 6: Test locally

### 6.1 Build and run tests

```bash
mvn test compile
```

### 6.2 Start kernel with your profile

```bash
mvn -pl kernel spring-boot:run \
  -Dspring-boot.run.profiles=module-example-dummy \
  -Dspring-boot.run.arguments="--spring.config.additional-location=optional:file:./config/"
```

### 6.3 Verify expected startup behavior

You should see:

- `event=module_discovered`
- `event=module_validation_report ... valid=true`
- `event=module_registered`
- `event=module_initialization_succeeded`
- `event=module_startup_summary`

## Forbidden patterns

Do not do the following:

- Import or use `io.govaryn.kernel.internal.*` APIs (forbidden by architecture verifier)
- Encode dependencies as direct module-to-module references instead of capabilities
- Reuse an existing `moduleId`
- Leave required metadata blank or invalid
- Put business/domain logic into the template module

## Definition of Done for a new contributor

A contributor succeeds if they can:

1. copy the two classes above
2. adjust IDs and names
3. wire one profile in configuration
4. run startup locally
5. see module discovered, validated, registered, initialized

At that point, the dummy module is runnable and contract-conformant.
