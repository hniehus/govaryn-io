package io.govaryn.kernel.security;

public class KernelTenantResolutionException extends RuntimeException {

    private final KernelTenantResolutionFailure failure;

    public KernelTenantResolutionException(KernelTenantResolutionFailure failure, String message) {
        super(message);
        if (failure == null) {
            throw new IllegalArgumentException("failure must not be null");
        }
        this.failure = failure;
    }

    public KernelTenantResolutionFailure failure() {
        return failure;
    }
}
