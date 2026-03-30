package io.govaryn.kernel.security.authorization;

import io.govaryn.kernel.security.authorization.model.AuthorizationRequest;
import io.govaryn.kernel.security.authorization.model.AuthorizationSubject;
import io.govaryn.kernel.security.authorization.model.PolicyEffect;
import io.govaryn.kernel.security.authorization.policy.ActiveAuthorizationPolicySnapshot;
import io.govaryn.kernel.security.authorization.policy.ActiveAuthorizationPolicyStore;
import io.govaryn.kernel.security.authorization.policy.InMemoryActiveAuthorizationPolicyStore;
import io.govaryn.kernel.security.authorization.policy.PolicyContextAttributeCondition;
import io.govaryn.kernel.security.authorization.policy.PolicyContextMatchCriteria;
import io.govaryn.kernel.security.authorization.policy.PolicyRuleDocument;
import io.govaryn.kernel.security.authorization.policy.PolicySetDocument;
import io.govaryn.kernel.security.authorization.policy.PolicySubjectMatchCriteria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
@DisplayName("KernelPolicyDecision Logging")
class KernelPolicyDecisionLoggingTest {

    @Test
    @DisplayName("Permit decision should log expected fields with sanitized references")
    void permitDecisionShouldLogExpectedFieldsWithSanitizedReferences(CapturedOutput output) {
        KernelPolicyDecisionPoint pdp = pdpWithPolicy(
            new PolicySetDocument(
                "policy-v1",
                List.of(rule("permit-read", PolicyEffect.PERMIT, List.of("ROLE_admin"), List.of("read"), List.of("module"), List.of()))
            )
        );

        MDC.put("traceId", "trace-123");
        try {
            pdp.authorize(request("subject-alice@example.com", "read", "module", "module-secret-001"));
        } finally {
            MDC.clear();
        }

        String logs = output.getOut() + output.getErr();
        assertThat(logs)
            .contains("event=authorization_decision")
            .contains("result=PERMIT")
            .contains("action=read")
            .contains("resourceType=module")
            .contains("context={environment=prod}")
            .contains("policyRevision=policy-v1")
            .contains("reasonCode=PERMIT_RULE_MATCHED")
            .contains("matchedRuleId=permit-read")
            .contains("requestRef=trace-123")
            .contains("subjectRef=sha256:")
            .contains("resourceIdRef=sha256:")
            .doesNotContain("subject-alice@example.com")
            .doesNotContain("module-secret-001");
    }

    @Test
    @DisplayName("Deny decision should log consistently")
    void denyDecisionShouldLogConsistently(CapturedOutput output) {
        KernelPolicyDecisionPoint pdp = pdpWithPolicy(
            new PolicySetDocument(
                "policy-v2",
                List.of(rule("deny-stop", PolicyEffect.DENY, List.of("ROLE_admin"), List.of("stop"), List.of("module"), List.of()))
            )
        );

        pdp.authorize(request("subject-777", "stop", "module", null));

        String logs = output.getOut() + output.getErr();
        assertThat(logs)
            .contains("event=authorization_decision")
            .contains("result=DENY")
            .contains("reasonCode=DENY_RULE_MATCHED")
            .contains("matchedRuleId=deny-stop")
            .contains("policyRevision=policy-v2")
            .contains("requestRef=none")
            .contains("errorType=none");
    }

    @Test
    @DisplayName("Evaluation error should log deny decision with error metadata")
    void evaluationErrorShouldLogDenyDecisionWithErrorMetadata(CapturedOutput output) {
        ActiveAuthorizationPolicyStore failingStore = new ActiveAuthorizationPolicyStore() {
            @Override
            public Optional<ActiveAuthorizationPolicySnapshot> getActivePolicy() {
                throw new IllegalStateException("backend failure");
            }

            @Override
            public ActiveAuthorizationPolicySnapshot activate(PolicySetDocument policySet) {
                throw new UnsupportedOperationException("not used");
            }
        };
        KernelPolicyDecisionPoint pdp = new KernelPolicyDecisionPoint(failingStore, new AuthorizationDecisionLogger());

        pdp.authorize(request("subject-888", "read", "module", "module-1"));

        String logs = output.getOut() + output.getErr();
        assertThat(logs)
            .contains("event=authorization_decision")
            .contains("result=DENY")
            .contains("reasonCode=EVALUATION_ERROR")
            .contains("policyRevision=none")
            .contains("errorType=IllegalStateException")
            .doesNotContain("subject-888")
            .doesNotContain("module-1");
    }

    @Test
    @DisplayName("Sensitive context values should be redacted in logs")
    void sensitiveContextValuesShouldBeRedactedInLogs(CapturedOutput output) {
        KernelPolicyDecisionPoint pdp = pdpWithPolicy(
            new PolicySetDocument(
                "policy-v3",
                List.of(rule("permit-read", PolicyEffect.PERMIT, List.of("ROLE_admin"), List.of("read"), List.of("module"), List.of()))
            )
        );

        AuthorizationRequest request = new AuthorizationRequest(
            new AuthorizationSubject("subject-999", List.of("ROLE_admin"), Map.of()),
            "read",
            "module",
            "module-22",
            Map.of(
                "environment", "prod",
                "authorization", "Bearer secret-token-value",
                "sessionToken", "eyJhbGciOiJIUzI1NiJ9.payload.signature"
            )
        );

        pdp.authorize(request);

        String logs = output.getOut() + output.getErr();
        assertThat(logs)
            .contains("event=authorization_decision")
            .contains("authorization=[redacted]")
            .contains("sessionToken=[redacted]")
            .doesNotContain("secret-token-value")
            .doesNotContain("eyJhbGciOiJIUzI1NiJ9.payload.signature");
    }

    private static KernelPolicyDecisionPoint pdpWithPolicy(PolicySetDocument policySetDocument) {
        InMemoryActiveAuthorizationPolicyStore store = new InMemoryActiveAuthorizationPolicyStore();
        store.activate(policySetDocument);
        return new KernelPolicyDecisionPoint(store, new AuthorizationDecisionLogger());
    }

    private static PolicyRuleDocument rule(
        String id,
        PolicyEffect effect,
        List<String> roles,
        List<String> actions,
        List<String> resourceTypes,
        List<String> resourceIds
    ) {
        return new PolicyRuleDocument(
            id,
            effect,
            new PolicySubjectMatchCriteria(roles),
            actions,
            resourceTypes,
            resourceIds,
            new PolicyContextMatchCriteria(Map.of("environment", new PolicyContextAttributeCondition(List.of("prod"))))
        );
    }

    private static AuthorizationRequest request(
        String subjectId,
        String action,
        String resourceType,
        String resourceId
    ) {
        return new AuthorizationRequest(
            new AuthorizationSubject(subjectId, List.of("ROLE_admin"), Map.of()),
            action,
            resourceType,
            resourceId,
            Map.of("environment", "prod")
        );
    }
}
