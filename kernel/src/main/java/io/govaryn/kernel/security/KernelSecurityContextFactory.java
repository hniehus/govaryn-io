package io.govaryn.kernel.security;

import io.govaryn.kernel.config.GovarynKernelSecurityProperties;
import io.govaryn.kernel.security.authorization.framework.model.SecurityContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class KernelSecurityContextFactory {

    private static final Pattern TOKEN_SPLIT_PATTERN = Pattern.compile("[,\\s]+");
    private static final List<String> TENANT_CLAIM_KEYS = List.of("tenant_id", "tenantId", "tid");

    private final KernelSecurityIdentityResolver securityIdentityResolver;
    private final GovarynKernelSecurityProperties securityProperties;

    public KernelSecurityContextFactory(
        KernelSecurityIdentityResolver securityIdentityResolver,
        GovarynKernelSecurityProperties securityProperties
    ) {
        this.securityIdentityResolver = securityIdentityResolver;
        this.securityProperties = securityProperties;
    }

    public SecurityContext create(Authentication authentication) {
        KernelSecurityIdentity identity = securityIdentityResolver.resolve(authentication);
        String tenantId = resolveTenantId(authentication);
        Map<String, String> claims = resolveClaims(authentication, tenantId);
        Map<String, String> authenticationMetadata = resolveAuthenticationMetadata(identity, authentication);

        return new SecurityContext(
            identity.subject(),
            tenantId,
            normalizeAuthorities(identity.authorities()),
            claims,
            authenticationMetadata
        );
    }

    private String resolveTenantId(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            return null;
        }

        for (String claimKey : TENANT_CLAIM_KEYS) {
            String value = claimAsString(jwtAuthenticationToken.getToken().getClaims().get(claimKey));
            if (hasText(value)) {
                return value.trim();
            }
        }

        return null;
    }

    private Map<String, String> resolveClaims(Authentication authentication, String tenantId) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            return Map.of();
        }

        Map<String, Object> jwtClaims = jwtAuthenticationToken.getToken().getClaims();
        Map<String, String> claims = new LinkedHashMap<>();

        if (hasText(tenantId)) {
            String tenantClaimKey = resolveTenantClaimKey(jwtClaims);
            claims.put(tenantClaimKey == null ? "tenant_id" : tenantClaimKey, tenantId.trim());
        }

        addNormalizedClaim(claims, "scope", jwtClaims.get("scope"));
        addNormalizedClaim(claims, "scp", jwtClaims.get("scp"));
        addNormalizedClaim(claims, securityProperties.getAuthorityClaim(), jwtClaims.get(securityProperties.getAuthorityClaim()));

        return Map.copyOf(claims);
    }

    private Map<String, String> resolveAuthenticationMetadata(
        KernelSecurityIdentity identity,
        Authentication authentication
    ) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("authenticationType", authentication.getClass().getSimpleName());
        if (hasText(identity.username())) {
            metadata.put("username", identity.username().trim());
        }
        if (hasText(identity.issuer())) {
            metadata.put("issuer", identity.issuer().trim());
        }
        return Map.copyOf(metadata);
    }

    private List<String> normalizeAuthorities(List<String> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            return List.of();
        }

        return authorities.stream()
            .filter(this::hasText)
            .map(String::trim)
            .distinct()
            .sorted()
            .toList();
    }

    private void addNormalizedClaim(Map<String, String> claims, String claimKey, Object claimValue) {
        if (!hasText(claimKey)) {
            return;
        }
        String normalized = normalizeClaimValue(claimValue);
        if (hasText(normalized)) {
            claims.put(claimKey.trim(), normalized);
        }
    }

    private String resolveTenantClaimKey(Map<String, Object> claims) {
        for (String claimKey : TENANT_CLAIM_KEYS) {
            if (claims.containsKey(claimKey)) {
                return claimKey;
            }
        }
        return null;
    }

    private String normalizeClaimValue(Object rawValue) {
        if (rawValue == null) {
            return null;
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
            default -> List.of(rawValue.toString());
        };

        if (tokens.isEmpty()) {
            return null;
        }

        return tokens.stream()
            .filter(this::hasText)
            .distinct()
            .sorted()
            .reduce((left, right) -> left + " " + right)
            .orElse(null);
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
