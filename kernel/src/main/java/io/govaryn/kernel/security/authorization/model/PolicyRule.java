package io.govaryn.kernel.security.authorization.model;

import java.util.List;

public record PolicyRule(
    String ruleId,
    PolicyEffect effect,
    List<String> roles,
    List<String> actions,
    List<String> resourceTypes,
    List<String> resourceIds
) {
    public PolicyRule {
        ruleId = AuthorizationModelValidation.requireNonBlank(ruleId, "ruleId");
        effect = AuthorizationModelValidation.requireNonNull(effect, "effect");
        roles = AuthorizationModelValidation.sanitizeStringList(roles, "roles");
        actions = AuthorizationModelValidation.sanitizeStringList(actions, "actions");
        resourceTypes = AuthorizationModelValidation.sanitizeStringList(resourceTypes, "resourceTypes");
        resourceIds = AuthorizationModelValidation.sanitizeStringList(resourceIds, "resourceIds");

        if (actions.isEmpty()) {
            throw new IllegalArgumentException("actions must not be empty");
        }
        if (resourceTypes.isEmpty()) {
            throw new IllegalArgumentException("resourceTypes must not be empty");
        }
    }
}
