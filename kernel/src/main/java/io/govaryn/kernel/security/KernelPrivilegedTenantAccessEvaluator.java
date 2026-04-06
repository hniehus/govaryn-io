package io.govaryn.kernel.security;

import io.govaryn.kernel.config.GovarynKernelSecurityProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Determines whether the authenticated principal holds the explicit authority required
 * for privileged cross-tenant access.
 */
@Component
public class KernelPrivilegedTenantAccessEvaluator {

    static final String CROSS_TENANT_AUTHORITY = "ROLE_tenant_cross_access";
    private static final String CROSS_TENANT_AUTHORITY_SUFFIX = "tenant_cross_access";

    private final GovarynKernelSecurityProperties securityProperties;

    public KernelPrivilegedTenantAccessEvaluator(GovarynKernelSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public boolean hasPrivilegedCrossTenantAccess(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority != null && !authority.isBlank())
            .map(String::trim)
            .anyMatch(this::matchesPrivilegedCrossTenantAuthority);
    }

    private boolean matchesPrivilegedCrossTenantAuthority(String authority) {
        if (CROSS_TENANT_AUTHORITY.equals(authority)) {
            return true;
        }
        String prefix = securityProperties.getAuthorityPrefix() == null
            ? ""
            : securityProperties.getAuthorityPrefix();
        return (prefix + CROSS_TENANT_AUTHORITY_SUFFIX).equals(authority);
    }
}
