# Contributing to Govaryn

Govaryn is compliance-targeted enterprise software. That means discipline.

## Hard rules
- **No tenant leaks. Ever.**
- **No cross-module “quick imports”.**
- **No silent breaking changes** in APIs/contracts.
- Tests are mandatory unless docs-only.

## Dev workflow
- Branches: `feat/*`, `fix/*`, `chore/*`
- Conventional commits: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`
- PR must include: rationale, tests, tenant/privacy impact note, ADR if architectural.

## Rust compute services
- Compute-only. No authoritative business state.
- Deterministic outputs where possible; include seed/params.
- Interfaces must be versioned.

## Security
- Never log secrets or raw PII/PHI.
- Security-relevant changes need explicit review.
- Vulnerabilities via SECURITY.md (no public issues).
