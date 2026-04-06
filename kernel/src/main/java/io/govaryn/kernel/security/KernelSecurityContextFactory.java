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

    private final KernelSecurityIdentityResolver securityIdentityResolver;
    private final KernelTenantScopeExtractor tenantScopeExtractor;
    private final KernelActiveTenantResolver activeTenantResolver;
    private final KernelPrivilegedTenantAccessEvaluator privilegedTenantAccessEvaluator;
    private final GovarynKernelSecurityProperties securityProperties;

    public KernelSecurityContextFactory(
        KernelSecurityIdentityResolver securityIdentityResolver,
        KernelTenantScopeExtractor tenantScopeExtractor,
        KernelActiveTenantResolver activeTenantResolver,
        KernelPrivilegedTenantAccessEvaluator privilegedTenantAccessEvaluator,
        GovarynKernelSecurityProperties securityProperties
    ) {
        this.securityIdentityResolver = securityIdentityResolver;
        this.tenantScopeExtractor = tenantScopeExtractor;
        this.activeTenantResolver = activeTenantResolver;
        this.privilegedTenantAccessEvaluator = privilegedTenantAccessEvaluator;
        this.securityProperties = securityProperties;
    }

    public SecurityContext create(Authentication authentication) {
        return toAuthorizationSecurityContext(createKernelContext(authentication));
    }

    public KernelSecurityTenantContext createKernelContext(Authentication authentication) {
        return createKernelContext(authentication, null, false);
    }

    public KernelSecurityTenantContext createKernelContext(
        Authentication authentication,
        String routeTenantId,
        boolean tenantProtectedOperation
    ) {
        boolean privilegedCrossTenantAccess = privilegedTenantAccessEvaluator
            .hasPrivilegedCrossTenantAccess(authentication);
        return createKernelContext(authentication, routeTenantId, tenantProtectedOperation, privilegedCrossTenantAccess);
    }

    private KernelSecurityTenantContext createKernelContext(
        Authentication authentication,
        String routeTenantId,
        boolean tenantProtectedOperation,
        boolean privilegedCrossTenantAccess
    ) {
        KernelSecurityIdentity identity = securityIdentityResolver.resolve(authentication);
        KernelTenantScopeExtraction tenantScopeExtraction = tenantScopeExtractor.extract(authentication);
        KernelActiveTenantContext activeTenant = activeTenantResolver.resolve(
            new KernelTenantResolutionRequest(
                tenantScopeExtraction.tenantScope(),
                routeTenantId,
                tenantProtectedOperation,
                privilegedCrossTenantAccess
            )
        ).orElse(null);
        Map<String, String> claims = resolveClaims(
            authentication,
            tenantScopeExtraction.sourceClaimKey(),
            activeTenant
        );
        Map<String, String> authenticationMetadata = resolveAuthenticationMetadata(
            identity,
            authentication,
            privilegedCrossTenantAccess
        );

        return new KernelSecurityTenantContext(
            identity,
            tenantScopeExtraction.tenantScope(),
            activeTenant,
            privilegedCrossTenantAccess,
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
        Authentication authentication,
        boolean privilegedCrossTenantAccess
    ) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("authenticationType", authentication.getClass().getSimpleName());
        metadata.put("privilegedCrossTenantAccess", String.valueOf(privilegedCrossTenantAccess));
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
