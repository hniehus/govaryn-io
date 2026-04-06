package io.govaryn.kernel.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Import(ModuleStatusAuthorizationIntegrationTest.TestSecurityBeans.class)
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
class ModuleStatusAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestKernelSecurityIdentityResolver securityIdentityResolver;

    @BeforeEach
    void resetIdentityResolverMode() {
        securityIdentityResolver.setMode(TestKernelSecurityIdentityResolver.Mode.VALID);
    }

    @Test
    void missingAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/modules/status"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void permittedRequestReachesBusinessLogic() throws Exception {
        securityIdentityResolver.setMode(TestKernelSecurityIdentityResolver.Mode.VALID);

        mockMvc.perform(get("/modules/status").header("Authorization", "Bearer read-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.modules").isArray());
    }

    @Test
    void deniedRequestReturnsForbidden() throws Exception {
        securityIdentityResolver.setMode(TestKernelSecurityIdentityResolver.Mode.VALID);

        mockMvc.perform(get("/modules/status").header("Authorization", "Bearer no-scope-token"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.reason").value("ACCESS_DENIED"));
    }

    @Test
    void missingSubjectFailsSafelyWithUnauthorized() throws Exception {
        securityIdentityResolver.setMode(TestKernelSecurityIdentityResolver.Mode.THROW);

        mockMvc.perform(get("/modules/status").header("Authorization", "Bearer read-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidSubjectInputFailsSafelyWithUnauthorized() throws Exception {
        securityIdentityResolver.setMode(TestKernelSecurityIdentityResolver.Mode.BLANK_SUBJECT);

        mockMvc.perform(get("/modules/status").header("Authorization", "Bearer read-token"))
            .andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    static class TestSecurityBeans {

        @Bean
        @Primary
        TestKernelSecurityIdentityResolver testKernelSecurityIdentityResolver() {
            return new TestKernelSecurityIdentityResolver();
        }

        @Bean("kernelJwtDecoder")
        JwtDecoder kernelJwtDecoder() {
            return token -> switch (token) {
                case "read-token" -> jwt(
                    token,
                    "subject-1",
                    "https://idp.example.com/realms/main",
                    "alice",
                    "tenant-1",
                    "module.status:read",
                    java.util.List.of("admin")
                );
                case "no-scope-token" -> jwt(
                    token,
                    "subject-2",
                    "https://idp.example.com/realms/main",
                    "bob",
                    "tenant-1",
                    "",
                    java.util.List.of("viewer")
                );
                default -> jwt(
                    token,
                    "subject-default",
                    "https://idp.example.com/realms/main",
                    "default-user",
                    "tenant-1",
                    "",
                    java.util.List.of("viewer")
                );
            };
        }

        private static Jwt jwt(
            String token,
            String subject,
            String issuer,
            String username,
            String tenantId,
            String scope,
            java.util.List<String> roles
        ) {
            return Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", subject)
                .claim("iss", issuer)
                .claim("aud", java.util.List.of("govaryn-kernel"))
                .claim("preferred_username", username)
                .claim("tenant_id", tenantId)
                .claim("scope", scope)
                .claim("roles", roles)
                .issuedAt(Instant.now().minusSeconds(5))
                .notBefore(Instant.now().minusSeconds(5))
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        }
    }

    static class TestKernelSecurityIdentityResolver extends KernelSecurityIdentityResolver {
        enum Mode { VALID, THROW, BLANK_SUBJECT }

        private volatile Mode mode = Mode.VALID;

        void setMode(Mode mode) {
            this.mode = mode;
        }

        @Override
        public KernelSecurityIdentity resolve(Authentication authentication) {
            return switch (mode) {
                case THROW -> throw new IllegalArgumentException("missing subject");
                case BLANK_SUBJECT -> new KernelSecurityIdentity(
                    " ",
                    "https://idp.example.com/realms/main",
                    "alice",
                    java.util.List.of("ROLE_admin")
                );
                case VALID -> super.resolve(authentication);
            };
        }
    }
}
