package io.govaryn.kernel.security.authorization;

import io.govaryn.kernel.security.authorization.model.AuthorizationDecision;
import io.govaryn.kernel.security.authorization.model.AuthorizationDecisionResult;
import io.govaryn.kernel.security.authorization.model.AuthorizationRequest;
import io.govaryn.kernel.security.authorization.model.AuthorizationSubject;
import io.govaryn.kernel.security.authorization.model.DecisionReasonCode;
import io.govaryn.kernel.security.authorization.model.PolicyEffect;
import io.govaryn.kernel.security.authorization.policy.ActiveAuthorizationPolicyStore;
import io.govaryn.kernel.security.authorization.policy.InMemoryActiveAuthorizationPolicyStore;
import io.govaryn.kernel.security.authorization.policy.PolicyContextAttributeCondition;
import io.govaryn.kernel.security.authorization.policy.PolicyContextMatchCriteria;
import io.govaryn.kernel.security.authorization.policy.PolicyRuleDocument;
import io.govaryn.kernel.security.authorization.policy.PolicySetDocument;
import io.govaryn.kernel.security.authorization.policy.PolicySubjectMatchCriteria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("KernelPolicyDecisionPoint Tests")
class KernelPolicyDecisionPointTest {

    @Test
    @DisplayName("Should permit when permit rule matches")
    void shouldPermitWhenPermitRuleMatches() {
        KernelPolicyDecisionPoint pdp = pdpWithPolicy(
            policyOf(
                rule("permit-read", PolicyEffect.PERMIT, List.of("ROLE_admin"), List.of("read"), List.of("module"), List.of(), Map.of())
            )
        );

        AuthorizationDecision decision = pdp.authorize(request("ROLE_admin", "read", "module", null, Map.of()));

        assertEquals(AuthorizationDecisionResult.PERMIT, decision.result());
        assertEquals(DecisionReasonCode.PERMIT_RULE_MATCHED, decision.reasonCode());
        assertEquals("permit-read", decision.matchedRuleId());
    }

    @Test
    @DisplayName("Should deny when deny rule matches")
    void shouldDenyWhenDenyRuleMatches() {
        KernelPolicyDecisionPoint pdp = pdpWithPolicy(
            policyOf(
                rule("deny-stop", PolicyEffect.DENY, List.of("ROLE_operator"), List.of("stop"), List.of("module"), List.of(), Map.of())
            )
        );

        AuthorizationDecision decision = pdp.authorize(request("ROLE_operator", "stop", "module", null, Map.of()));

        assertEquals(AuthorizationDecisionResult.DENY, decision.result());
        assertEquals(DecisionReasonCode.DENY_RULE_MATCHED, decision.reasonCode());
        assertEquals("deny-stop", decision.matchedRuleId());
    }

    @Test
    @DisplayName("Should deny when both permit and deny rules match")
    void shouldDenyWhenBothPermitAndDenyRulesMatch() {
        KernelPolicyDecisionPoint pdp = pdpWithPolicy(
            policyOf(
                rule("permit-read", PolicyEffect.PERMIT, List.of("ROLE_support"), List.of("read"), List.of("module"), List.of(), Map.of()),
                rule("deny-read", PolicyEffect.DENY, List.of("ROLE_support"), List.of("read"), List.of("module"), List.of(), Map.of())
            )
        );

        AuthorizationDecision decision = pdp.authorize(request("ROLE_support", "read", "module", null, Map.of()));

        assertEquals(AuthorizationDecisionResult.DENY, decision.result());
        assertEquals(DecisionReasonCode.DENY_RULE_MATCHED, decision.reasonCode());
        assertEquals("deny-read", decision.matchedRuleId());
    }

    @Test
    @DisplayName("Should deny when no rule matches")
    void shouldDenyWhenNoRuleMatches() {
        KernelPolicyDecisionPoint pdp = pdpWithPolicy(
            policyOf(
                rule("permit-read", PolicyEffect.PERMIT, List.of("ROLE_admin"), List.of("read"), List.of("module"), List.of(), Map.of())
            )
        );

        AuthorizationDecision decision = pdp.authorize(request("ROLE_guest", "read", "module", null, Map.of()));

        assertEquals(AuthorizationDecisionResult.DENY, decision.result());
        assertEquals(DecisionReasonCode.NO_MATCHING_RULE, decision.reasonCode());
        assertEquals(null, decision.matchedRuleId());
    }

    @Test
    @DisplayName("Should support resourceId-based matching")
    void shouldSupportResourceIdBasedMatching() {
        KernelPolicyDecisionPoint pdp = pdpWithPolicy(
            policyOf(
                rule(
                    "permit-restart-reference-minimal",
                    PolicyEffect.PERMIT,
                    List.of("ROLE_admin"),
                    List.of("restart"),
                    List.of("module"),
                    List.of("reference-minimal"),
                    Map.of()
                )
            )
        );

        AuthorizationDecision matched = pdp.authorize(
            request("ROLE_admin", "restart", "module", "reference-minimal", Map.of())
        );
        AuthorizationDecision notMatched = pdp.authorize(
            request("ROLE_admin", "restart", "module", "another-module", Map.of())
        );

        assertEquals(AuthorizationDecisionResult.PERMIT, matched.result());
        assertEquals(AuthorizationDecisionResult.DENY, notMatched.result());
    }

    @Test
    @DisplayName("Should support controlled context matching")
    void shouldSupportControlledContextMatching() {
        KernelPolicyDecisionPoint pdp = pdpWithPolicy(
            policyOf(
                rule(
                    "deny-stop-in-prod",
                    PolicyEffect.DENY,
                    List.of("ROLE_operator"),
                    List.of("stop"),
                    List.of("module"),
                    List.of(),
                    Map.of("environment", List.of("prod"))
                )
            )
        );

        AuthorizationDecision prodDecision = pdp.authorize(
            request("ROLE_operator", "stop", "module", null, Map.of("environment", "prod"))
        );
        AuthorizationDecision devDecision = pdp.authorize(
            request("ROLE_operator", "stop", "module", null, Map.of("environment", "dev"))
        );

        assertEquals(AuthorizationDecisionResult.DENY, prodDecision.result());
        assertEquals(DecisionReasonCode.DENY_RULE_MATCHED, prodDecision.reasonCode());
        assertEquals(AuthorizationDecisionResult.DENY, devDecision.result());
        assertEquals(DecisionReasonCode.NO_MATCHING_RULE, devDecision.reasonCode());
    }

    @Test
    @DisplayName("Malformed context should not grant access")
    void malformedContextShouldNotGrantAccess() {
        KernelPolicyDecisionPoint pdp = pdpWithPolicy(
            policyOf(
                rule(
                    "permit-read-with-context",
                    PolicyEffect.PERMIT,
                    List.of("ROLE_admin"),
                    List.of("read"),
                    List.of("module"),
                    List.of(),
                    Map.of("environment", List.of("prod"))
                )
            )
        );

        AuthorizationDecision decision = pdp.authorize(
            request("ROLE_admin", "read", "module", null, Map.of("environment", " "))
        );

        assertEquals(AuthorizationDecisionResult.DENY, decision.result());
        assertEquals(DecisionReasonCode.NO_MATCHING_RULE, decision.reasonCode());
    }

    @Test
    @DisplayName("Internal evaluation error should deny")
    void internalEvaluationErrorShouldDeny() {
        ActiveAuthorizationPolicyStore failingStore = new ActiveAuthorizationPolicyStore() {
            @Override
            public Optional<io.govaryn.kernel.security.authorization.policy.ActiveAuthorizationPolicySnapshot> getActivePolicy() {
                throw new IllegalStateException("Store unavailable");
            }

            @Override
            public io.govaryn.kernel.security.authorization.policy.ActiveAuthorizationPolicySnapshot activate(
                PolicySetDocument policySet
            ) {
                throw new UnsupportedOperationException("Not used");
            }
        };
        KernelPolicyDecisionPoint pdp = new KernelPolicyDecisionPoint(failingStore, new AuthorizationDecisionLogger());

        AuthorizationDecision decision = pdp.authorize(request("ROLE_admin", "read", "module", null, Map.of()));

        assertEquals(AuthorizationDecisionResult.DENY, decision.result());
        assertEquals(DecisionReasonCode.EVALUATION_ERROR, decision.reasonCode());
    }

    private static KernelPolicyDecisionPoint pdpWithPolicy(PolicySetDocument policySetDocument) {
        InMemoryActiveAuthorizationPolicyStore store = new InMemoryActiveAuthorizationPolicyStore();
        store.activate(policySetDocument);
        return new KernelPolicyDecisionPoint(store, new AuthorizationDecisionLogger());
    }

    private static PolicySetDocument policyOf(PolicyRuleDocument... rules) {
        return new PolicySetDocument("test-rev", List.of(rules));
    }

    private static PolicyRuleDocument rule(
        String id,
        PolicyEffect effect,
        List<String> roles,
        List<String> actions,
        List<String> resourceTypes,
        List<String> resourceIds,
        Map<String, List<String>> contextAnyOfByAttribute
    ) {
        Map<String, PolicyContextAttributeCondition> attributes = contextAnyOfByAttribute.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> new PolicyContextAttributeCondition(entry.getValue())
            ));

        return new PolicyRuleDocument(
            id,
            effect,
            new PolicySubjectMatchCriteria(roles),
            actions,
            resourceTypes,
            resourceIds,
            new PolicyContextMatchCriteria(attributes)
        );
    }

    private static AuthorizationRequest request(
        String role,
        String action,
        String resourceType,
        String resourceId,
        Map<String, String> context
    ) {
        return new AuthorizationRequest(
            new AuthorizationSubject("subject-1", List.of(role), Map.of()),
            action,
            resourceType,
            resourceId,
            context
        );
    }
}
