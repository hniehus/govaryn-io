package io.govaryn.kernel.security;

import io.govaryn.kernel.api.KernelCurrentSecurityContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("KernelWhoAmIController")
class KernelWhoAmIControllerTest {

    @Test
    @DisplayName("Should return unauthorized when principal is not JWT authentication")
    void shouldReturnUnauthorizedWhenPrincipalIsNotJwtAuthentication() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new KernelWhoAmIController(() -> Optional.empty())
        ).build();

        mockMvc.perform(
                get("/api/kernel/whoami").principal(new TestingAuthenticationToken("user", "pw"))
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return ok when kernel current context provides principal")
    void shouldReturnOkWhenCurrentContextProvidesPrincipal() throws Exception {
        KernelCurrentSecurityContext currentSecurityContext = () -> Optional.of(
            new KernelSecurityTenantContext(
                new KernelSecurityIdentity(
                    "subject-1",
                    "https://idp.example.com/realms/main",
                    "alice",
                    java.util.List.of("ROLE_admin")
                ),
                new KernelTenantScope(java.util.List.of("tenant-a")),
                new KernelActiveTenantContext("tenant-a"),
                false,
                java.util.Map.of("scope", "module.status:read"),
                java.util.Map.of("authenticationType", "JwtAuthenticationToken")
            )
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new KernelWhoAmIController(currentSecurityContext)
        ).build();

        mockMvc.perform(get("/api/kernel/whoami"))
            .andExpect(status().isOk());
    }
}
