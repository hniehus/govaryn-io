package io.govaryn.kernel.security;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KernelWhoAmIController {

    private final KernelSecurityIdentityResolver securityIdentityResolver;

    public KernelWhoAmIController(KernelSecurityIdentityResolver securityIdentityResolver) {
        this.securityIdentityResolver = securityIdentityResolver;
    }

    @GetMapping("/api/kernel/whoami")
    public KernelSecurityIdentity whoAmI(Authentication authentication) {
        return securityIdentityResolver.resolve(authentication);
    }
}
