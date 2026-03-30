package io.govaryn.kernel.security.authorization.policy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("YamlAuthorizationPolicyParser Tests")
class YamlAuthorizationPolicyParserTest {

    private final YamlAuthorizationPolicyParser parser = new YamlAuthorizationPolicyParser();

    @Test
    @DisplayName("Should parse valid policy YAML into parsed DTO")
    void shouldParseValidPolicyYamlIntoParsedDto() {
        ParsedPolicySet parsed = parser.parse(new ClassPathResource("security/authorization/policy/policy-valid.yaml"));

        assertEquals("2026-03-30.v1", parsed.policySetRevision());
        assertEquals(3, parsed.rules().size());
        assertEquals("permit-module-status-read", parsed.rules().getFirst().id());
    }

    @Test
    @DisplayName("Should reject malformed YAML")
    void shouldRejectMalformedYaml() {
        PolicyParseException ex = assertThrows(
            PolicyParseException.class,
            () -> parser.parse(new ClassPathResource("security/authorization/policy/policy-malformed.yaml"))
        );

        assertTrue(ex.getMessage().contains("Failed to parse policy YAML"));
    }

    @Test
    @DisplayName("Should reject wrong field types with clear field path")
    void shouldRejectWrongFieldTypesWithClearFieldPath() {
        String yaml = """
            policySetRevision: "2026-03-30.v1"
            rules:
              - id: 123
                effect: PERMIT
                subject:
                  roles:
                    - ROLE_admin
                actions:
                  - read
                resourceTypes:
                  - module
            """;

        PolicyParseException ex = assertThrows(
            PolicyParseException.class,
            () -> parser.parse(new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8), "inline-invalid-types"))
        );

        assertTrue(ex.getMessage().contains("rules[0].id"));
        assertTrue(ex.getMessage().contains("must be a string"));
    }
}
