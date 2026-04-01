package io.govaryn.kernel.security.authorization.framework;

import io.govaryn.kernel.backend.standard.KernelStandardRecordContract;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Startup guardrail preventing protected backend contracts from running without valid policy registration.
 */
@Component
public class KernelProtectedAuthorizationIntegrationGuardrail implements SmartInitializingSingleton {

    private static final Logger log = LoggerFactory.getLogger(KernelProtectedAuthorizationIntegrationGuardrail.class);

    private static final List<ProtectedResourceRequirement> REQUIRED_PROTECTED_RESOURCES = List.of(
        new ProtectedResourceRequirement(
            KernelStandardRecordContract.MODULE_ID,
            KernelStandardRecordContract.RESOURCE_TYPE,
            EnumSet.of(
                AuthorizationAction.READ,
                AuthorizationAction.LIST,
                AuthorizationAction.CREATE,
                AuthorizationAction.UPDATE,
                AuthorizationAction.DELETE
            )
        )
    );

    private final ResourcePolicyRegistry policyRegistry;

    public KernelProtectedAuthorizationIntegrationGuardrail(ResourcePolicyRegistry policyRegistry) {
        this.policyRegistry = policyRegistry;
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (ProtectedResourceRequirement requirement : REQUIRED_PROTECTED_RESOURCES) {
            validateRequirement(requirement);
        }
    }

    void validateRequirement(ProtectedResourceRequirement requirement) {
        ResourcePolicyRegistration registration = policyRegistry.resolve(
            requirement.moduleId(),
            requirement.resourceType()
        ).orElseThrow(() -> new ModuleSecurityRegistrationException(
            "Protected integration missing: moduleId='"
                + requirement.moduleId()
                + "', resourceType='"
                + requirement.resourceType()
                + "', requiredActions="
                + sortedActions(requirement.requiredActions())
        ));

        Set<AuthorizationAction> missingActions = EnumSet.copyOf(requirement.requiredActions());
        missingActions.removeAll(registration.supportedActions());

        if (!missingActions.isEmpty()) {
            throw new ModuleSecurityRegistrationException(
                "Protected integration invalid: moduleId='"
                    + requirement.moduleId()
                    + "', resourceType='"
                    + requirement.resourceType()
                    + "', missingActions="
                    + sortedActions(missingActions)
                    + ", registeredActions="
                    + sortedActions(registration.supportedActions())
            );
        }

        log.info(
            "event=protected_integration_validated moduleId={} resourceType={} requiredActions={} registeredActions={}",
            requirement.moduleId(),
            requirement.resourceType(),
            sortedActions(requirement.requiredActions()),
            sortedActions(registration.supportedActions())
        );
    }

    private static List<String> sortedActions(Set<AuthorizationAction> actions) {
        return actions.stream().map(Enum::name).sorted().toList();
    }

    record ProtectedResourceRequirement(
        String moduleId,
        String resourceType,
        Set<AuthorizationAction> requiredActions
    ) {
        ProtectedResourceRequirement {
            moduleId = requireNonBlank(moduleId, "moduleId");
            resourceType = requireNonBlank(resourceType, "resourceType");
            requiredActions = sanitizeRequiredActions(requiredActions);
        }

        private static String requireNonBlank(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                throw new ModuleSecurityRegistrationException(fieldName + " must not be blank");
            }
            return value.trim();
        }

        private static Set<AuthorizationAction> sanitizeRequiredActions(Set<AuthorizationAction> requiredActions) {
            if (requiredActions == null || requiredActions.isEmpty()) {
                throw new ModuleSecurityRegistrationException("requiredActions must not be empty");
            }
            EnumSet<AuthorizationAction> normalized = EnumSet.noneOf(AuthorizationAction.class);
            for (AuthorizationAction action : requiredActions) {
                if (action == null) {
                    throw new ModuleSecurityRegistrationException("requiredActions must not contain null entries");
                }
                normalized.add(action);
            }
            return Set.copyOf(normalized);
        }
    }
}
