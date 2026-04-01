# How To Build a Conformant Module

## 1. Implement the Module Interface

Create a class implementing `KernelModule` (or `KernelRuntimeModule`) and provide:

- `metadata()`
- `initialize(context)`
- `start()`
- `stop()`

## 2. Define Valid Metadata

In `metadata()`, ensure:

- stable, unique `moduleId`
- valid semantic `moduleVersion`
- realistic `requiredKernelApiVersion`
- correct `moduleType`

If your module is optional, keep behavior safe when services are unavailable.

## 3. Keep Initialization Deterministic

`initialize(context)` should:

- validate required capabilities
- fail fast with meaningful exception messages when mandatory prerequisites are missing
- avoid side effects that cannot be rolled back

## 4. Understand Startup Outcomes

Your module may be:

- discovered
- validated
- rejected
- registered
- initializing
- initialized
- failed
- degraded

These transitions are kernel-controlled:
- [module-lifecycle/v1.0.0/lifecycle-model.md](../architecture/module-lifecycle/v1.0.0/lifecycle-model.md)

## 5. Test Against Negative Cases

Before publishing, test your module against:

- incompatible API requirement
- duplicate `moduleId`
- initialization failure path

Use:
- [REFERENCE_MODULES.md](./REFERENCE_MODULES.md)

## 6. Verify Structured Diagnostics

Check startup logs for:

- module discovery event
- validation report and issues
- registration and initialization events
- startup summary counters

This is the minimum operability baseline for support and platform teams.

## 7. Consume Kernel Authentication (Do Not Re-Validate Tokens)

If your module exposes HTTP endpoints:

- rely on kernel authentication and the Spring `Authentication` already established by the kernel
- use authorities from the authenticated context for module logic
- do not parse/validate bearer tokens inside module code

Example usage pattern:

```java
@RestController
class ModuleStatusController {

    @GetMapping("/api/modules/example/status")
    Map<String, Object> status(Authentication authentication) {
        return Map.of(
            "subject", authentication.getName(),
            "authorities", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList()
        );
    }
}
```

The module consumes authenticated context only; token validation remains kernel-owned.

## 8. Register Protected Resource Rules Through Kernel SPI

If your module has protected resources, provide a `ModuleSecurityContributor` bean and register resource policies.

- define `moduleId()` with your module identity
- register each protected `resourceType`
- declare supported `AuthorizationAction` values
- implement evaluator logic that returns explicit `AuthorizationDecision`

Example:

```java
@Component
final class ExampleSecurityContributor implements ModuleSecurityContributor {

    @Override
    public String moduleId() {
        return "example-module";
    }

    @Override
    public void contribute(ModuleSecurityRegistry registry) {
        registry.registerResourcePolicy(
            "example-resource",
            EnumSet.of(AuthorizationAction.READ, AuthorizationAction.UPDATE),
            this::evaluate
        );
    }

    private AuthorizationDecision evaluate(AuthorizationRequest request) {
        Set<String> scopes = AuthorizationScopeExtractor.extractScopes(request);
        if (request.action() == AuthorizationAction.READ && scopes.contains("example.read")) {
            return AuthorizationDecision.allow("example.scope-policy");
        }
        return AuthorizationDecision.deny(DenyReason.RESOURCE_ACCESS_DENIED, "example.scope-policy");
    }
}
```

Keep evaluator logic explicit and deterministic. Do not add a module-local enforcement pipeline.

## 9. Use Kernel Enforcement Path

- For kernel standard backend paths, authorization is enforced automatically by the kernel before service access.
- For additional protected module endpoints, call `KernelAuthorizationEnforcer` before business logic.
- Throwing/propagating `KernelAccessDeniedException` preserves the platform-standard deny response (`403 ACCESS_DENIED`).

Denied decisions are logged structurally (`event=authorization_deny_audit`) by the kernel; do not duplicate sensitive payload logging in module code.
