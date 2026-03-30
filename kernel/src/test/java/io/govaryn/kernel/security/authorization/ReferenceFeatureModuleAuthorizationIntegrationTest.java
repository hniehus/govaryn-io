package io.govaryn.kernel.security.authorization;

import io.govaryn.kernel.api.KernelAuthorizationOperation;
import io.govaryn.kernel.api.KernelAuthorizationService;
import io.govaryn.kernel.api.KernelContext;
import io.govaryn.kernel.config.KernelEnvironment;
import io.govaryn.kernel.security.authorization.model.AuthorizationDecision;
import io.govaryn.kernel.security.authorization.model.AuthorizationDecisionResult;
import io.govaryn.kernel.security.authorization.model.AuthorizationSubject;
import io.govaryn.kernel.security.authorization.model.DecisionReasonCode;
import io.govaryn.modules.examples.ReferenceFeatureModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Reference Feature Module Authorization Integration")
class ReferenceFeatureModuleAuthorizationIntegrationTest {

    @Test
    @DisplayName("Protected capability delegates to kernel authorization service")
    void protectedCapabilityDelegatesToKernelAuthorizationService() {
        ReferenceFeatureModule module = new ReferenceFeatureModule(false);
        module.initialize(new KernelContext(
            "test-kernel",
            KernelEnvironment.DEV,
            "1.2.0",
            null,
            new PermitAuthorizationService()
        ));

        String result = module.restartProtectedModule(
            new AuthorizationSubject("user-1", java.util.List.of("ROLE_admin"), Map.of()),
            "reference-minimal",
            "dev"
        );

        assertEquals("module-restart-requested:reference-minimal", result);
    }

    @Test
    @DisplayName("Protected capability stops execution when authorization is denied")
    void protectedCapabilityStopsExecutionWhenAuthorizationIsDenied() {
        ReferenceFeatureModule module = new ReferenceFeatureModule(false);
        module.initialize(new KernelContext(
            "test-kernel",
            KernelEnvironment.DEV,
            "1.2.0",
            null,
            new DenyAuthorizationService()
        ));

        assertThrows(
            SecurityException.class,
            () -> module.restartProtectedModule(
                new AuthorizationSubject("user-2", java.util.List.of("ROLE_support"), Map.of()),
                "reference-minimal",
                "prod"
            )
        );
    }

    private static final class PermitAuthorizationService implements KernelAuthorizationService {
        @Override
        public AuthorizationDecision authorize(AuthorizationSubject subject, KernelAuthorizationOperation operation) {
            return new AuthorizationDecision(
                AuthorizationDecisionResult.PERMIT,
                DecisionReasonCode.PERMIT_RULE_MATCHED,
                "permit"
            );
        }

        @Override
        public AuthorizationDecision authorize(
            AuthorizationSubject subject,
            String action,
            String resourceType,
            String resourceId,
            Map<String, String> context
        ) {
            return authorize(subject, new KernelAuthorizationOperation(action, resourceType, resourceId, context));
        }
    }

    private static final class DenyAuthorizationService implements KernelAuthorizationService {
        @Override
        public AuthorizationDecision authorize(AuthorizationSubject subject, KernelAuthorizationOperation operation) {
            return new AuthorizationDecision(
                AuthorizationDecisionResult.DENY,
                DecisionReasonCode.DENY_RULE_MATCHED,
                "deny"
            );
        }

        @Override
        public AuthorizationDecision authorize(
            AuthorizationSubject subject,
            String action,
            String resourceType,
            String resourceId,
            Map<String, String> context
        ) {
            return authorize(subject, new KernelAuthorizationOperation(action, resourceType, resourceId, context));
        }
    }
}
