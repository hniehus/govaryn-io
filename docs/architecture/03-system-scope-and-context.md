# 3. System Scope and Context

## 3.1 Business Context

The Govaryn kernel provides a managed runtime for modular capabilities. Module teams
publish compliant modules that can be discovered, validated, registered, and started
without compromising platform integrity. Operators rely on structured startup logs and
status views to understand module health and enforce governance.

## 3.2 Technical Context

The module system operates inside the kernel runtime:

- Modules are Spring-managed `KernelModule` components or classpath-discovered candidates.
- Module metadata conforms to the versioned module contract.
- The kernel acts as gatekeeper and orchestrator for module lifecycle phases.
- Module capabilities declare required/provided features for dependency resolution.
- Module API usage is constrained by kernel API classification (`declared`, `provisional`, `internal`).

## Context Diagram

```mermaid
flowchart LR
    ModuleTeams[Module Teams] -->|Publish Modules| ModuleRepo[Module Packages]
    ModuleRepo -->|Discovery Candidates| Kernel[Govaryn Kernel]
    Operators[Operators] -->|Observe Logs/Status| Kernel
    Kernel -->|Lifecycle Events| Logs[Structured Logs]
    Kernel -->|Status View| StatusAPI[Module Status API]
    Kernel -->|Contracts & Rules| Governance[Module Governance]
```
