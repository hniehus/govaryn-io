package io.govaryn.kernel.security.authorization.framework;

import io.govaryn.kernel.backend.standard.KernelStandardRecordContract;
import io.govaryn.kernel.health.security.ModuleStatusAuthorizationContract;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationAction;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationDecision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("KernelProtectedAuthorizationIntegrationGuardrail")
class KernelProtectedAuthorizationIntegrationGuardrailTest {

    @Test
    @DisplayName("Passes when required protected policy registration exists with full action set")
    void passesWhenRequiredProtectedPolicyRegistrationExistsWithFullActionSet() {
        InMemoryResourcePolicyRegistry registry = new InMemoryResourcePolicyRegistry();
        registry.register(new ResourcePolicyRegistration(
            KernelStandardRecordContract.MODULE_ID,
            KernelStandardRecordContract.RESOURCE_TYPE,
            EnumSet.allOf(AuthorizationAction.class),
            request -> AuthorizationDecision.allow("test-policy")
        ));
        registry.register(new ResourcePolicyRegistration(
            ModuleStatusAuthorizationContract.MODULE_ID,
            ModuleStatusAuthorizationContract.RESOURCE_TYPE,
            EnumSet.of(AuthorizationAction.READ),
            request -> AuthorizationDecision.allow("test-policy")
        ));

        KernelProtectedAuthorizationIntegrationGuardrail guardrail =
            new KernelProtectedAuthorizationIntegrationGuardrail(registry);

        assertDoesNotThrow(guardrail::afterSingletonsInstantiated);
    }

    @Test
    @DisplayName("Fails clearly when required protected policy registration is missing")
    void failsClearlyWhenRequiredProtectedPolicyRegistrationIsMissing() {
        InMemoryResourcePolicyRegistry registry = new InMemoryResourcePolicyRegistry();
        KernelProtectedAuthorizationIntegrationGuardrail guardrail =
            new KernelProtectedAuthorizationIntegrationGuardrail(registry);

        ModuleSecurityRegistrationException ex = assertThrows(
            ModuleSecurityRegistrationException.class,
            guardrail::afterSingletonsInstantiated
        );

        assertTrue(ex.getMessage().contains("Protected integration missing"));
        assertTrue(ex.getMessage().contains(KernelStandardRecordContract.MODULE_ID));
        assertTrue(ex.getMessage().contains(KernelStandardRecordContract.RESOURCE_TYPE));
    }

    @Test
    @DisplayName("Fails clearly when required protected actions are not fully registered")
    void failsClearlyWhenRequiredProtectedActionsAreNotFullyRegistered() {
        InMemoryResourcePolicyRegistry registry = new InMemoryResourcePolicyRegistry();
        registry.register(new ResourcePolicyRegistration(
            KernelStandardRecordContract.MODULE_ID,
            KernelStandardRecordContract.RESOURCE_TYPE,
            EnumSet.of(AuthorizationAction.READ, AuthorizationAction.LIST),
            request -> AuthorizationDecision.allow("test-policy")
        ));

        KernelProtectedAuthorizationIntegrationGuardrail guardrail =
            new KernelProtectedAuthorizationIntegrationGuardrail(registry);

        ModuleSecurityRegistrationException ex = assertThrows(
            ModuleSecurityRegistrationException.class,
            guardrail::afterSingletonsInstantiated
        );

        assertTrue(ex.getMessage().contains("Protected integration invalid"));
        assertTrue(ex.getMessage().contains("missingActions"));
        assertTrue(ex.getMessage().contains("CREATE"));
        assertTrue(ex.getMessage().contains("UPDATE"));
        assertTrue(ex.getMessage().contains("DELETE"));
    }
}
