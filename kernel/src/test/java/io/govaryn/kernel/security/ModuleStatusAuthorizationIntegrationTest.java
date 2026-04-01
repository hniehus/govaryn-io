package io.govaryn.kernel.security;

import io.govaryn.kernel.api.KernelAuthorizationService;
import io.govaryn.kernel.api.KernelAuthorizationOperation;
import io.govaryn.kernel.security.authorization.model.AuthorizationDecision;
import io.govaryn.kernel.security.authorization.model.AuthorizationDecisionResult;
import io.govaryn.kernel.security.authorization.model.AuthorizationSubject;
import io.govaryn.kernel.security.authorization.model.DecisionReasonCode;
import org.junit.jupiter.api.Test;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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
    private TestKernelAuthorizationService kernelAuthorizationService;

    @Autowired
    private TestKernelSecurityIdentityResolver securityIdentityResolver;

    @Test
    void missingAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/modules/status"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void permittedRequestReachesBusinessLogic() throws Exception {
        securityIdentityResolver.setMode(TestKernelSecurityIdentityResolver.Mode.VALID);
        kernelAuthorizationService.setDecision(
            new AuthorizationDecision(AuthorizationDecisionResult.PERMIT, DecisionReasonCode.PERMIT_RULE_MATCHED, "permit-rule")
        );

        mockMvc.perform(get("/modules/status").header("Authorization", "Bearer test-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.modules").isArray());

        AuthorizationSubject capturedSubject = kernelAuthorizationService.lastSubject();
        assertThat(capturedSubject).isNotNull();
        assertThat(capturedSubject.subjectId()).isEqualTo("subject-1");
        assertThat(capturedSubject.roles()).containsExactly("ROLE_admin");
        assertThat(capturedSubject.attributes())
            .containsEntry("roles", "admin")
            .containsEntry("scope", "module.status:read module.status:write")
            .containsEntry("tenant_id", "tenant-1");
    }

    @Test
    void deniedRequestReturnsForbidden() throws Exception {
        securityIdentityResolver.setMode(TestKernelSecurityIdentityResolver.Mode.VALID);
        kernelAuthorizationService.setDecision(
            new AuthorizationDecision(AuthorizationDecisionResult.DENY, DecisionReasonCode.DENY_RULE_MATCHED, "deny-rule")
        );

        mockMvc.perform(get("/modules/status").header("Authorization", "Bearer test-token"))
            .andExpect(status().isForbidden());
    }

    @Test
    void missingSubjectFailsSafelyWithForbidden() throws Exception {
        securityIdentityResolver.setMode(TestKernelSecurityIdentityResolver.Mode.THROW);
        kernelAuthorizationService.setDecision(
            new AuthorizationDecision(AuthorizationDecisionResult.PERMIT, DecisionReasonCode.PERMIT_RULE_MATCHED, "permit-rule")
        );

        mockMvc.perform(get("/modules/status").header("Authorization", "Bearer test-token"))
            .andExpect(status().isForbidden());
    }

    @Test
    void invalidSubjectInputFailsSafelyWithForbidden() throws Exception {
        securityIdentityResolver.setMode(TestKernelSecurityIdentityResolver.Mode.BLANK_SUBJECT);
        kernelAuthorizationService.setDecision(
            new AuthorizationDecision(AuthorizationDecisionResult.PERMIT, DecisionReasonCode.PERMIT_RULE_MATCHED, "permit-rule")
        );

        mockMvc.perform(get("/modules/status").header("Authorization", "Bearer test-token"))
            .andExpect(status().isForbidden());
    }

    @TestConfiguration
    static class TestSecurityBeans {
        @Bean
        @Primary
        TestKernelAuthorizationService testKernelAuthorizationService() {
            return new TestKernelAuthorizationService();
        }

        @Bean
        @Primary
        TestKernelSecurityIdentityResolver testKernelSecurityIdentityResolver() {
            return new TestKernelSecurityIdentityResolver();
        }

        @Bean("kernelJwtDecoder")
        JwtDecoder kernelJwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", "subject-1")
                .claim("iss", "https://idp.example.com/realms/main")
                .claim("aud", java.util.List.of("govaryn-kernel"))
                .claim("preferred_username", "alice")
                .claim("tenant_id", "tenant-1")
                .claim("scope", "module.status:write module.status:read")
                .claim("roles", java.util.List.of("admin"))
                .issuedAt(Instant.now().minusSeconds(5))
                .notBefore(Instant.now().minusSeconds(5))
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        }

    }

    static class TestKernelAuthorizationService implements KernelAuthorizationService {
        private volatile AuthorizationDecision decision = new AuthorizationDecision(
            AuthorizationDecisionResult.PERMIT,
            DecisionReasonCode.PERMIT_RULE_MATCHED,
            "permit-rule"
        );
        private volatile AuthorizationSubject lastSubject;

        void setDecision(AuthorizationDecision decision) {
            this.decision = decision;
        }

        AuthorizationSubject lastSubject() {
            return lastSubject;
        }

        @Override
        public AuthorizationDecision authorize(AuthorizationSubject subject, KernelAuthorizationOperation operation) {
            this.lastSubject = subject;
            return decision;
        }

        @Override
        public AuthorizationDecision authorize(
            AuthorizationSubject subject,
            String action,
            String resourceType,
            String resourceId,
            Map<String, String> context
        ) {
            this.lastSubject = subject;
            return decision;
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
                case VALID -> new KernelSecurityIdentity(
                    "subject-1",
                    "https://idp.example.com/realms/main",
                    "alice",
                    java.util.List.of("ROLE_admin")
                );
            };
        }
    }
}
