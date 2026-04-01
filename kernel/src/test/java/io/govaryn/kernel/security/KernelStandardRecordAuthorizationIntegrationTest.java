package io.govaryn.kernel.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.util.JsonPathExpectationsHelper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Import(KernelStandardRecordAuthorizationIntegrationTest.TestSecurityBeans.class)
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
class KernelStandardRecordAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Allowed token can execute standard read/write paths")
    void allowedTokenCanExecuteStandardReadWritePaths() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/kernel/records")
                .header("Authorization", "Bearer allow-token")
                .contentType(APPLICATION_JSON)
                .content("{\"value\":\"first\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.value").value("first"))
            .andReturn();

        String recordId = new JsonPathExpectationsHelper("$.id")
            .evaluateJsonPath(createResult.getResponse().getContentAsString(), String.class);

        mockMvc.perform(get("/api/kernel/records/" + recordId).header("Authorization", "Bearer allow-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(recordId))
            .andExpect(jsonPath("$.value").value("first"));

        mockMvc.perform(get("/api/kernel/records").header("Authorization", "Bearer allow-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());

        mockMvc.perform(put("/api/kernel/records/" + recordId)
                .header("Authorization", "Bearer allow-token")
                .contentType(APPLICATION_JSON)
                .content("{\"value\":\"updated\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(recordId))
            .andExpect(jsonPath("$.value").value("updated"));

        mockMvc.perform(delete("/api/kernel/records/" + recordId).header("Authorization", "Bearer allow-token"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/kernel/records/" + recordId).header("Authorization", "Bearer allow-token"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Denied access returns consistent 403 and enforcement happens before write")
    void deniedAccessReturnsConsistent403AndEnforcementHappensBeforeWrite() throws Exception {
        int beforeCount = listCount("allow-token");

        mockMvc.perform(get("/api/kernel/records").header("Authorization", "Bearer deny-token"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.message").value("Forbidden"))
            .andExpect(jsonPath("$.reason").value("ACCESS_DENIED"));

        mockMvc.perform(post("/api/kernel/records")
                .header("Authorization", "Bearer deny-token")
                .contentType(APPLICATION_JSON)
                .content("{\"value\":\"blocked\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.message").value("Forbidden"))
            .andExpect(jsonPath("$.reason").value("ACCESS_DENIED"));

        int afterCount = listCount("allow-token");
        assertThat(afterCount).isEqualTo(beforeCount);
    }

    private int listCount(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/kernel/records")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
        List<?> root = new JsonPathExpectationsHelper("$")
            .evaluateJsonPath(result.getResponse().getContentAsString(), List.class);
        return root.size();
    }

    @TestConfiguration
    static class TestSecurityBeans {
        @Bean("kernelJwtDecoder")
        @Primary
        JwtDecoder kernelJwtDecoder() {
            return token -> switch (token) {
                case "allow-token" -> jwt(
                    token,
                    "subject-allow",
                    "https://idp.example.com/realms/main",
                    List.of("govaryn-kernel"),
                    "alice",
                    List.of("admin"),
                    "kernel.records.read kernel.records.write",
                    "tenant-1"
                );
                case "deny-token" -> jwt(
                    token,
                    "subject-deny",
                    "https://idp.example.com/realms/main",
                    List.of("govaryn-kernel"),
                    "bob",
                    List.of("viewer"),
                    "module.status:read",
                    "tenant-2"
                );
                default -> jwt(
                    token,
                    "subject-default",
                    "https://idp.example.com/realms/main",
                    List.of("govaryn-kernel"),
                    "default-user",
                    List.of("viewer"),
                    "",
                    "tenant-3"
                );
            };
        }

        private static Jwt jwt(
            String token,
            String subject,
            String issuer,
            List<String> audience,
            String username,
            List<String> roles,
            String scope,
            String tenantId
        ) {
            return Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", subject)
                .claim("iss", issuer)
                .claim("aud", audience)
                .claim("preferred_username", username)
                .claim("roles", roles)
                .claim("scope", scope)
                .claim("tenant_id", tenantId)
                .issuedAt(Instant.now().minusSeconds(5))
                .notBefore(Instant.now().minusSeconds(5))
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        }
    }
}
