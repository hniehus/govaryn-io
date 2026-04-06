package io.govaryn.kernel.security;

import io.govaryn.kernel.security.authorization.framework.model.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Request-scoped access to kernel-managed security contexts.
 */
@Component
public class KernelRequestSecurityContext {

    public static final String KERNEL_CONTEXT_REQUEST_ATTRIBUTE = KernelRequestSecurityContext.class.getName()
        + ".KERNEL_CONTEXT";

    private static final Logger log = LoggerFactory.getLogger(KernelRequestSecurityContext.class);

    private final KernelSecurityContextFactory securityContextFactory;

    public KernelRequestSecurityContext(KernelSecurityContextFactory securityContextFactory) {
        this.securityContextFactory = securityContextFactory;
    }

    public Optional<KernelSecurityTenantContext> currentKernelContext() {
        Optional<KernelSecurityTenantContext> cached = resolveCachedKernelContext();
        if (cached.isPresent()) {
            return cached;
        }

        Authentication authentication = currentAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }

        try {
            KernelSecurityTenantContext kernelContext = securityContextFactory.createKernelContext(authentication);
            storeCurrentKernelContext(kernelContext);
            return Optional.of(kernelContext);
        } catch (IllegalArgumentException ex) {
            log.warn(
                "event=security_context_mapping_failed authenticationType={} errorType={}",
                authentication.getClass().getSimpleName(),
                ex.getClass().getSimpleName()
            );
            return Optional.empty();
        }
    }

    public Optional<SecurityContext> current() {
        return currentKernelContext().map(securityContextFactory::toAuthorizationSecurityContext);
    }

    void storeCurrentKernelContext(KernelSecurityTenantContext kernelContext) {
        if (kernelContext == null) {
            throw new IllegalArgumentException("kernelContext must not be null");
        }
        ServletRequestAttributes requestAttributes = currentRequestAttributes();
        if (requestAttributes == null) {
            return;
        }
        requestAttributes.getRequest().setAttribute(KERNEL_CONTEXT_REQUEST_ATTRIBUTE, kernelContext);
    }

    private Optional<KernelSecurityTenantContext> resolveCachedKernelContext() {
        ServletRequestAttributes requestAttributes = currentRequestAttributes();
        if (requestAttributes == null) {
            return Optional.empty();
        }
        Object cached = requestAttributes.getRequest().getAttribute(KERNEL_CONTEXT_REQUEST_ATTRIBUTE);
        if (!(cached instanceof KernelSecurityTenantContext kernelContext)) {
            return Optional.empty();
        }
        return Optional.of(kernelContext);
    }

    private Authentication currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication;
    }

    private ServletRequestAttributes currentRequestAttributes() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes requestAttributes)) {
            return null;
        }
        return requestAttributes;
    }
}
