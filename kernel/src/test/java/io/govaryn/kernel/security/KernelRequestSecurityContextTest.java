package io.govaryn.kernel.security;

import io.govaryn.kernel.config.GovarynKernelSecurityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KernelRequestSecurityContextTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsMappedContextForAuthenticatedRequest() {
        KernelRequestSecurityContext requestSecurityContext = new KernelRequestSecurityContext(newFactory());
        SecurityContextHolder.getContext().setAuthentication(jwtAuthenticationToken(
            Map.of("sub", "subject-1", "tenant_id", "tenant-a", "roles", List.of("admin")),
            List.of("ROLE_admin"),
            "alice"
        ));

        var current = requestSecurityContext.current();

        assertThat(current).isPresent();
        assertThat(current.get().userId()).isEqualTo("subject-1");
        assertThat(current.get().tenantId()).isEqualTo("tenant-a");
        assertThat(current.get().globalRoles()).containsExactly("ROLE_admin");
    }

    @Test
    void exposesKernelSecurityTenantContextForAuthenticatedRequest() {
        KernelRequestSecurityContext requestSecurityContext = new KernelRequestSecurityContext(newFactory());
        SecurityContextHolder.getContext().setAuthentication(jwtAuthenticationToken(
            Map.of("sub", "subject-1", "tenant_id", "tenant-a", "roles", List.of("admin")),
            List.of("ROLE_admin"),
            "alice"
        ));

        var current = requestSecurityContext.currentKernelContext();

        assertThat(current).isPresent();
        assertThat(current.get().userId()).isEqualTo("subject-1");
        assertThat(current.get().tenantScope().permittedTenantIds()).containsExactly("tenant-a");
        assertThat(current.get().activeTenantId()).isEqualTo("tenant-a");
    }

    @Test
    void returnsEmptyWhenNoAuthenticationExists() {
        KernelRequestSecurityContext requestSecurityContext = new KernelRequestSecurityContext(newFactory());

        assertThat(requestSecurityContext.current()).isEmpty();
    }

    private static KernelSecurityContextFactory newFactory() {
        GovarynKernelSecurityProperties properties = new GovarynKernelSecurityProperties();
        properties.setAuthorityClaim("roles");
        properties.setAuthorityPrefix("ROLE_");
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

        return new JwtAuthenticationToken(
            jwt,
            authorities.stream().map(SimpleGrantedAuthority::new).toList(),
            username
        );
    }
}
