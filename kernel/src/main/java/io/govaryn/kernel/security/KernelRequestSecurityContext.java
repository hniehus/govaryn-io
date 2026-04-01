package io.govaryn.kernel.security;

import io.govaryn.kernel.security.authorization.framework.model.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Request-scoped access to the normalized kernel {@link SecurityContext}.
 */
@Component
public class KernelRequestSecurityContext {

    private static final Logger log = LoggerFactory.getLogger(KernelRequestSecurityContext.class);

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
            log.warn(
                "event=security_context_mapping_failed authenticationType={} errorType={}",
                authentication.getClass().getSimpleName(),
                ex.getClass().getSimpleName()
            );
            return Optional.empty();
        }
    }
}
