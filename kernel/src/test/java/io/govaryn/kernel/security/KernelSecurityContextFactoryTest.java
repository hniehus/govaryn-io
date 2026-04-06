package io.govaryn.kernel.security;

import io.govaryn.kernel.config.GovarynKernelSecurityProperties;
import io.govaryn.kernel.security.authorization.framework.model.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KernelSecurityContextFactoryTest {

    @Test
    void mapsExpectedPrincipalDataIntoKernelSecurityContext() {
        KernelSecurityContextFactory factory = newFactory("roles", "ROLE_");

        JwtAuthenticationToken authentication = jwtAuthenticationToken(
            Map.of(
                "sub", "user-42",
                "iss", "https://idp.example.com/realms/main",
                "tenant_id", "tenant-a",
                "scope", "module.status:read module.status:write",
                "roles", List.of("support", "admin")
            ),
            List.of("ROLE_support", "ROLE_admin"),
            "alice"
        );

        SecurityContext securityContext = factory.create(authentication);

        assertThat(securityContext.userId()).isEqualTo("user-42");
        assertThat(securityContext.tenantId()).isEqualTo("tenant-a");
        assertThat(securityContext.globalRoles()).containsExactly("ROLE_admin", "ROLE_support");
        assertThat(securityContext.claims())
            .containsEntry("tenant_id", "tenant-a")
            .containsEntry("scope", "module.status:read module.status:write")
            .containsEntry("roles", "admin support");
        assertThat(securityContext.authenticationMetadata())
            .containsEntry("authenticationType", "JwtAuthenticationToken")
            .containsEntry("username", "alice")
            .containsEntry("issuer", "https://idp.example.com/realms/main");
    }

    @Test
    void handlesMissingOptionalClaims() {
        KernelSecurityContextFactory factory = newFactory("roles", "ROLE_");

        JwtAuthenticationToken authentication = jwtAuthenticationToken(
            Map.of("sub", "user-7"),
            List.of(),
            "user-7"
        );

        SecurityContext securityContext = factory.create(authentication);

        assertThat(securityContext.userId()).isEqualTo("user-7");
        assertThat(securityContext.tenantId()).isNull();
        assertThat(securityContext.globalRoles()).isEmpty();
        assertThat(securityContext.claims()).isEmpty();
        assertThat(securityContext.authenticationMetadata())
            .containsEntry("authenticationType", "JwtAuthenticationToken")
            .containsEntry("username", "user-7")
            .doesNotContainKey("issuer");
    }

    @Test
    void normalizesRolesAndClaimsDeterministically() {
        KernelSecurityContextFactory factory = newFactory("roles", "ROLE_");

        JwtAuthenticationToken authentication = jwtAuthenticationToken(
            Map.of(
                "sub", "user-99",
                "tid", "tenant-z",
                "scope", "write read write",
                "scp", List.of("payments:write", "payments:read", "payments:write"),
                "roles", List.of("support", "admin", "support")
            ),
            List.of("ROLE_support", "ROLE_admin", "ROLE_support"),
            "zora"
        );

        SecurityContext securityContext = factory.create(authentication);

        assertThat(securityContext.globalRoles()).containsExactly("ROLE_admin", "ROLE_support");
        assertThat(securityContext.tenantId()).isEqualTo("tenant-z");
        assertThat(securityContext.claims())
            .containsEntry("tid", "tenant-z")
            .containsEntry("scope", "read write")
            .containsEntry("scp", "payments:read payments:write")
            .containsEntry("roles", "admin support");
    }

    @Test
    void mapsTenantScopeAndLeavesActiveTenantUnsetWhenMultipleTenantsAreGranted() {
        KernelSecurityContextFactory factory = newFactory("roles", "ROLE_");

        JwtAuthenticationToken authentication = jwtAuthenticationToken(
            Map.of(
                "sub", "user-314",
                "tenant_ids", List.of("tenant-b", "tenant-a", "tenant-b"),
                "roles", List.of("support")
            ),
            List.of("ROLE_support"),
            "mia"
        );

        KernelSecurityTenantContext kernelContext = factory.createKernelContext(authentication);

        assertThat(kernelContext.tenantScope().permittedTenantIds()).containsExactly("tenant-a", "tenant-b");
        assertThat(kernelContext.activeTenant()).isNull();
        assertThat(kernelContext.principal().subject()).isEqualTo("user-314");
        assertThat(kernelContext.principal().authorities()).containsExactly("ROLE_support");
    }

    @Test
    void resolvesExplicitRouteTenantForTenantProtectedRequestWhenTenantIsInScope() {
        KernelSecurityContextFactory factory = newFactory("roles", "ROLE_");

        JwtAuthenticationToken authentication = jwtAuthenticationToken(
            Map.of(
                "sub", "user-500",
                "tenant_ids", List.of("tenant-a", "tenant-b"),
                "roles", List.of("support")
            ),
            List.of("ROLE_support"),
            "ava"
        );

        KernelSecurityTenantContext kernelContext = factory.createKernelContext(
            authentication,
            "tenant-b",
            true,
            false
        );

        assertThat(kernelContext.activeTenantId()).isEqualTo("tenant-b");
    }

    @Test
    void deniesTenantProtectedRequestWithMultiTenantScopeWhenRouteTenantIsMissing() {
        KernelSecurityContextFactory factory = newFactory("roles", "ROLE_");

        JwtAuthenticationToken authentication = jwtAuthenticationToken(
            Map.of(
                "sub", "user-501",
                "tenant_ids", List.of("tenant-a", "tenant-b"),
                "roles", List.of("support")
            ),
            List.of("ROLE_support"),
            "kai"
        );

        assertThatThrownBy(() -> factory.createKernelContext(authentication, null, true, false))
            .isInstanceOf(KernelTenantResolutionException.class)
            .extracting(exception -> ((KernelTenantResolutionException) exception).failure())
            .isEqualTo(KernelTenantResolutionFailure.EXPLICIT_TENANT_SELECTION_REQUIRED);
    }

    @Test
    void allowsPrivilegedExplicitCrossTenantRouteSelectionOutsideGrantedScope() {
        KernelSecurityContextFactory factory = newFactory("roles", "ROLE_");

        JwtAuthenticationToken authentication = jwtAuthenticationToken(
            Map.of(
                "sub", "user-777",
                "tenant_id", "tenant-a",
                "roles", List.of("admin", "tenant_cross_access")
            ),
            List.of("ROLE_admin", KernelPrivilegedTenantAccessEvaluator.CROSS_TENANT_AUTHORITY),
            "privileged-user"
        );

        KernelSecurityTenantContext kernelContext = factory.createKernelContext(
            authentication,
            "tenant-z",
            true,
            true
        );

        assertThat(kernelContext.activeTenantId()).isEqualTo("tenant-z");
        assertThat(kernelContext.tenantScope().permittedTenantIds()).containsExactly("tenant-a");
        assertThat(kernelContext.privilegedCrossTenantAccess()).isTrue();
    }

    private static KernelSecurityContextFactory newFactory(String authorityClaim, String authorityPrefix) {
        GovarynKernelSecurityProperties properties = new GovarynKernelSecurityProperties();
        properties.setAuthorityClaim(authorityClaim);
        properties.setAuthorityPrefix(authorityPrefix);
        return new KernelSecurityContextFactory(
            new KernelSecurityIdentityResolver(),
            new KernelTenantScopeExtractor(),
            new KernelActiveTenantResolver(new KernelTenantAccessValidator()),
            properties
        );
    }

    private static JwtAuthenticationToken jwtAuthenticationToken(
        Map<String, Object> claims,
        List<String> authorities,
        String username
    ) {
        Jwt jwt = Jwt.withTokenValue("token-value")
            .header("alg", "RS256")
            .claims(map -> map.putAll(claims))
            .issuedAt(Instant.now().minusSeconds(10))
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

        List<GrantedAuthority> grantedAuthorities = authorities.stream()
            .map(SimpleGrantedAuthority::new)
            .map(GrantedAuthority.class::cast)
            .toList();

        return new JwtAuthenticationToken(jwt, grantedAuthorities, username);
    }
}
