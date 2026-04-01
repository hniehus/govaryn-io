package io.govaryn.kernel.security.authorization.framework;

import io.govaryn.kernel.security.authorization.framework.model.AuthorizationAction;

import java.util.Set;

/**
 * Registry abstraction exposed to module contributors for protected resource declarations.
 */
public interface ModuleSecurityRegistry {

    void registerResourcePolicy(
        String resourceType,
        Set<AuthorizationAction> supportedActions,
        ResourcePolicyEvaluator evaluator
    );
}
