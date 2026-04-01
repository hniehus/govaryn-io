package io.govaryn.kernel.security.authorization.framework;

import io.govaryn.kernel.security.authorization.framework.model.AuthorizationAction;

import java.util.EnumSet;
import java.util.Set;

/**
 * Kernel registration entry for a protected resource policy.
 */
public record ResourcePolicyRegistration(
    String moduleId,
    String resourceType,
    Set<AuthorizationAction> supportedActions,
    ResourcePolicyEvaluator evaluator
) {
    public ResourcePolicyRegistration {
        moduleId = requireNonBlank(moduleId, "moduleId");
        resourceType = requireNonBlank(resourceType, "resourceType");
        evaluator = requireNonNull(evaluator, "evaluator");
        supportedActions = sanitizeActions(supportedActions);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ModuleSecurityRegistrationException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new ModuleSecurityRegistrationException(fieldName + " must not be null");
        }
        return value;
    }

    private static Set<AuthorizationAction> sanitizeActions(Set<AuthorizationAction> actions) {
        if (actions == null || actions.isEmpty()) {
            throw new ModuleSecurityRegistrationException("supportedActions must not be empty");
        }
        EnumSet<AuthorizationAction> normalized = EnumSet.noneOf(AuthorizationAction.class);
        for (AuthorizationAction action : actions) {
            if (action == null) {
                throw new ModuleSecurityRegistrationException("supportedActions must not contain null entries");
            }
            normalized.add(action);
        }
        return Set.copyOf(normalized);
    }
}
