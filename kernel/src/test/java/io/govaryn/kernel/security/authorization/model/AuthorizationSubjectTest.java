package io.govaryn.kernel.security.authorization.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AuthorizationSubject Tests")
class AuthorizationSubjectTest {

    @Test
    @DisplayName("Should reject blank subjectId")
    void shouldRejectBlankSubjectId() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> new AuthorizationSubject("  ", List.of("admin"), Map.of())
        );

        assertTrue(ex.getMessage().contains("subjectId"));
    }

    @Test
    @DisplayName("Should sanitize and copy roles and attributes")
    void shouldSanitizeAndCopyRolesAndAttributes() {
        List<String> mutableRoles = new ArrayList<>(List.of(" admin ", "support", "support"));
        Map<String, String> mutableAttributes = new LinkedHashMap<>();
        mutableAttributes.put(" region ", "eu");

        AuthorizationSubject subject = new AuthorizationSubject("user-1", mutableRoles, mutableAttributes);

        mutableRoles.add("ops");
        mutableAttributes.put("team", "kernel");

        assertEquals(List.of("admin", "support"), subject.roles());
        assertEquals("eu", subject.attributes().get("region"));
        assertThrows(UnsupportedOperationException.class, () -> subject.roles().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> subject.attributes().put("y", "z"));
    }
}
