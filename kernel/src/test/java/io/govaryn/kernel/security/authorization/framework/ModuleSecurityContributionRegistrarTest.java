package io.govaryn.kernel.security.authorization.framework;

import io.govaryn.kernel.security.authorization.framework.model.AuthorizationAction;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationDecision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ModuleSecurityContributionRegistrar")
class ModuleSecurityContributionRegistrarTest {

    @Test
    @DisplayName("Registers contributor policies into kernel registry")
    void registersContributorPoliciesIntoKernelRegistry() {
        InMemoryResourcePolicyRegistry policyRegistry = new InMemoryResourcePolicyRegistry();
        ModuleSecurityContributor contributor = new ModuleSecurityContributor() {
            @Override
            public String moduleId() {
                return "orders";
            }

            @Override
            public void contribute(ModuleSecurityRegistry registry) {
                registry.registerResourcePolicy(
                    "order",
                    Set.of(AuthorizationAction.READ, AuthorizationAction.UPDATE),
                    request -> AuthorizationDecision.allow("orders-policy")
                );
            }
        };

        ModuleSecurityContributionRegistrar registrar = new ModuleSecurityContributionRegistrar(
            List.of(contributor),
            policyRegistry
        );

        registrar.registerContributions();

        ResourcePolicyRegistration registration = policyRegistry.resolve("orders", "order").orElseThrow();
        assertEquals("orders", registration.moduleId());
        assertEquals(Set.of(AuthorizationAction.READ, AuthorizationAction.UPDATE), registration.supportedActions());
        assertEquals(1, policyRegistry.findByModuleId("orders").size());
    }

    @Test
    @DisplayName("Rejects contributor with blank module id")
    void rejectsContributorWithBlankModuleId() {
        InMemoryResourcePolicyRegistry policyRegistry = new InMemoryResourcePolicyRegistry();
        ModuleSecurityContributor contributor = new ModuleSecurityContributor() {
            @Override
            public String moduleId() {
                return "   ";
            }

            @Override
            public void contribute(ModuleSecurityRegistry registry) {
                registry.registerResourcePolicy(
                    "order",
                    Set.of(AuthorizationAction.READ),
                    request -> AuthorizationDecision.allow("orders-policy")
                );
            }
        };

        ModuleSecurityContributionRegistrar registrar = new ModuleSecurityContributionRegistrar(
            List.of(contributor),
            policyRegistry
        );

        ModuleSecurityRegistrationException ex = assertThrows(
            ModuleSecurityRegistrationException.class,
            registrar::registerContributions
        );
        assertTrue(ex.getMessage().contains("moduleId"));
    }

    @Test
    @DisplayName("Rejects contradictory resource declarations from a contributor")
    void rejectsContradictoryResourceDeclarationsFromContributor() {
        InMemoryResourcePolicyRegistry policyRegistry = new InMemoryResourcePolicyRegistry();
        ModuleSecurityContributor contributor = new ModuleSecurityContributor() {
            @Override
            public String moduleId() {
                return "orders";
            }

            @Override
            public void contribute(ModuleSecurityRegistry registry) {
                registry.registerResourcePolicy(
                    "order",
                    Set.of(AuthorizationAction.READ),
                    request -> AuthorizationDecision.allow("orders-policy")
                );
                registry.registerResourcePolicy(
                    "order",
                    Set.of(AuthorizationAction.UPDATE),
                    request -> AuthorizationDecision.allow("orders-policy-v2")
                );
            }
        };

        ModuleSecurityContributionRegistrar registrar = new ModuleSecurityContributionRegistrar(
            List.of(contributor),
            policyRegistry
        );

        ModuleSecurityRegistrationException ex = assertThrows(
            ModuleSecurityRegistrationException.class,
            registrar::registerContributions
        );
        assertTrue(ex.getMessage().contains("Conflicting resource policy registration"));
    }
}
