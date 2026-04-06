package io.govaryn.kernel.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Establishes kernel-managed security and tenant context once per authenticated request.
 */
@Component
public class KernelRequestTenantContextInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(KernelRequestTenantContextInterceptor.class);

    private final KernelRequestSecurityContext requestSecurityContext;
    private final KernelSecurityContextFactory securityContextFactory;
    private final KernelRouteTenantSelectionResolver routeTenantSelectionResolver;
    private final KernelPrivilegedTenantAccessEvaluator privilegedTenantAccessEvaluator;

    public KernelRequestTenantContextInterceptor(
        KernelRequestSecurityContext requestSecurityContext,
        KernelSecurityContextFactory securityContextFactory,
        KernelRouteTenantSelectionResolver routeTenantSelectionResolver,
        KernelPrivilegedTenantAccessEvaluator privilegedTenantAccessEvaluator
    ) {
        this.requestSecurityContext = requestSecurityContext;
        this.securityContextFactory = securityContextFactory;
        this.routeTenantSelectionResolver = routeTenantSelectionResolver;
        this.privilegedTenantAccessEvaluator = privilegedTenantAccessEvaluator;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getAttribute(KernelRequestSecurityContext.KERNEL_CONTEXT_REQUEST_ATTRIBUTE)
            instanceof KernelSecurityTenantContext) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)
            || !jwtAuthenticationToken.isAuthenticated()) {
            return true;
        }

        KernelRouteTenantSelection routeTenantSelection = routeTenantSelectionResolver.resolve(request, handler);
        boolean privilegedCrossTenantAccess = privilegedTenantAccessEvaluator
            .hasPrivilegedCrossTenantAccess(jwtAuthenticationToken);
        try {
            KernelSecurityTenantContext kernelContext = securityContextFactory.createKernelContext(
                jwtAuthenticationToken,
                routeTenantSelection.routeTenantId(),
                routeTenantSelection.tenantProtectedOperation(),
                privilegedCrossTenantAccess
            );
            requestSecurityContext.storeCurrentKernelContext(kernelContext);
        } catch (KernelTenantResolutionException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            log.warn(
                "event=security_context_preload_failed authenticationType={} errorType={}",
                jwtAuthenticationToken.getClass().getSimpleName(),
                ex.getClass().getSimpleName()
            );
        }
        return true;
    }
}
