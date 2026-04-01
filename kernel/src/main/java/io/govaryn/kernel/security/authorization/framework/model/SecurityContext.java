package io.govaryn.kernel.security.authorization.framework.model;

import java.util.List;
import java.util.Map;

/**
 * Active request security context assembled by the kernel.
 */
public record SecurityContext(
    String userId,
    String tenantId,
    List<String> globalRoles,
    Map<String, String> claims,
    Map<String, String> authenticationMetadata
) {
    public SecurityContext {
        userId = AuthorizationFrameworkModelValidation.requireNonBlank(userId, "userId");
        tenantId = AuthorizationFrameworkModelValidation.trimToNull(tenantId);
        globalRoles = AuthorizationFrameworkModelValidation.sanitizeStringList(globalRoles, "globalRoles");
        claims = AuthorizationFrameworkModelValidation.sanitizeStringMap(claims, "claims");
        authenticationMetadata = AuthorizationFrameworkModelValidation.sanitizeStringMap(
            authenticationMetadata,
            "authenticationMetadata"
        );
    }
}
