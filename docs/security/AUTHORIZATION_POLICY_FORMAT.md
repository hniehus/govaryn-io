# Kernel Authorization Policy Format (V1)

This document defines the V1 YAML format for kernel-owned policy authorization.

## Goals

- Declarative and strongly structured
- No expression language
- No scripting
- Suitable for deterministic machine validation

## Top-level structure

```yaml
policySetRevision: "2026-03-30.v1"
rules:
  - ...
```

- `policySetRevision` (required, string): policy set revision identifier.
- `rules` (required, non-empty list): ordered policy rules.

## Rule structure

```yaml
- id: permit-module-status-read
  effect: PERMIT
  subject:
    roles: [ROLE_admin, ROLE_support]
  actions: [read]
  resourceTypes: [module-status]
  resourceIds: [reference-minimal]     # optional
  context:                              # optional
    attributes:
      environment:
        anyOf: [prod]
```

- `id` (required, string): unique rule identifier within a policy set.
- `effect` (required): `PERMIT` or `DENY`.
- `subject.roles` (required, non-empty list of strings): role-based subject match criteria.
- `actions` (required, non-empty list of strings).
- `resourceTypes` (required, non-empty list of strings).
- `resourceIds` (optional list of strings).
- `context.attributes` (optional object):
  - key = context attribute name
  - value object currently supports:
    - `anyOf` (required, non-empty list of strings)

## V1 matching model limits

- Exact membership style matching only.
- No boolean expressions.
- No arithmetic/string expression operators.
- No embedded script execution.

## Example files in repository

- Valid example:
  - `kernel/src/test/resources/security/authorization/policy/policy-valid.yaml`
- Invalid example:
  - `kernel/src/test/resources/security/authorization/policy/policy-invalid.yaml`

## Runtime loading configuration

- `govaryn.kernel.authorization.enabled=true`
- `govaryn.kernel.authorization.policy-path=/absolute/or/relative/path/to/policy.yaml`

When authorization policy loading is enabled, startup requires a valid policy file.
