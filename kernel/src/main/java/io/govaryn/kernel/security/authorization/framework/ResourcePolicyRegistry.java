package io.govaryn.kernel.security.authorization.framework;

import java.util.List;
import java.util.Optional;

/**
 * Kernel registry for protected resource policy registrations.
 */
public interface ResourcePolicyRegistry {

    void register(ResourcePolicyRegistration registration);

    Optional<ResourcePolicyRegistration> resolve(String moduleId, String resourceType);

    List<ResourcePolicyRegistration> findByModuleId(String moduleId);
}
