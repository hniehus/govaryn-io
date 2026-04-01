# 6. Runtime View

This section captures runtime behavior of the modular kernel.

## Runtime Scenarios

1. Module startup lifecycle (discovery to initialization)
2. Initialization failure handling with policy enforcement
3. Runtime start failure handling
4. Observability and operations flow
5. HTTP authentication flow for public/protected endpoints
6. Kernel authorization framework flow for protected backend paths

## Module Startup Lifecycle

```mermaid
flowchart TB
    A[Discovery] --> B[Validation]
    B -->|valid| C[Registration]
    B -->|invalid| X[Rejected]
    C --> D[Dependency Graph Validation]
    D -->|acyclic| E[Initialization]
    D -->|cycle| Y[Startup Aborted]
    E -->|success| F[Initialized]
    E -->|failure| G[Policy Applied]
    G -->|REJECT_MODULE_CONTINUE| H[Failed]
    G -->|MARK_MODULE_DEGRADED| I[Degraded]
    G -->|FAIL_FAST| Y
```

## Failure Handling (Initialization and Start)

- Initialization errors are captured with module context and failure policy.
- `REJECT_MODULE_CONTINUE` keeps the kernel running and marks the module as `failed`.
- `MARK_MODULE_DEGRADED` keeps the kernel running and marks the module as `degraded`.
- `FAIL_FAST` aborts kernel startup when a module fails.

## Observability Flow

- Each phase emits structured logs (`module_discovered`, `module_validation_report`,
  `module_registered`, `module_initialization_*`, `module_start_failed`).
- A startup summary is emitted once per kernel start:
  `found`, `validated`, `rejected`, `registered`, `failed`, `degraded`.
- Module status is exposed through the status service.

## HTTP Authentication Flow (Current Foundation)

```mermaid
sequenceDiagram
    participant Client
    participant Kernel
    participant OIDC as OIDC Issuer/JWKS

    Client->>Kernel: GET /health (no token)
    Kernel-->>Client: 200 OK (public path)

    Client->>Kernel: GET /api/kernel/whoami (Bearer token)
    Kernel->>OIDC: Resolve issuer metadata / JWKS (as needed)
    OIDC-->>Kernel: Metadata/keys (or error)
    alt Token valid (issuer, signature, exp/nbf, audience)
        Kernel-->>Client: 200 OK with mapped security identity
    else Validation or verification-material failure
        Kernel-->>Client: 401 Unauthorized
        Kernel-->>Kernel: Log sanitized failure category
    end
```

## Kernel Authorization Framework Flow (Standard Path Enforcement)

```mermaid
sequenceDiagram
    participant Client
    participant Controller as Kernel-Managed Controller Path
    participant ReqCtx as KernelRequestSecurityContext
    participant Enforcer as KernelAuthorizationEnforcer
    participant Service as KernelAuthorizationService
    participant Registry as ResourcePolicyRegistry
    participant Eval as Module ResourcePolicyEvaluator
    participant Audit as AuthorizationAuditLogger

    Client->>Controller: Protected request (authenticated)
    Controller->>Enforcer: enforce(moduleId, action, resourceType, resourceId?, attributes)
    Enforcer->>ReqCtx: current()
    ReqCtx-->>Enforcer: SecurityContext
    Enforcer->>Service: authorize(AuthorizationRequest)
    Service->>Registry: resolve(moduleId, resourceType)
    Registry-->>Service: ResourcePolicyRegistration
    Service->>Eval: evaluate(request)
    alt Decision is DENY / missing registration / unsupported action / evaluation error
        Service->>Audit: logDenied(...)
        Service-->>Enforcer: AuthorizationDecision(denied)
        Enforcer-->>Controller: throw KernelAccessDeniedException
        Controller-->>Client: 403 {code=ACCESS_DENIED, reason=ACCESS_DENIED}
    else Decision is ALLOW
        Service-->>Enforcer: AuthorizationDecision(allowed)
        Enforcer-->>Controller: continue
        Controller-->>Client: 2xx + business response
    end
```

Startup guardrail:
- `KernelProtectedAuthorizationIntegrationGuardrail` fails startup if required protected resource/action registrations are missing.
