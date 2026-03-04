# Branching Guide (Govaryn IO)

This repo follows a **simple, explicit branching model**. The goal is predictable releases and clean history — without ceremony.

## Branches

### `master` (delivery branch)
- **Purpose:** what gets delivered.
- **Rules:**
  - No direct commits.
  - Only merge via PR.
  - Must be green (CI + required checks).
  - Tagged releases are cut from `release/*` and merged into `master`.

### `develop` (integration / test branch)
- **Purpose:** shared integration branch for ongoing work.
- **Rules:**
  - Feature and fix branches target `develop`.
  - Keep it green as much as possible (at least before merging).

### `release/*` (release preparation)
- **Purpose:** final hardening before delivery: version bumps, release notes, tagging.
- **Rules:**
  - Branch off from `develop`.
  - Only allow release-related changes (no new features).
  - After tagging: merge into `master` **and** back-merge into `develop`.

---

## Working branches

### `feature/<short-description>`
- **Purpose:** new functionality or non-trivial refactoring.
- **Base:** `develop`
- **Merge target:** `develop`

### `fix/<short-description>`
- **Purpose:** bug fixes (including security fixes).
- **Base:** `develop` (or `master` if it’s a hotfix that must ship immediately)
- **Merge target:** usually `develop`; for hotfixes also `master` (see Hotfix section)

---

## Typical workflows

## 1) Start a feature

```bash
git checkout develop
git pull

git checkout -b feature/tenant-audit-stream
# work...
git status
git add -A
git commit -m "feat: add append-only tenant audit stream"

git push -u origin feature/tenant-audit-stream
# open PR: feature/... -> develop
```

After merge:

```bash
git checkout develop
git pull
git branch -d feature/tenant-audit-stream
```

---

## 2) Start a fix (normal)

```bash
git checkout develop
git pull

git checkout -b fix/audit-ordering
# work...
git add -A
git commit -m "fix: stabilize audit event ordering"
git push -u origin fix/audit-ordering
# open PR: fix/... -> develop
```

---

## 3) Cut a release

Create a release branch from `develop`:

```bash
git checkout develop
git pull

git checkout -b release/0.3.0
git push -u origin release/0.3.0
```

Do release preparation work (typical examples):

```bash
# bump version, update changelog/release notes
git add -A
git commit -m "chore: prepare release 0.3.0"
git push
```

Tag the release (annotated tags are preferred):

```bash
git tag -a v0.3.0 -m "Govaryn IO v0.3.0"
git push origin v0.3.0
```

Merge the release into `master`:

```bash
git checkout master
git pull

git merge --no-ff release/0.3.0
git push
```

Back-merge into `develop`:

```bash
git checkout develop
git pull

git merge --no-ff release/0.3.0
git push
```

Clean up:

```bash
git branch -d release/0.3.0
git push origin --delete release/0.3.0
```

---

## 4) Hotfix (must ship now)

If something is broken in production and cannot wait for the next release, branch from `master`:

```bash
git checkout master
git pull

git checkout -b fix/hotfix-tenant-leak
# work...
git add -A
git commit -m "fix: prevent tenant data leak in query filter"
git push -u origin fix/hotfix-tenant-leak
# open PR: fix/... -> master
```

After merge into `master`, back-port to `develop`:

```bash
git checkout develop
git pull

git merge --no-ff master
git push
```

---

## Naming conventions (keep it readable)

- Use kebab-case: `feature/capacity-planning-v2`
- Keep names short and specific.
- If the change is big, split it into smaller features.

---

## Merge policy (what reviewers expect)

- Use PRs for everything (no direct commits to `master`).
- Keep PRs small enough to review.
- Include tests (unless docs-only).
- Don’t break module boundaries or tenant isolation.
