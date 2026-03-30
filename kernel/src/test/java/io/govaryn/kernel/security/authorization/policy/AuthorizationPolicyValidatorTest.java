package io.govaryn.kernel.security.authorization.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AuthorizationPolicyValidator Tests")
class AuthorizationPolicyValidatorTest {

    private final YamlAuthorizationPolicyParser parser = new YamlAuthorizationPolicyParser();
    private final AuthorizationPolicyValidator validator = new AuthorizationPolicyValidator();

    @Test
    @DisplayName("Should accept valid policy")
    void shouldAcceptValidPolicy() {
        ParsedPolicySet parsed = parser.parse(new ClassPathResource("security/authorization/policy/policy-valid.yaml"));

        PolicySetDocument validated = validator.validate(parsed);

        assertEquals("2026-03-30.v1", validated.policySetRevision());
        assertEquals(3, validated.rules().size());
    }

    @Test
    @DisplayName("Should reject duplicate rule ids")
    void shouldRejectDuplicateRuleIds() {
        ParsedPolicySet parsed = parser.parse(new ClassPathResource("security/authorization/policy/policy-duplicate-rule-id.yaml"));

        PolicyValidationException ex = assertThrows(PolicyValidationException.class, () -> validator.validate(parsed));

        assertTrue(ex.getMessage().contains("duplicate id"));
    }

    @Test
    @DisplayName("Should reject empty actions")
    void shouldRejectEmptyActions() {
        ParsedPolicySet parsed = parser.parse(new ClassPathResource("security/authorization/policy/policy-empty-actions.yaml"));

        PolicyValidationException ex = assertThrows(PolicyValidationException.class, () -> validator.validate(parsed));

        assertTrue(ex.getMessage().contains("actions"));
    }

    @Test
    @DisplayName("Should reject empty resource types")
    void shouldRejectEmptyResourceTypes() {
        ParsedPolicySet parsed = parser.parse(new ClassPathResource("security/authorization/policy/policy-empty-resource-types.yaml"));

        PolicyValidationException ex = assertThrows(PolicyValidationException.class, () -> validator.validate(parsed));

        assertTrue(ex.getMessage().contains("resourceTypes"));
    }

    @Test
    @DisplayName("Should reject unsupported context keys and operators")
    void shouldRejectUnsupportedContextKeysAndOperators() {
        ParsedPolicySet parsed = parser.parse(new ClassPathResource("security/authorization/policy/policy-unsupported-context.yaml"));

        PolicyValidationException ex = assertThrows(PolicyValidationException.class, () -> validator.validate(parsed));

        assertTrue(ex.getMessage().contains("unsupported key: tenant"));
        assertTrue(ex.getMessage().contains("unsupported operator: equals"));
    }

    @Test
    @DisplayName("Should reject missing top-level fields")
    void shouldRejectMissingTopLevelFields() {
        PolicyValidationException ex = assertThrows(
            PolicyValidationException.class,
            () -> validator.validate(new ParsedPolicySet(null, null))
        );

        assertTrue(ex.getMessage().contains("policySetRevision"));
        assertTrue(ex.getMessage().contains("rules"));
    }
}
