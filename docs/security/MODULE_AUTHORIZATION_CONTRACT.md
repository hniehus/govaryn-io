# Module Authorization Contract (Kernel-Owned Decisions)

Modules must delegate authorization decisions to the kernel through `KernelAuthorizationService`.

## Authorization request structure

Modules provide decision input only. The kernel owns decision evaluation.

- `subject`: `AuthorizationSubject(subjectId, roles, attributes)`
- `action`: required operation name
- `resourceType`: required resource class
- `resourceId`: optional object-level identifier
- `context`: optional constrained key-value map

## Required inputs

- `subject` (who is requesting)
- `action` (required)
- `resourceType` (required)

## Optional inputs

- `resourceId` (object-level authorization)
- `context` (constrained key-value attributes)

## Input validation guidance

`KernelAuthorizationOperation` validates:
- `action` format: `^[a-z][a-z0-9._:-]{1,63}$`
- `resourceType` format: `^[a-z][a-z0-9._:-]{1,63}$`
- supported context keys (V1): `environment`

Invalid or missing required input is rejected safely and results in a deny decision when routed via `KernelAuthorizationService`.

## Reference usage

See `ReferenceFeatureModule#restartProtectedModule(...)` for delegation before business execution.

## Developer example

```java
AuthorizationDecision decision = kernelContext.authorizationService().authorize(
    new AuthorizationSubject(identity.subject(), List.copyOf(identity.authorities()), Map.of()),
    KernelAuthorizationOperations.of(
        "restart",
        "module",
        "reference-minimal",
        Map.of("environment", "prod")
    )
);

if (decision.result() != AuthorizationDecisionResult.PERMIT) {
    throw new IllegalStateException("Forbidden");
}
```

This pattern keeps modules free of policy ownership and token validation logic.
