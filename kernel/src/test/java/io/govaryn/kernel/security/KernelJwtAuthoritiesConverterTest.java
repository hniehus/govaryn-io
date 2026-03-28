package io.govaryn.kernel.security;

import io.govaryn.kernel.config.GovarynKernelSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KernelJwtAuthoritiesConverterTest {

    @Test
    void derivesAuthoritiesFromConfiguredClaimAndPrefix() {
        GovarynKernelSecurityProperties properties = new GovarynKernelSecurityProperties();
        properties.setAuthorityClaim("roles");
        properties.setAuthorityPrefix("ROLE_");
        KernelJwtAuthoritiesConverter converter = new KernelJwtAuthoritiesConverter(properties);

        Jwt jwt = jwtWithClaims(Map.of("roles", List.of("admin", "support")));

        assertThat(converter.convert(jwt))
            .extracting(authority -> authority.getAuthority())
            .containsExactly("ROLE_admin", "ROLE_support");
    }

    @Test
    void returnsEmptyAuthoritiesWhenClaimIsAbsent() {
        GovarynKernelSecurityProperties properties = new GovarynKernelSecurityProperties();
        properties.setAuthorityClaim("roles");
        properties.setAuthorityPrefix("ROLE_");
        KernelJwtAuthoritiesConverter converter = new KernelJwtAuthoritiesConverter(properties);

        Jwt jwt = jwtWithClaims(Map.of("sub", "user-1"));

        assertThat(converter.convert(jwt)).isEmpty();
    }

    private Jwt jwtWithClaims(Map<String, Object> claims) {
        return Jwt.withTokenValue("token-value")
            .header("alg", "RS256")
            .claims(map -> map.putAll(claims))
            .issuedAt(Instant.now().minusSeconds(10))
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
    }
}
