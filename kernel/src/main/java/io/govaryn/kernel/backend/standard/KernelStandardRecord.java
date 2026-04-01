package io.govaryn.kernel.backend.standard;

/**
 * Minimal record model for the kernel-managed standard CRUD path.
 */
public record KernelStandardRecord(
    String id,
    String value
) {
}
