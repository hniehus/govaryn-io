package io.govaryn.kernel.security.authorization.framework;

import io.govaryn.kernel.security.authorization.framework.model.AuthorizationRequest;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Shared scope extraction utility for module policy evaluators.
 */
public final class AuthorizationScopeExtractor {

    private static final Pattern SCOPE_SPLIT_PATTERN = Pattern.compile("[,\\s]+");

    private AuthorizationScopeExtractor() {
    }

    public static Set<String> extractScopes(AuthorizationRequest request) {
        if (request == null || request.securityContext() == null) {
            return Set.of();
        }
        Set<String> scopes = new LinkedHashSet<>();
        appendScopes(scopes, request.securityContext().claims().get("scope"));
        appendScopes(scopes, request.securityContext().claims().get("scp"));
        return Set.copyOf(scopes);
    }

    private static void appendScopes(Set<String> scopes, String rawScopes) {
        if (rawScopes == null || rawScopes.isBlank()) {
            return;
        }
        SCOPE_SPLIT_PATTERN.splitAsStream(rawScopes)
            .filter(scope -> scope != null && !scope.isBlank())
            .map(String::trim)
            .forEach(scopes::add);
    }
}
