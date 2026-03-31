package io.govaryn.kernel.security.authorization.framework;

import io.govaryn.kernel.security.authorization.framework.model.AuthorizationAction;

import java.util.Set;

/**
 * Registry abstraction used by module contributors to declare authorization surface.
 */
public interface ModuleSecurityRegistry {

    void registerModuleActions(String moduleId, Set<AuthorizationAction> actions);

    void registerResourceActions(String moduleId, String resourceType, Set<AuthorizationAction> actions);
}
