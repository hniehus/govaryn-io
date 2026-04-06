package io.govaryn.kernel.security;

import io.govaryn.kernel.api.KernelCurrentSecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
public class KernelWhoAmIController {

    private final KernelCurrentSecurityContext currentSecurityContext;

    public KernelWhoAmIController(KernelCurrentSecurityContext currentSecurityContext) {
        this.currentSecurityContext = currentSecurityContext;
    }

    @GetMapping("/api/kernel/whoami")
    public KernelSecurityIdentity whoAmI() {
        return currentSecurityContext.currentPrincipal()
            .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Unauthorized"));
    }
}
