package io.govaryn.kernel.security.authorization.framework;

import io.govaryn.kernel.security.authorization.framework.model.AuthorizationAction;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationDecision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("InMemoryResourcePolicyRegistry")
class InMemoryResourcePolicyRegistryTest {

    @Test
    @DisplayName("Registers and resolves resource policy")
    void registersAndResolvesResourcePolicy() {
        InMemoryResourcePolicyRegistry registry = new InMemoryResourcePolicyRegistry();
        ResourcePolicyEvaluator evaluator = request -> AuthorizationDecision.allow("orders-policy");

        registry.register(new ResourcePolicyRegistration(
            "orders",
            "order",
            Set.of(AuthorizationAction.READ, AuthorizationAction.UPDATE),
            evaluator
        ));

        ResourcePolicyRegistration policy = registry.resolve("orders", "order").orElseThrow();
        assertEquals("orders", policy.moduleId());
        assertEquals("order", policy.resourceType());
        assertEquals(Set.of(AuthorizationAction.READ, AuthorizationAction.UPDATE), policy.supportedActions());
        assertSame(evaluator, policy.evaluator());
        assertEquals(1, registry.findByModuleId("orders").size());
    }

    @Test
    @DisplayName("Rejects duplicate resource policy registration for the same module and resource")
    void rejectsDuplicateResourcePolicyRegistration() {
        InMemoryResourcePolicyRegistry registry = new InMemoryResourcePolicyRegistry();
        ResourcePolicyRegistration first = new ResourcePolicyRegistration(
            "orders",
            "order",
            Set.of(AuthorizationAction.READ),
            request -> AuthorizationDecision.allow("first")
        );
        ResourcePolicyRegistration duplicate = new ResourcePolicyRegistration(
            "orders",
            "order",
            Set.of(AuthorizationAction.UPDATE),
            request -> AuthorizationDecision.allow("duplicate")
        );

        registry.register(first);

        ModuleSecurityRegistrationException ex = assertThrows(
            ModuleSecurityRegistrationException.class,
            () -> registry.register(duplicate)
        );
        assertTrue(ex.getMessage().contains("Conflicting resource policy registration"));
        assertTrue(ex.getMessage().contains("moduleId='orders'"));
        assertTrue(ex.getMessage().contains("resourceType='order'"));
    }

    @Test
    @DisplayName("Rejects invalid registration payload")
    void rejectsInvalidRegistrationPayload() {
        ModuleSecurityRegistrationException emptyActions = assertThrows(
            ModuleSecurityRegistrationException.class,
            () -> new ResourcePolicyRegistration(
                "orders",
                "order",
                Set.of(),
                request -> AuthorizationDecision.allow("orders")
            )
        );
        assertTrue(emptyActions.getMessage().contains("supportedActions"));

        ModuleSecurityRegistrationException blankResourceType = assertThrows(
            ModuleSecurityRegistrationException.class,
            () -> new ResourcePolicyRegistration(
                "orders",
                "   ",
                Set.of(AuthorizationAction.READ),
                request -> AuthorizationDecision.allow("orders")
            )
        );
        assertTrue(blankResourceType.getMessage().contains("resourceType"));
    }
}
