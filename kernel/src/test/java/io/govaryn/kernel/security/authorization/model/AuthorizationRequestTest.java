package io.govaryn.kernel.security.authorization.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AuthorizationRequest Tests")
class AuthorizationRequestTest {

    private static final AuthorizationSubject SUBJECT = new AuthorizationSubject("user-1", java.util.List.of("admin"), Map.of());

    @Test
    @DisplayName("Should reject missing required fields")
    void shouldRejectMissingRequiredFields() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AuthorizationRequest(null, "read", "module", null, Map.of())
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new AuthorizationRequest(SUBJECT, " ", "module", null, Map.of())
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new AuthorizationRequest(SUBJECT, "read", " ", null, Map.of())
        );
    }

    @Test
    @DisplayName("Should reject blank resourceId when provided")
    void shouldRejectBlankResourceIdWhenProvided() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> new AuthorizationRequest(SUBJECT, "read", "module", "  ", Map.of())
        );

        assertTrue(ex.getMessage().contains("resourceId"));
    }

    @Test
    @DisplayName("Should sanitize and copy context")
    void shouldSanitizeAndCopyContext() {
        Map<String, String> mutableContext = new LinkedHashMap<>();
        mutableContext.put(" traceId ", "abc-123");

        AuthorizationRequest request = new AuthorizationRequest(SUBJECT, "read", "module", null, mutableContext);

        mutableContext.put("scope", "extra");

        assertEquals("abc-123", request.context().get("traceId"));
        assertThrows(UnsupportedOperationException.class, () -> request.context().put("k", "v"));
    }

    @Test
    @DisplayName("Should reject null context value")
    void shouldRejectNullContextValue() {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("traceId", null);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> new AuthorizationRequest(SUBJECT, "read", "module", null, context)
        );

        assertTrue(ex.getMessage().contains("context value"));
    }

    @Test
    @DisplayName("Should default null context to empty map")
    void shouldDefaultNullContextToEmptyMap() {
        AuthorizationRequest request = new AuthorizationRequest(SUBJECT, "read", "module", null, null);

        assertTrue(request.context().isEmpty());
    }
}
