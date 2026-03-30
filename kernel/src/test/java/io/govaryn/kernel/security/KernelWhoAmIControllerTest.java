package io.govaryn.kernel.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("KernelWhoAmIController")
class KernelWhoAmIControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
        new KernelWhoAmIController(new KernelSecurityIdentityResolver())
    ).build();

    @Test
    @DisplayName("Should return unauthorized when principal is not JWT authentication")
    void shouldReturnUnauthorizedWhenPrincipalIsNotJwtAuthentication() throws Exception {
        mockMvc.perform(
                get("/api/kernel/whoami").principal(new TestingAuthenticationToken("user", "pw"))
            )
            .andExpect(status().isUnauthorized());
    }
}
