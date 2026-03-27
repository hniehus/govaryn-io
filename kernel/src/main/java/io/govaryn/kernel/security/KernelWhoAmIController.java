package io.govaryn.kernel.security;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class KernelWhoAmIController {

    @GetMapping("/api/kernel/whoami")
    public Map<String, String> whoAmI(Authentication authentication) {
        return Map.of("subject", authentication.getName());
    }
}
