package io.govaryn.kernel.security.authorization.policy;

import io.govaryn.kernel.security.authorization.model.PolicyEffect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("InMemoryActiveAuthorizationPolicyStore")
class InMemoryActiveAuthorizationPolicyStoreTest {

    @Test
    @DisplayName("Should have no active policy before first activation")
    void shouldHaveNoActivePolicyBeforeFirstActivation() {
        InMemoryActiveAuthorizationPolicyStore store = new InMemoryActiveAuthorizationPolicyStore();

        assertTrue(store.getActivePolicy().isEmpty());
    }

    @Test
    @DisplayName("Should reject null policy activation")
    void shouldRejectNullPolicyActivation() {
        InMemoryActiveAuthorizationPolicyStore store = new InMemoryActiveAuthorizationPolicyStore();

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> store.activate(null)
        );

        assertTrue(ex.getMessage().contains("policySet"));
    }

    @Test
    @DisplayName("Should atomically replace active snapshot on activation")
    void shouldAtomicallyReplaceActiveSnapshotOnActivation() {
        InMemoryActiveAuthorizationPolicyStore store = new InMemoryActiveAuthorizationPolicyStore();

        ActiveAuthorizationPolicySnapshot v1 = store.activate(policy("rev-1", "rule-1"));
        ActiveAuthorizationPolicySnapshot v2 = store.activate(policy("rev-2", "rule-2"));

        assertEquals("rev-1", v1.revision());
        assertEquals("rev-2", v2.revision());
        assertEquals("rev-2", store.getActivePolicy().orElseThrow().revision());
        assertEquals("rule-2", store.getActivePolicy().orElseThrow().policySet().rules().getFirst().id());
    }

    private static PolicySetDocument policy(String revision, String ruleId) {
        return new PolicySetDocument(
            revision,
            List.of(
                new PolicyRuleDocument(
                    ruleId,
                    PolicyEffect.PERMIT,
                    new PolicySubjectMatchCriteria(List.of("ROLE_admin")),
                    List.of("read"),
                    List.of("module"),
                    List.of(),
                    new PolicyContextMatchCriteria(Map.of())
                )
            )
        );
    }
}
