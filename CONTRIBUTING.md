# Contributing to Govaryn IO

Want to help build **Govaryn IO**? Nice.  
This project thrives on people who enjoy building things **properly**: clear modules, stable contracts, strong tests, and zero “cleverness” that compromises security.

Govaryn IO is a **PPM tool** with an enterprise backbone: multi-tenant, compliance-minded, modular — and still modern. If that sounds like your kind of problem space, you’ll fit right in.

---

## Why it’s worth contributing

- **Real architecture work**, not ticket-factory grind: modular boundaries, interface design, determinism, performance.
- **Impact**: we’re not polishing pixels — we’re building a system organizations must be able to trust.
- **Solid engineering culture**: clear rules, fast reviews, decisions captured in ADRs.

---

## What you can contribute

You don’t need to do everything. Pick an entry point that matches your strengths:

- **Code**: features, bug fixes, refactorings, performance work
- **Architecture**: ADRs, module boundaries, interface/versioning strategy
- **Docs**: README, how-tos, examples, API/contract documentation
- **Testing**: unit/integration/contract tests, fixtures, test utilities
- **Security/Compliance**: threat modeling, logging minimization, hardening
- **DevEx**: build, CI, templates, automation

---

## Guardrails (non-negotiable)

We’re friendly. But we’re not careless.

### 1) No tenant leaks. Ever.
Multi-tenancy is core product DNA. Every change treats tenant isolation as a **first-class requirement**.

### 2) Module boundaries are real boundaries
No “quick imports” across layers/modules just because it’s convenient.

### 3) No silent breaking changes
APIs/contracts are **versioned**. Changes are announced, documented, and tested.

### 4) Tests are the default
If it’s not pure documentation: tests belong with it. Period.

*(These guardrails stay hard because the product demands it.)*

---

## Tech context (so you’re not starting in the fog)

- **Latest Java LTS** + **latest Spring / Spring Boot**
- **Classic 3-layer architecture** (UI / Application / Domain / Persistence — depending on module)
- **Modular**: clear modules, clear contracts
- **Rust compute services**: for heavy, deterministic computation — but **not** authoritative business state (see below)

---

## Workflow (fast, traceable, review-friendly)

### Branch names
- `feat/<short-description>`
- `fix/<short-description>`
- `chore/<short-description>`

### Commit format (Conventional Commits)
- `feat: ...`
- `fix: ...`
- `docs: ...`
- `refactor: ...`
- `test: ...`
- `chore: ...`

### Pull requests: what must be included
A PR is not just “code”. It’s a **small package of responsibility**:

- **What** changed (1–3 sentences)
- **Why** this is the right solution (rationale)
- **How** it was tested (and where the tests live)
- **Tenant/privacy impact** (short, explicit)
- **ADR** if you change architecture/interfaces/module boundaries

---

## Rust compute services — rules that keep it scalable

Rust services are for compute here, not a second domain.

- **Compute-only**: no authoritative business state
- **Deterministic**, where possible (seed/parameters explicit)
- **Interfaces are versioned** (same as everywhere)
- Outputs must be explainable and reproducible (audit-friendly)

---

## Security & privacy (take this seriously)

- **Never** log secrets or raw PII/PHI.
- Security-relevant changes need **explicit review**.
- Please **don’t** post vulnerabilities as public issues → see **SECURITY.md**.

---

## Quality bar (realistic, but high)

We’d rather merge less — and merge it right.

- Code is **readable**, not clever.
- Public APIs/contracts are **documented**.
- No “just this once” shortcuts we’ll regret in three months.
- Performance work: **measure** and attach numbers to the PR.

---

## Review culture (how it works here)

- Reviews are **about the work**. No ego, no gatekeeping.
- If you receive feedback: don’t “defend” — **understand**.
- If you review: be specific. “I don’t like it” isn’t feedback.

---

## First PR? Here’s how to land safely

1. Pick something small and well-scoped (docs, tests, small fix).
2. In the PR, briefly write what you were thinking.
3. If you’re unsure, ask early — one extra question beats one wrong assumption.

---

## PR checklist (copy/paste)

- [ ] Change is tenant-safe (tenant isolation checked)
- [ ] Module boundaries respected (no cross-import hacks)
- [ ] No silent breaking changes (contracts/APIs versioned)
- [ ] Tests added/updated (or justified: docs-only)
- [ ] Logging: no secrets, no raw PII/PHI
- [ ] PR description includes rationale + testing notes + tenant/privacy note
- [ ] ADR added if architecture/interfaces are affected

---

Thanks for taking engineering seriously. That’s exactly the kind of energy this project runs on.