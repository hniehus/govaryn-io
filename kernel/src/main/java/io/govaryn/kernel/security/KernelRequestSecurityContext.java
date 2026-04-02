package io.govaryn.kernel.security;

import io.govaryn.kernel.security.authorization.framework.model.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.function.Function;
import java.util.Optional;

/**
 * Request-scoped access to kernel-managed security contexts.
 */
@Component
public class KernelRequestSecurityContext {

    private static final Logger log = LoggerFactory.getLogger(KernelRequestSecurityContext.class);

    private final KernelSecurityContextFactory securityContextFactory;

    public KernelRequestSecurityContext(KernelSecurityContextFactory securityContextFactory) {
        this.securityContextFactory = securityContextFactory;
    }

    public Optional<KernelSecurityTenantContext> currentKernelContext() {
        return resolveCurrent(securityContextFactory::createKernelContext);
    }

    public Optional<SecurityContext> current() {
        return resolveCurrent(securityContextFactory::create);
    }

    private <T> Optional<T> resolveCurrent(Function<Authentication, T> mapper) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapper.apply(authentication));
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
