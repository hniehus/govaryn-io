package io.govaryn.kernel.security.authorization.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Policy Authorization Model Tests")
class PolicyModelTest {

    @Test
    @DisplayName("Should reject invalid policy rule required fields")
    void shouldRejectInvalidPolicyRuleRequiredFields() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new PolicyRule(" ", PolicyEffect.PERMIT, List.of("admin"), List.of("read"), List.of("module"), List.of())
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new PolicyRule("rule-1", null, List.of("admin"), List.of("read"), List.of("module"), List.of())
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new PolicyRule("rule-1", PolicyEffect.PERMIT, List.of("admin"), List.of(), List.of("module"), List.of())
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new PolicyRule("rule-1", PolicyEffect.PERMIT, List.of("admin"), List.of("read"), List.of(), List.of())
        );
    }

    @Test
    @DisplayName("Should copy policy set rules and reject null entries")
    void shouldCopyPolicySetRulesAndRejectNullEntries() {
        PolicyRule rule = new PolicyRule("rule-1", PolicyEffect.PERMIT, List.of("admin"), List.of("read"), List.of("module"), List.of());
        List<PolicyRule> mutableRules = new ArrayList<>(List.of(rule));

        PolicySet policySet = new PolicySet("policy-main", mutableRules);
        mutableRules.clear();

        assertEquals(1, policySet.rules().size());
        assertThrows(UnsupportedOperationException.class, () -> policySet.rules().add(rule));
        List<PolicyRule> rulesWithNull = new ArrayList<>();
        rulesWithNull.add(rule);
        rulesWithNull.add(null);
        assertThrows(IllegalArgumentException.class, () -> new PolicySet("policy-main", rulesWithNull));
    }

    @Test
    @DisplayName("Should validate authorization decision fields")
    void shouldValidateAuthorizationDecisionFields() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AuthorizationDecision(null, DecisionReasonCode.NO_MATCHING_RULE, null)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new AuthorizationDecision(AuthorizationDecisionResult.DENY, null, null)
        );

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> new AuthorizationDecision(AuthorizationDecisionResult.PERMIT, DecisionReasonCode.POLICY_RULE_MATCHED, " ")
        );
        assertTrue(ex.getMessage().contains("matchedRuleId"));
    }
}
