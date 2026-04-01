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

    private static KernelSecurityContextFactory newFactory(String authorityClaim, String authorityPrefix) {
        GovarynKernelSecurityProperties properties = new GovarynKernelSecurityProperties();
        properties.setAuthorityClaim(authorityClaim);
        properties.setAuthorityPrefix(authorityPrefix);
        return new KernelSecurityContextFactory(new KernelSecurityIdentityResolver(), properties);
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
