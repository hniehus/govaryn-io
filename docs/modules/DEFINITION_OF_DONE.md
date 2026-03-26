# Definition of Done for Govaryn Modules

This definition applies to both kernel and module teams when adding or changing modules.

## Contract and Compatibility

- Module metadata conforms to the current module contract.
- `requiredKernelApiVersion` is compatible with the running kernel.
- Contract validation passes with zero errors.

## Architecture Rules

- No usage of `internal` kernel APIs.
- Capability dependencies are explicit and acyclic.
- No duplicate `moduleId` across modules.

## Tests

- Unit tests cover module metadata and behavior.
- Integration tests cover discovery, validation, registration, and initialization.
- Failure handling is tested for at least one negative path.

## Logging and Error Handling

- Initialization and runtime errors are logged with module context.
- Failure policy is declared and verified in tests.
- Status transitions are deterministic and reproducible.

## Documentation

- Module description and capabilities are documented.
- Any new or changed kernel API is documented with stability class.
- Operational notes are updated for new failure modes.

## Release Readiness

- CI gates pass locally and in CI.
- Version numbers follow semantic versioning.
- Deprecations and breaking changes follow governance rules.
