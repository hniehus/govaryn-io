# 2. Architecture Constraints

Current constraints evident in the repository and existing documentation.

## Constraints

| Constraint | Category | Impact |
| --- | --- | --- |
| Spring Boot 4 / Java 21 stack | Technical | Architecture and extension points follow Spring Security, configuration properties, and Boot startup model |
| Kernel-owned module contract and lifecycle governance | Organizational/Technical | Modules must comply with contract metadata and capability rules before activation |
| Kernel-owned authentication pipeline | Technical | Modules may consume security context but must not introduce independent token validation |
| First-cut single-provider JWT/OIDC model (`issuer-uri` + `audience`) | Technical | Security docs and architecture claims must stay narrow; no multi-provider/SAML/introspection assumptions |
| Fail-closed authentication behavior on verification uncertainty | Security | Protected endpoints must reject access when token verification material is unavailable |
