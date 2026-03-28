# 5. Building Block View

## 5.1 Whitebox Overall System

### Kernel (Gatekeeper and Orchestrator)

Responsibilities:

- Discover module candidates from classpath and plugin sources.
- Validate module metadata against the formal module contract.
- Enforce kernel API compatibility and capability dependency rules.
- Register valid modules and track lifecycle state.
- Orchestrate initialization and start with failure policy enforcement.
- Emit structured logs and startup summaries.

### Module

Responsibilities:

- Provide compliant module metadata and capabilities.
- Implement `KernelModule` and declare supported kernel API version.
- Execute initialization and optional runtime start logic.
- Handle errors locally when possible, surfacing failures to the kernel.

### Supporting Components

- `ModuleDiscoveryService`: candidate discovery and source tracking.
- `ModuleValidator`: contract validation and error model generation.
- `ModuleRegistry`: authoritative registry of module status and metadata.
- `ModuleDependencyGraphValidator`: capability resolution and cycle detection.
- `ModuleInitializationExecutor`: controlled initialization with policy handling.
- `KernelHttpSecurityConfiguration`: central security filter chain and JWT resource-server wiring.
- `GovarynKernelSecurityProperties`: validated security configuration (`enabled`, `issuer-uri`, `audience`, `public-paths`, authority claim/prefix).
- `KernelJwtAuthenticationConverter` + `KernelJwtAuthoritiesConverter`: standardized principal/authority mapping from JWT claims.
- `KernelAuthenticationFailureEntryPoint`: sanitized authentication failure categorization and 401 responses.

## 5.2 Whitebox Level 2

### Kernel Module Orchestration (Level 2)

Key steps:

1. Discovery -> candidates (with origin/source).
2. Validation -> reports (valid/rejected + issues).
3. Collision detection -> abort on duplicate `moduleId`.
4. Dependency validation -> capability graph + cycle check.
5. Registration -> registry entries for valid modules.
6. Initialization/start -> policy-controlled transitions and logs.

## 5.3 Whitebox Level 3

### Registry State Model (Level 3)

States include:

- `discovered`, `validated`, `rejected`, `registered`
- `initializing`, `initialized`
- `failed`, `degraded`

See: `docs/architecture/module-lifecycle/v1.0.0/lifecycle-model.md`.
