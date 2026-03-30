# 4. Solution Strategy

Summarize the fundamental strategic decisions and architecture approaches.

## Strategy Items

- Technology and platform choices
- Decomposition strategy
- Integration strategy
- Security and compliance strategy
- Scalability and availability strategy

## Current Strategy Notes

- Authentication is kernel-owned and implemented as a JWT resource-server foundation (single configured OIDC-compatible issuer).
- Authorization is kernel-owned and policy-based:
  - modules provide decision input (`subject`, `action`, `resourceType`, optional `resourceId` and constrained `context`)
  - kernel PDP evaluates against active YAML policy
  - deny overrides permit, default deny, and fail closed on internal errors
- Policy lifecycle uses external YAML + parse + semantic validation + atomic activation with last-known-valid retention on reload failures.
