package io.govaryn.kernel.security.authorization.framework.model;

import java.util.Map;

/**
 * Canonical kernel authorization input for module and record access checks.
 */
public record AuthorizationRequest(
    SecurityContext securityContext,
    String moduleId,
    AuthorizationAction action,
    String resourceType,
    String resourceId,
    Map<String, String> attributes
) {
    public AuthorizationRequest {
        securityContext = AuthorizationFrameworkModelValidation.requireNonNull(securityContext, "securityContext");
        moduleId = AuthorizationFrameworkModelValidation.requireNonBlank(moduleId, "moduleId");
        action = AuthorizationFrameworkModelValidation.requireNonNull(action, "action");
        resourceType = AuthorizationFrameworkModelValidation.requireNonBlank(resourceType, "resourceType");
        resourceId = AuthorizationFrameworkModelValidation.trimToNull(resourceId);
        attributes = AuthorizationFrameworkModelValidation.sanitizeStringMap(attributes, "attributes");
    }
}
