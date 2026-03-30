package io.govaryn.kernel.security.authorization.policy;

import io.govaryn.kernel.security.authorization.model.PolicyEffect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PolicySetDocument Tests")
class PolicySetDocumentTest {

    @Test
    @DisplayName("Should parse valid policy YAML example")
    void shouldParseValidPolicyYamlExample() {
        PolicySetDocument policySet = PolicySetDocument.fromMap(loadYaml("security/authorization/policy/policy-valid.yaml"));

        assertEquals("2026-03-30.v1", policySet.policySetRevision());
        assertEquals(3, policySet.rules().size());
        assertTrue(policySet.rules().stream().anyMatch(rule -> rule.effect() == PolicyEffect.PERMIT));
        assertTrue(policySet.rules().stream().anyMatch(rule -> rule.effect() == PolicyEffect.DENY));
        assertTrue(policySet.rules().stream().anyMatch(rule -> !rule.resourceIds().isEmpty()));
        assertTrue(
            policySet.rules().stream()
                .anyMatch(rule -> rule.context().attributes().containsKey("environment"))
        );
    }

    @Test
    @DisplayName("Should reject invalid policy YAML example")
    void shouldRejectInvalidPolicyYamlExample() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> PolicySetDocument.fromMap(loadYaml("security/authorization/policy/policy-invalid.yaml"))
        );

        assertTrue(
            ex.getMessage().contains("policySetRevision")
                || ex.getMessage().contains("effect")
                || ex.getMessage().contains("actions")
        );
    }

    @Test
    @DisplayName("Should reject duplicate rule identifiers")
    void shouldRejectDuplicateRuleIdentifiers() {
        PolicyRuleDocument ruleOne = new PolicyRuleDocument(
            "rule-1",
            PolicyEffect.PERMIT,
            new PolicySubjectMatchCriteria(List.of("ROLE_admin")),
            List.of("read"),
            List.of("module"),
            List.of(),
            new PolicyContextMatchCriteria(Map.of())
        );
        PolicyRuleDocument ruleTwo = new PolicyRuleDocument(
            "rule-1",
            PolicyEffect.DENY,
            new PolicySubjectMatchCriteria(List.of("ROLE_support")),
            List.of("read"),
            List.of("module"),
            List.of(),
            new PolicyContextMatchCriteria(Map.of())
        );

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> new PolicySetDocument("rev-1", List.of(ruleOne, ruleTwo))
        );

        assertTrue(ex.getMessage().contains("duplicate id"));
    }

    @Test
    @DisplayName("Should enforce required rule fields in map conversion")
    void shouldEnforceRequiredRuleFieldsInMapConversion() {
        Map<String, Object> subject = Map.of("roles", List.of("ROLE_admin"));

        Map<String, Object> invalidRule = new LinkedHashMap<>();
        invalidRule.put("id", "rule-1");
        invalidRule.put("effect", "PERMIT");
        invalidRule.put("subject", subject);
        invalidRule.put("actions", List.of());
        invalidRule.put("resourceTypes", List.of("module"));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("policySetRevision", "rev-1");
        root.put("rules", List.of(invalidRule));

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> PolicySetDocument.fromMap(root)
        );

        assertTrue(ex.getMessage().contains("actions"));
    }

    private static Map<String, Object> loadYaml(String classpathLocation) {
        YamlMapFactoryBean yaml = new YamlMapFactoryBean();
        yaml.setResources(new ClassPathResource(classpathLocation));
        Map<String, Object> map = yaml.getObject();
        if (map == null) {
            throw new IllegalStateException("Failed to load YAML from classpath: " + classpathLocation);
        }
        return map;
    }
}
