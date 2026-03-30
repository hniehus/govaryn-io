package io.govaryn.kernel.security;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
public class KernelWhoAmIController {

    private final KernelSecurityIdentityResolver securityIdentityResolver;

    public KernelWhoAmIController(KernelSecurityIdentityResolver securityIdentityResolver) {
        this.securityIdentityResolver = securityIdentityResolver;
    }

    @GetMapping("/api/kernel/whoami")
    public KernelSecurityIdentity whoAmI(Authentication authentication) {
        try {
            return securityIdentityResolver.resolve(authentication);
        } catch (Exception ex) {
            throw new ResponseStatusException(UNAUTHORIZED, "Unauthorized", ex);
        }
    }
}
