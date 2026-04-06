package io.govaryn.kernel.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KernelTenantScopeExtractorTest {

    private final KernelTenantScopeExtractor extractor = new KernelTenantScopeExtractor();

    @Test
    void extractsMultiTenantScopeFromTokenClaimsWithDeterministicNormalization() {
        KernelTenantScopeExtraction extraction = extractor.extract(jwtAuthenticationToken(
            Map.of(
                "sub", "user-1",
                "tenant_ids", List.of("tenant-b", " tenant-a ", "tenant-b"),
                "tenant_id", "tenant-single-fallback"
            )
        ));

        assertThat(extraction.sourceClaimKey()).isEqualTo("tenant_ids");
        assertThat(extraction.tenantScope().permittedTenantIds()).containsExactly("tenant-a", "tenant-b");
    }

    @Test
    void fallsBackToSingleTenantClaimWhenMultiTenantClaimIsMissing() {
        KernelTenantScopeExtraction extraction = extractor.extractFromClaims(
            Map.of("sub", "user-2", "tid", " tenant-z ")
        );

        assertThat(extraction.sourceClaimKey()).isEqualTo("tid");
        assertThat(extraction.tenantScope().permittedTenantIds()).containsExactly("tenant-z");
    }

    @Test
    void returnsEmptyScopeWhenTenantClaimsAreAbsent() {
        KernelTenantScopeExtraction extraction = extractor.extractFromClaims(
            Map.of("sub", "user-3", "scope", "records:read")
        );

        assertThat(extraction.sourceClaimKey()).isNull();
        assertThat(extraction.tenantScope().isEmpty()).isTrue();
    }

    private static JwtAuthenticationToken jwtAuthenticationToken(Map<String, Object> claims) {
        Jwt jwt = Jwt.withTokenValue("token-value")
            .header("alg", "RS256")
            .claims(map -> map.putAll(claims))
            .issuedAt(Instant.now().minusSeconds(10))
            .expiresAt(Instant.now().plusSeconds(300))
            .build();

        return new JwtAuthenticationToken(jwt, List.of(), jwt.getSubject());
    }
}
