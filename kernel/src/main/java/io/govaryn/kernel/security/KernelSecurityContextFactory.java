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
    private static final List<String> TENANT_SCOPE_CLAIM_KEYS = List.of(
        "tenant_scope",
        "tenantScope",
        "tenant_ids",
        "tenantIds",
        "tenants"
    );
    private static final List<String> TENANT_SINGLE_CLAIM_KEYS = List.of("tenant_id", "tenantId", "tid");

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
        return toAuthorizationSecurityContext(createKernelContext(authentication));
    }

    public KernelSecurityTenantContext createKernelContext(Authentication authentication) {
        KernelSecurityIdentity identity = securityIdentityResolver.resolve(authentication);
        TenantScopeResolution tenantResolution = resolveTenantScope(authentication);
        KernelActiveTenantContext activeTenant = resolveActiveTenant(tenantResolution.scope());
        Map<String, String> claims = resolveClaims(
            authentication,
            tenantResolution.sourceClaimKey(),
            activeTenant
        );
        Map<String, String> authenticationMetadata = resolveAuthenticationMetadata(identity, authentication);

        return new KernelSecurityTenantContext(
            identity,
            tenantResolution.scope(),
            activeTenant,
            claims,
            authenticationMetadata
        );
    }

    SecurityContext toAuthorizationSecurityContext(KernelSecurityTenantContext kernelContext) {
        return new SecurityContext(
            kernelContext.userId(),
            kernelContext.activeTenantId(),
            kernelContext.authorities(),
            kernelContext.claims(),
            kernelContext.authenticationMetadata()
        );
    }

    private TenantScopeResolution resolveTenantScope(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            return new TenantScopeResolution(KernelTenantScope.empty(), null);
        }

        Map<String, Object> claims = jwtAuthenticationToken.getToken().getClaims();
        for (String claimKey : TENANT_SCOPE_CLAIM_KEYS) {
            List<String> scopeValues = normalizeClaimTokens(claims.get(claimKey));
            if (!scopeValues.isEmpty()) {
                return new TenantScopeResolution(new KernelTenantScope(scopeValues), claimKey);
            }
        }

        for (String claimKey : TENANT_SINGLE_CLAIM_KEYS) {
            String singleTenantId = claimAsString(claims.get(claimKey));
            if (hasText(singleTenantId)) {
                return new TenantScopeResolution(new KernelTenantScope(List.of(singleTenantId)), claimKey);
            }
        }

        return new TenantScopeResolution(KernelTenantScope.empty(), null);
    }

    private KernelActiveTenantContext resolveActiveTenant(KernelTenantScope tenantScope) {
        return tenantScope.singleTenantId()
            .map(KernelActiveTenantContext::new)
            .orElse(null);
    }

    private Map<String, String> resolveClaims(
        Authentication authentication,
        String tenantClaimKey,
        KernelActiveTenantContext activeTenant
    ) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            return Map.of();
        }

        Map<String, Object> jwtClaims = jwtAuthenticationToken.getToken().getClaims();
        Map<String, String> claims = new LinkedHashMap<>();

        if (activeTenant != null) {
            claims.put(hasText(tenantClaimKey) ? tenantClaimKey.trim() : "tenant_id", activeTenant.tenantId());
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

    private void addNormalizedClaim(Map<String, String> claims, String claimKey, Object claimValue) {
        if (!hasText(claimKey)) {
            return;
        }
        String normalized = normalizeClaimValue(claimValue);
        if (hasText(normalized)) {
            claims.put(claimKey.trim(), normalized);
        }
    }

    private String normalizeClaimValue(Object rawValue) {
        List<String> tokens = normalizeClaimTokens(rawValue);
        if (tokens.isEmpty()) {
            return null;
        }

        return String.join(" ", tokens);
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

    private record TenantScopeResolution(KernelTenantScope scope, String sourceClaimKey) {
    }
}
