package io.govaryn.kernel.security.authorization.framework;

import io.govaryn.kernel.security.authorization.framework.model.AuthorizationAction;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Registers module security contributors into the kernel resource policy registry.
 */
@Component
public class ModuleSecurityContributionRegistrar {

    private final List<ModuleSecurityContributor> contributors;
    private final ResourcePolicyRegistry resourcePolicyRegistry;

    public ModuleSecurityContributionRegistrar(
        List<ModuleSecurityContributor> contributors,
        ResourcePolicyRegistry resourcePolicyRegistry
    ) {
        this.contributors = contributors == null ? List.of() : List.copyOf(contributors);
        this.resourcePolicyRegistry = resourcePolicyRegistry;
    }

    @PostConstruct
    void registerContributions() {
        for (ModuleSecurityContributor contributor : contributors) {
            registerContributor(contributor);
        }
    }

    void registerContributor(ModuleSecurityContributor contributor) {
        if (contributor == null) {
            throw new ModuleSecurityRegistrationException("ModuleSecurityContributor must not be null");
        }

        String moduleId = requireNonBlank(contributor.moduleId(), "moduleId");
        ModuleSecurityRegistry scopedRegistry = new ScopedModuleSecurityRegistry(moduleId, resourcePolicyRegistry);

        try {
            contributor.contribute(scopedRegistry);
        } catch (ModuleSecurityRegistrationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ModuleSecurityRegistrationException(
                "Security contribution failed for moduleId='" + moduleId + "'",
                ex
            );
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ModuleSecurityRegistrationException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static final class ScopedModuleSecurityRegistry implements ModuleSecurityRegistry {
        private final String moduleId;
        private final ResourcePolicyRegistry registry;

        private ScopedModuleSecurityRegistry(String moduleId, ResourcePolicyRegistry registry) {
            this.moduleId = moduleId;
            this.registry = registry;
        }

        @Override
        public void registerResourcePolicy(
            String resourceType,
            Set<AuthorizationAction> supportedActions,
            ResourcePolicyEvaluator evaluator
        ) {
            registry.register(new ResourcePolicyRegistration(moduleId, resourceType, supportedActions, evaluator));
        }
    }
}
