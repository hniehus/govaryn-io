# Govaryn IO

**Govaryn** is a portfolio decision & governance system: strategy → investment → capacity → delivery reality → value — with audit-grade traceability.

This repo is a **modular Spring Boot (3-layer) modular monolith** plus **Rust compute services** for heavy optimization/simulation workloads.
Self-hosting is supported via a **Helm chart**.

## Non-negotiables
- **Tenant isolation by construction** (no “remember to add tenant_id”)
- **Append-only audit trail** for decision-grade objects
- **Evidence-based status** (integrations > manual traffic lights)
- **Compute services (Rust) are not the source of truth**

## Tech baseline
- Java LTS toolchain (default: 25)
- Spring Boot (pinned centrally via parent POM)
- Maven multi-module
- Rust workspace for compute-intensive services

## Structure
```
apps/api                      Spring Boot API
platform/*                    cross-cutting modules (identity, tenancy, audit, i18n, ...)
domains/*                     business modules (strategy, funding, capacity, dependencies, ...)
rust-services/*               compute-only services
infra/helm/govaryn            self-hosting Helm chart
infra/ansible                 SaaS provisioning/config (Ansible)
docs                          ADRs, security, compliance
```

## Quickstart (dev)
1) Init Maven wrapper (recommended):
```bash
./scripts/mvnw-init.sh
```

2) Build & test:
```bash
mvn -B -ntp clean test
# or ./mvnw -B -ntp clean test
```

3) Run API:
```bash
mvn -pl apps/api spring-boot:run
# API: http://localhost:8080
# Health: http://localhost:8080/actuator/health
```

## Self-hosting (Helm)
Helm chart is at `infra/helm/govaryn`.

Example:
```bash
helm upgrade --install govaryn infra/helm/govaryn -n govaryn --create-namespace
```

⚠️ Note: the included Postgres manifest is a placeholder. For real installs, use a proper Postgres chart/StatefulSet with PVC, backups, and secrets.

## Contributing
Read CONTRIBUTING.md. PRs without tests and tenant-safety are dead on arrival.
