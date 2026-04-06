package io.govaryn.kernel.security;

import io.govaryn.kernel.security.authorization.framework.model.SecurityContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Import({
    KernelTenantContextRequestFlowIntegrationTest.TenantContextFixtureConfiguration.class,
    KernelTenantContextRequestFlowIntegrationTest.TestSecurityBeans.class
})
@SpringBootTest(properties = {
    "govaryn.kernel.id=test-kernel",
    "govaryn.kernel.environment=dev",
    "spring.application.version=1.2.0",
    "spring.main.allow-bean-definition-overriding=true",
    "govaryn.kernel.security.enabled=true",
    "govaryn.kernel.security.issuer-uri=https://idp.example.com/realms/main",
    "govaryn.kernel.security.audience=govaryn-kernel",
    "govaryn.kernel.security.authority-claim=roles",
    "govaryn.kernel.security.authority-prefix=ROLE_",
    "govaryn.kernel.security.public-paths[0]=/health"
})
class KernelTenantContextRequestFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Missing authentication returns 401")
    void missingAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/kernel/test/tenant-context/tenants/tenant-1/protected"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Invalid authentication returns 401")
    void invalidAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/kernel/test/tenant-context/tenants/tenant-1/protected")
                .header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Route tenant selection establishes one active kernel context for the request")
    void routeTenantSelectionEstablishesOneActiveKernelContextForRequest() throws Exception {
        mockMvc.perform(get("/api/kernel/test/tenant-context/tenants/tenant-2/protected")
                .header("Authorization", "Bearer multi-tenant-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("subject-multi"))
            .andExpect(jsonPath("$.activeTenantId").value("tenant-2"))
            .andExpect(jsonPath("$.authorizationTenantId").value("tenant-2"))
            .andExpect(jsonPath("$.tenantScope[0]").value("tenant-1"))
            .andExpect(jsonPath("$.tenantScope[1]").value("tenant-2"))
            .andExpect(jsonPath("$.sameContextInstance").value(true));
    }

    @Test
    @DisplayName("Explicit route tenant outside token scope returns 403")
    void explicitRouteTenantOutsideTokenScopeReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/kernel/test/tenant-context/tenants/tenant-2/protected")
                .header("Authorization", "Bearer single-tenant-token"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.reason").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Privileged authority allows explicit cross-tenant route selection")
    void privilegedAuthorityAllowsExplicitCrossTenantRouteSelection() throws Exception {
        mockMvc.perform(get("/api/kernel/test/tenant-context/tenants/tenant-2/protected")
                .header("Authorization", "Bearer privileged-cross-tenant-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("subject-privileged"))
            .andExpect(jsonPath("$.activeTenantId").value("tenant-2"))
            .andExpect(jsonPath("$.authorizationTenantId").value("tenant-2"))
            .andExpect(jsonPath("$.tenantScope[0]").value("tenant-1"))
            .andExpect(jsonPath("$.sameContextInstance").value(true));
    }

    @Test
    @DisplayName("Tenant-protected endpoint without route tenant denies multi-tenant tokens with 403")
    void tenantProtectedEndpointWithoutRouteTenantDeniesMultiTenantTokens() throws Exception {
        mockMvc.perform(get("/api/kernel/test/tenant-context/protected-no-route")
                .header("Authorization", "Bearer multi-tenant-token"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.reason").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Single-tenant token without explicit route tenant resolves active tenant from scope")
    void singleTenantTokenWithoutExplicitRouteTenantResolvesActiveTenantFromScope() throws Exception {
        mockMvc.perform(get("/api/kernel/test/tenant-context/protected-no-route")
                .header("Authorization", "Bearer single-tenant-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activeTenantId").value("tenant-1"))
            .andExpect(jsonPath("$.authorizationTenantId").value("tenant-1"))
            .andExpect(jsonPath("$.sameContextInstance").value(true));
    }

    @TestConfiguration
    static class TenantContextFixtureConfiguration {
        @Bean
        TenantContextFixtureController tenantContextFixtureController(
            KernelRequestSecurityContext requestSecurityContext
        ) {
            return new TenantContextFixtureController(requestSecurityContext);
        }
    }

    @RestController
    @RequestMapping("/api/kernel/test/tenant-context")
    static class TenantContextFixtureController {

        private final KernelRequestSecurityContext requestSecurityContext;

        TenantContextFixtureController(KernelRequestSecurityContext requestSecurityContext) {
            this.requestSecurityContext = requestSecurityContext;
        }

        @GetMapping("/tenants/{tenantId}/protected")
        ResponseEntity<TenantContextSnapshot> tenantProtected(@PathVariable String tenantId) {
            return ResponseEntity.ok(snapshot());
        }

        @KernelTenantProtectedOperation
        @GetMapping("/protected-no-route")
        ResponseEntity<TenantContextSnapshot> tenantProtectedWithoutRoute() {
            return ResponseEntity.ok(snapshot());
        }

        private TenantContextSnapshot snapshot() {
            KernelSecurityTenantContext first = requestSecurityContext.currentKernelContext()
                .orElseThrow(() -> new IllegalStateException("Kernel context missing"));
            KernelSecurityTenantContext second = requestSecurityContext.currentKernelContext()
                .orElseThrow(() -> new IllegalStateException("Kernel context missing"));
            SecurityContext authorizationSecurityContext = requestSecurityContext.current()
                .orElseThrow(() -> new IllegalStateException("Authorization context missing"));

            return new TenantContextSnapshot(
                first.userId(),
                first.activeTenantId(),
                first.tenantScope().permittedTenantIds(),
                authorizationSecurityContext.tenantId(),
                first == second
            );
        }
    }

    record TenantContextSnapshot(
        String userId,
        String activeTenantId,
        List<String> tenantScope,
        String authorizationTenantId,
        boolean sameContextInstance
    ) {
    }

    @TestConfiguration
    static class TestSecurityBeans {
        @Bean("kernelJwtDecoder")
        @Primary
        JwtDecoder kernelJwtDecoder() {
            return token -> switch (token) {
                case "single-tenant-token" -> jwt(
                    token,
                    "subject-single",
                    "single-user",
                    List.of("viewer"),
                    "tenant-1"
                );
                case "multi-tenant-token" -> jwt(
                    token,
                    "subject-multi",
                    "multi-user",
                    List.of("viewer"),
                    List.of("tenant-1", "tenant-2")
                );
                case "privileged-cross-tenant-token" -> jwt(
                    token,
                    "subject-privileged",
                    "privileged-user",
                    List.of("viewer", "tenant_cross_access"),
                    "tenant-1"
                );
                default -> throw new JwtException("invalid token");
            };
        }

        private static Jwt jwt(
            String token,
            String subject,
            String username,
            List<String> roles,
            String tenantId
        ) {
            return Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", subject)
                .claim("iss", "https://idp.example.com/realms/main")
                .claim("aud", List.of("govaryn-kernel"))
                .claim("preferred_username", username)
                .claim("roles", roles)
                .claim("tenant_id", tenantId)
                .issuedAt(Instant.now().minusSeconds(5))
                .notBefore(Instant.now().minusSeconds(5))
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        }

        private static Jwt jwt(
            String token,
            String subject,
            String username,
            List<String> roles,
            List<String> tenantIds
        ) {
            return Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", subject)
                .claim("iss", "https://idp.example.com/realms/main")
                .claim("aud", List.of("govaryn-kernel"))
                .claim("preferred_username", username)
                .claim("roles", roles)
                .claim("tenant_ids", tenantIds)
                .issuedAt(Instant.now().minusSeconds(5))
                .notBefore(Instant.now().minusSeconds(5))
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        }
    }
}
