package io.govaryn.kernel.api;

import java.util.Map;
import java.util.Optional;

public final class KernelAuthorizationOperations {

    private KernelAuthorizationOperations() {
    }

    public static KernelAuthorizationOperation of(String action, String resourceType) {
        return new KernelAuthorizationOperation(action, resourceType, null, Map.of());
    }

    public static KernelAuthorizationOperation of(
        String action,
        String resourceType,
        String resourceId,
        Map<String, String> context
    ) {
        return new KernelAuthorizationOperation(action, resourceType, resourceId, context);
    }

    public static Optional<KernelAuthorizationOperation> tryCreate(
        String action,
        String resourceType,
        String resourceId,
        Map<String, String> context
    ) {
        try {
            return Optional.of(new KernelAuthorizationOperation(action, resourceType, resourceId, context));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
