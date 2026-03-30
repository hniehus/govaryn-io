package io.govaryn.kernel.security.authorization.model;

import java.util.List;
import java.util.Map;

public record AuthorizationSubject(
    String subjectId,
    List<String> roles,
    Map<String, String> attributes
) {
    public AuthorizationSubject {
        subjectId = AuthorizationModelValidation.requireNonBlank(subjectId, "subjectId");
        roles = AuthorizationModelValidation.sanitizeStringList(roles, "roles");
        attributes = AuthorizationModelValidation.sanitizeStringMap(attributes, "attributes");
    }
}
