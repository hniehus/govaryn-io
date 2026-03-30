package io.govaryn.kernel.security.authorization;

import io.govaryn.kernel.api.KernelAuthorizationOperation;
import io.govaryn.kernel.security.authorization.model.AuthorizationDecision;
import io.govaryn.kernel.security.authorization.model.AuthorizationDecisionResult;
import io.govaryn.kernel.security.authorization.model.AuthorizationRequest;
import io.govaryn.kernel.security.authorization.model.AuthorizationSubject;
import io.govaryn.kernel.security.authorization.model.DecisionReasonCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("KernelAuthorizationServiceAdapter Tests")
class KernelAuthorizationServiceAdapterTest {

    @Test
    @DisplayName("Missing required raw input should be denied safely")
    void missingRequiredRawInputShouldBeDeniedSafely() {
        KernelAuthorizationServiceAdapter adapter = new KernelAuthorizationServiceAdapter(new AllowAllAuthorizationService());
        AuthorizationSubject subject = new AuthorizationSubject("subject-1", java.util.List.of("ROLE_admin"), Map.of());

        AuthorizationDecision missingAction = adapter.authorize(subject, " ", "module", null, Map.of());
        AuthorizationDecision missingResourceType = adapter.authorize(subject, "read", " ", null, Map.of());
        AuthorizationDecision nullSubject = adapter.authorize(null, "read", "module", null, Map.of());

        assertEquals(AuthorizationDecisionResult.DENY, missingAction.result());
        assertEquals(DecisionReasonCode.INVALID_REQUEST, missingAction.reasonCode());
        assertEquals(AuthorizationDecisionResult.DENY, missingResourceType.result());
        assertEquals(DecisionReasonCode.INVALID_REQUEST, missingResourceType.reasonCode());
        assertEquals(AuthorizationDecisionResult.DENY, nullSubject.result());
        assertEquals(DecisionReasonCode.INVALID_REQUEST, nullSubject.reasonCode());
    }

    @Test
    @DisplayName("Unsupported context key should be denied safely")
    void unsupportedContextKeyShouldBeDeniedSafely() {
        KernelAuthorizationServiceAdapter adapter = new KernelAuthorizationServiceAdapter(new AllowAllAuthorizationService());
        AuthorizationSubject subject = new AuthorizationSubject("subject-1", java.util.List.of("ROLE_admin"), Map.of());

        AuthorizationDecision decision = adapter.authorize(
            subject,
            "read",
            "module",
            null,
            Map.of("tenant", "t1")
        );

        assertEquals(AuthorizationDecisionResult.DENY, decision.result());
        assertEquals(DecisionReasonCode.INVALID_REQUEST, decision.reasonCode());
    }

    @Test
    @DisplayName("Validated operation should delegate to PDP service")
    void validatedOperationShouldDelegateToPdpService() {
        KernelAuthorizationServiceAdapter adapter = new KernelAuthorizationServiceAdapter(new AllowAllAuthorizationService());
        AuthorizationSubject subject = new AuthorizationSubject("subject-1", java.util.List.of("ROLE_admin"), Map.of());
        KernelAuthorizationOperation operation = new KernelAuthorizationOperation("read", "module", null, Map.of());

        AuthorizationDecision decision = adapter.authorize(subject, operation);

        assertEquals(AuthorizationDecisionResult.PERMIT, decision.result());
    }

    @Test
    @DisplayName("Null operation should be denied safely")
    void nullOperationShouldBeDeniedSafely() {
        KernelAuthorizationServiceAdapter adapter = new KernelAuthorizationServiceAdapter(new AllowAllAuthorizationService());
        AuthorizationSubject subject = new AuthorizationSubject("subject-1", java.util.List.of("ROLE_admin"), Map.of());

        AuthorizationDecision decision = adapter.authorize(subject, (KernelAuthorizationOperation) null);

        assertEquals(AuthorizationDecisionResult.DENY, decision.result());
        assertEquals(DecisionReasonCode.INVALID_REQUEST, decision.reasonCode());
    }

    private static final class AllowAllAuthorizationService implements AuthorizationService {
        @Override
        public AuthorizationDecision authorize(AuthorizationRequest request) {
            return new AuthorizationDecision(
                AuthorizationDecisionResult.PERMIT,
                DecisionReasonCode.PERMIT_RULE_MATCHED,
                "stub-permit"
            );
        }
    }
}
