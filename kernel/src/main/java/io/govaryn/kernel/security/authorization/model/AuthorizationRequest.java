package io.govaryn.kernel.security.authorization.model;

import java.util.Map;

public record AuthorizationRequest(
    AuthorizationSubject subject,
    String action,
    String resourceType,
    String resourceId,
    Map<String, String> context
) {
    public AuthorizationRequest {
        subject = AuthorizationModelValidation.requireNonNull(subject, "subject");
        action = AuthorizationModelValidation.requireNonBlank(action, "action");
        resourceType = AuthorizationModelValidation.requireNonBlank(resourceType, "resourceType");
        resourceId = resourceId == null ? null : AuthorizationModelValidation.requireNonBlank(resourceId, "resourceId");
        context = AuthorizationModelValidation.sanitizeStringMap(context, "context");
    }
}
