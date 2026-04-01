package io.govaryn.kernel.security;

import io.govaryn.kernel.security.authorization.framework.model.SecurityContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Request-scoped access to the normalized kernel {@link SecurityContext}.
 */
@Component
public class KernelRequestSecurityContext {

    private final KernelSecurityContextFactory securityContextFactory;

    public KernelRequestSecurityContext(KernelSecurityContextFactory securityContextFactory) {
        this.securityContextFactory = securityContextFactory;
    }

    public Optional<SecurityContext> current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        try {
            return Optional.of(securityContextFactory.create(authentication));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
