# Govaryn IO

Govaryn IO is a portfolio decision & governance system: **strategy → investment → capacity → delivery reality → value** — with **audit-grade traceability**.

If you like building systems where correctness, boundaries, and evidence matter more than dashboards and guesswork, you’ll feel at home here.

---

## What this repo is

This repository contains:

- the **Govaryn Kernel** runtime (`kernel/`) built with Spring Boot
- shared **module example sources** (`modules/`) used by kernel reference scenarios
- architecture, ADR, operations, and security documentation (`docs/`)
- local configuration templates (`config/`) and build/run helper scripts (`scripts/`, `run.sh`)

The guiding idea: **keep business truth in one place**, enforce boundaries centrally, and make decisions explainable months later.

---

## Principles (the stuff we don’t compromise on)

### Tenant isolation by construction
No “remember to add tenant_id”. Tenant boundaries are enforced as a default, not a convention.

### Append-only audit trail
Decision-grade objects have a history that can’t be hand-waved away. You can tell *who changed what, when, and why*.

### Evidence-based status
Whenever possible, the system should reflect delivery reality through integrations and facts — not manual traffic lights.

### Compute services are not the source of truth
External compute services can do heavy processing, but authoritative business state stays in the core system.

---

## Architecture at a glance

- **Java 21** + **Spring Boot 4.0.3**, pinned in the parent POM
- **Maven multi-module**
- “Modular monolith” approach: module boundaries and contracts matter; deployment stays simple

---

## Repository layout

```text
kernel/                       Spring Boot kernel runtime
modules/                      shared example module sources
docs/                         architecture, ADRs, operations, security, compliance
config/                       local application.properties templates
scripts/                      Maven wrapper bootstrap helpers
run.sh                        local kernel startup wrapper
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
./run.sh
# or: mvn -pl kernel spring-boot:run
# or: ./mvnw -pl kernel spring-boot:run
```

**Kernel endpoints:**
- Health (public): http://localhost:8080/health
- Actuator health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/metrics
- Info: http://localhost:8080/actuator/info
- Who am I (protected): http://localhost:8080/api/kernel/whoami
- Module status (protected + policy-enforced): http://localhost:8080/modules/status

---

## Monitoring with Spring Boot Actuator

The Kernel includes Spring Boot Actuator for monitoring and metrics.

**Available endpoints** (in `config/application.properties`):
- `/actuator/health` — application health status
- `/actuator/metrics` — detailed metrics
- `/actuator/env` — environment properties
- `/actuator/configprops` — configuration properties

**Configuration**:
- `management.endpoints.web.exposure.include` — expose specific endpoints
- `management.endpoint.health.show-details` — control health endpoint detail level

---

## Kernel Policy Authorization (Current)

The kernel implements policy-based authorization with a kernel-owned PDP.

- Policy source: external YAML (`govaryn.kernel.authorization.policy-path`)
- Decision model: deny overrides permit, default deny when no rule matches, fail closed on evaluation errors
- Module contract: modules integrate protected resources through `ModuleSecurityContributor`; kernel enforces decisions
- Reload hook: `POST /api/kernel/internal/authorization/policy/reload` (protected endpoint)

Reference docs:
- `docs/security/AUTHORIZATION_POLICY_FORMAT.md`
- `docs/security/MODULE_AUTHORIZATION_CONTRACT.md`
- `docs/security/KERNEL_POLICY_AUTHZ_INTEGRATION_NOTE.md`

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
