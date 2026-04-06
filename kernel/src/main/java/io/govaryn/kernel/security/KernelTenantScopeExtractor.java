package io.govaryn.kernel.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Extracts token-authoritative tenant scope from JWT claims.
 */
@Component
public class KernelTenantScopeExtractor {

    private static final Pattern TOKEN_SPLIT_PATTERN = Pattern.compile("[,\\s]+");
    private static final List<String> TENANT_SCOPE_CLAIM_KEYS = List.of(
        "tenant_scope",
        "tenantScope",
        "tenant_ids",
        "tenantIds",
        "tenants"
    );
    private static final List<String> TENANT_SINGLE_CLAIM_KEYS = List.of("tenant_id", "tenantId", "tid");

    public KernelTenantScopeExtraction extract(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            throw new IllegalArgumentException("Authenticated JWT principal is required");
        }
        return extractFromClaims(jwtAuthenticationToken.getToken().getClaims());
    }

    KernelTenantScopeExtraction extractFromClaims(Map<String, Object> claims) {
        if (claims == null || claims.isEmpty()) {
            return new KernelTenantScopeExtraction(KernelTenantScope.empty(), null);
        }

        for (String claimKey : TENANT_SCOPE_CLAIM_KEYS) {
            List<String> scopeValues = normalizeClaimTokens(claims.get(claimKey));
            if (!scopeValues.isEmpty()) {
                return new KernelTenantScopeExtraction(new KernelTenantScope(scopeValues), claimKey);
            }
        }

        for (String claimKey : TENANT_SINGLE_CLAIM_KEYS) {
            String singleTenantId = claimAsString(claims.get(claimKey));
            if (hasText(singleTenantId)) {
                return new KernelTenantScopeExtraction(
                    new KernelTenantScope(List.of(singleTenantId)),
                    claimKey
                );
            }
        }

        return new KernelTenantScopeExtraction(KernelTenantScope.empty(), null);
    }

    private List<String> normalizeClaimTokens(Object rawValue) {
        if (rawValue == null) {
            return List.of();
        }

        Collection<String> tokens = switch (rawValue) {
            case String value -> TOKEN_SPLIT_PATTERN.splitAsStream(value)
                .filter(this::hasText)
                .map(String::trim)
                .toList();
            case Collection<?> values -> values.stream()
                .filter(item -> item != null && hasText(item.toString()))
                .map(item -> item.toString().trim())
                .flatMap(item -> TOKEN_SPLIT_PATTERN.splitAsStream(item))
                .filter(this::hasText)
                .map(String::trim)
                .toList();
            default -> List.of(rawValue.toString().trim());
        };

        return tokens.stream()
            .filter(this::hasText)
            .distinct()
            .sorted()
            .toList();
    }

    private String claimAsString(Object claimValue) {
        if (claimValue == null) {
            return null;
        }
        if (claimValue instanceof String claim) {
            return claim;
        }
        return String.valueOf(claimValue);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
