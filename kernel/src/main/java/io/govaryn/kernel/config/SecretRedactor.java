package io.govaryn.kernel.config;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility for redacting sensitive configuration values in logs and diagnostics.
 * Automatically detects common secret patterns and allows custom registration.
 */
public class SecretRedactor {

    private static final String REDACTED = "***REDACTED***";
    private static final Set<String> SECRET_PATTERNS = new HashSet<>();
    private static final Set<Pattern> SECRET_KEY_REGEXES = new HashSet<>();

    static {
        // Common secret key patterns
        SECRET_PATTERNS.add("password");
        SECRET_PATTERNS.add("secret");
        SECRET_PATTERNS.add("token");
        SECRET_PATTERNS.add("apikey");
        SECRET_PATTERNS.add("api_key");
        SECRET_PATTERNS.add("key");
        SECRET_PATTERNS.add("credential");
        SECRET_PATTERNS.add("auth");

        // Regex patterns for secret keys
        SECRET_KEY_REGEXES.add(Pattern.compile("(?i).*password.*"));
        SECRET_KEY_REGEXES.add(Pattern.compile("(?i).*secret.*"));
        SECRET_KEY_REGEXES.add(Pattern.compile("(?i).*token.*"));
        SECRET_KEY_REGEXES.add(Pattern.compile("(?i).*apikey.*|.*api_key.*"));
        SECRET_KEY_REGEXES.add(Pattern.compile("(?i).*credential.*"));
        SECRET_KEY_REGEXES.add(Pattern.compile("(?i).*auth.*"));
    }

    /**
     * Register a custom secret key pattern to be redacted.
     *
     * @param pattern Regex pattern for secret keys (case-insensitive)
     */
    public static void registerSecretPattern(String pattern) {
        SECRET_KEY_REGEXES.add(Pattern.compile(pattern));
    }

    /**
     * Check if a key should be redacted (contains secret keywords).
     *
     * @param key Configuration key
     * @return true if the key appears to contain a secret
     */
    public static boolean isSecret(String key) {
        if (key == null) {
            return false;
        }

        String lowerKey = key.toLowerCase();

        // Check against simple patterns
        for (String pattern : SECRET_PATTERNS) {
            if (lowerKey.contains(pattern)) {
                return true;
            }
        }

        // Check against regex patterns
        for (Pattern regex : SECRET_KEY_REGEXES) {
            if (regex.matcher(key).matches()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Redact a configuration value if the key is a secret.
     *
     * @param key Configuration key
     * @param value Configuration value
     * @return Original value if not a secret, redacted value if secret, null if value is null
     */
    public static Object redactIfSecret(String key, Object value) {
        if (value == null || !isSecret(key)) {
            return value;
        }
        return REDACTED;
    }

    /**
     * Redact all secret values in a configuration map.
     * Creates a copy to avoid modifying the original.
     *
     * @param config Configuration map
     * @return A copy of the map with secrets redacted
     */
    public static java.util.Map<String, Object> redactSecrets(java.util.Map<String, Object> config) {
        var redacted = new java.util.LinkedHashMap<>(config);
        redacted.replaceAll((key, value) -> redactIfSecret(key, value));
        return redacted;
    }

    /**
     * Get a redacted string representation of a configuration map for logging.
     *
     * @param config Configuration map
     * @return String representation with secrets redacted
     */
    public static String toRedactedString(java.util.Map<String, Object> config) {
        var sb = new StringBuilder("{");
        var redacted = redactSecrets(config);
        var first = true;
        for (var entry : redacted.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}

