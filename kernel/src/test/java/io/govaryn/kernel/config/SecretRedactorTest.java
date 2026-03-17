package io.govaryn.kernel.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SecretRedactor Tests")
class SecretRedactorTest {

    @Test
    @DisplayName("Should detect password keys as secrets")
    void testDetectPasswordKey() {
        assertTrue(SecretRedactor.isSecret("db.password"));
        assertTrue(SecretRedactor.isSecret("api.secret.password"));
        assertTrue(SecretRedactor.isSecret("PASSWORD"));
    }

    @Test
    @DisplayName("Should detect token keys as secrets")
    void testDetectTokenKey() {
        assertTrue(SecretRedactor.isSecret("jwt.token"));
        assertTrue(SecretRedactor.isSecret("auth.token"));
        assertTrue(SecretRedactor.isSecret("refresh_token"));
    }

    @Test
    @DisplayName("Should detect apikey keys as secrets")
    void testDetectApiKeyKey() {
        assertTrue(SecretRedactor.isSecret("service.apikey"));
        assertTrue(SecretRedactor.isSecret("service.api_key"));
        assertTrue(SecretRedactor.isSecret("API_KEY"));
    }

    @Test
    @DisplayName("Should not detect non-secret keys as secrets")
    void testNonSecretKeys() {
        assertFalse(SecretRedactor.isSecret("govaryn.kernel.id"));
        assertFalse(SecretRedactor.isSecret("server.port"));
        assertFalse(SecretRedactor.isSecret("app.name"));
    }

    @Test
    @DisplayName("Should redact secret values")
    void testRedactSecretValue() {
        Object result = SecretRedactor.redactIfSecret("db.password", "my-secret-password");
        assertEquals("***REDACTED***", result);
    }

    @Test
    @DisplayName("Should not redact non-secret values")
    void testNotRedactNonSecretValue() {
        Object value = "some-value";
        Object result = SecretRedactor.redactIfSecret("app.name", value);
        assertEquals(value, result);
    }

    @Test
    @DisplayName("Should redact all secrets in a configuration map")
    void testRedactSecretsInMap() {
        Map<String, Object> config = new HashMap<>();
        config.put("app.name", "MyApp");
        config.put("db.password", "secret123");
        config.put("db.host", "localhost");
        config.put("api.token", "token-xyz");

        Map<String, Object> redacted = SecretRedactor.redactSecrets(config);

        assertEquals("MyApp", redacted.get("app.name"));
        assertEquals("***REDACTED***", redacted.get("db.password"));
        assertEquals("localhost", redacted.get("db.host"));
        assertEquals("***REDACTED***", redacted.get("api.token"));
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void testHandleNullValues() {
        Object result = SecretRedactor.redactIfSecret("db.password", null);
        assertNull(result);

        result = SecretRedactor.redactIfSecret(null, "some-value");
        assertEquals("some-value", result);
    }

    @Test
    @DisplayName("Should provide redacted string representation of configuration")
    void testToRedactedString() {
        Map<String, Object> config = new HashMap<>();
        config.put("app.name", "MyApp");
        config.put("db.password", "secret123");

        String result = SecretRedactor.toRedactedString(config);

        assertTrue(result.contains("app.name=MyApp"));
        assertTrue(result.contains("db.password=***REDACTED***"));
    }

    @Test
    @DisplayName("Should allow custom secret patterns to be registered")
    void testRegisterCustomPattern() {
        String customKey = "mycompany.proprietary.encrypted_data";
        // This key doesn't match any default secret patterns
        // (Note: "secret" is a default pattern, so "custom.secret.key" would match anyway)

        // After registering a custom pattern, it should be detected
        SecretRedactor.registerSecretPattern("(?i).*encrypted_data.*");

        assertTrue(SecretRedactor.isSecret(customKey));
    }
}


