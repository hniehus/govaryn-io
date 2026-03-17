# Govaryn IO

Govaryn IO is a portfolio decision & governance system: **strategy → investment → capacity → delivery reality → value** — with **audit-grade traceability**.

If you like building systems where correctness, boundaries, and evidence matter more than dashboards and guesswork, you’ll feel at home here.

---

## What this repo is

This repository contains:

- a **modular Spring Boot** application (classic 3-layer, organized as a modular monolith)
- **Rust compute services** for heavy optimization/simulation workloads
- a **Helm chart** for self-hosting
- **Ansible** for SaaS provisioning/configuration

The guiding idea: **keep business truth in one place**, keep compute fast and replaceable, and make decisions explainable months later.

---

## Principles (the stuff we don’t compromise on)

### Tenant isolation by construction
No “remember to add tenant_id”. Tenant boundaries are enforced as a default, not a convention.

### Append-only audit trail
Decision-grade objects have a history that can’t be hand-waved away. You can tell *who changed what, when, and why*.

### Evidence-based status
Whenever possible, the system should reflect delivery reality through integrations and facts — not manual traffic lights.

### Compute services are not the source of truth
Rust services do compute. The authoritative business state stays in the core system.

---

## Architecture at a glance

- **Java (latest LTS)** + **Spring Boot (latest)**, pinned centrally via a parent POM
- **Maven multi-module**
- **Rust workspace** for compute-only services
- “Modular monolith” approach: module boundaries and contracts matter; deployment stays simple

---

## Repository layout

```text
apps/api                      Spring Boot API
platform/*                    cross-cutting modules (identity, tenancy, audit, i18n, ...)
domains/*                     business modules (strategy, funding, capacity, dependencies, ...)
rust-services/*               compute-only services
infra/helm/govaryn            self-hosting Helm chart
infra/ansible                 SaaS provisioning/config (Ansible)
docs                          ADRs, security, compliance
```

---

## Quickstart (development)

### 1) Initialize the Maven wrapper (recommended)

```bash
./scripts/mvnw-init.sh
```

### 2) Build & test

```bash
mvn -B -ntp clean test
# or: ./mvnw -B -ntp clean test
```

### 3) Run the API

```bash
mvn -pl apps/api spring-boot:run
# API:    http://localhost:8080
# Health: http://localhost:8080/actuator/health
```

---

## Self-hosting (Helm)

The Helm chart lives at `infra/helm/govaryn`.

```bash
helm upgrade --install govaryn infra/helm/govaryn -n govaryn --create-namespace
```

**Important:** the included Postgres manifest is a placeholder. For a real install, use a proper Postgres chart/StatefulSet with PVCs, backups, and secrets management.

---

## How to contribute

If you want to contribute, start here:

- Read **CONTRIBUTING.md**
- Browse **docs/** for ADRs and constraints
- Pick something small first (docs, tests, a tight bug fix) and get a feel for the module boundaries

Blunt truth: PRs that break tenant safety, ignore module boundaries, or ship without tests won’t make it in — and that’s intentional.

---

## Project notes

- This README is intentionally plain-spoken. If something is unclear, open an issue or improve the docs.
- If you’re proposing a change that affects architecture, interfaces, or module boundaries: write an ADR. Future you will be grateful.
