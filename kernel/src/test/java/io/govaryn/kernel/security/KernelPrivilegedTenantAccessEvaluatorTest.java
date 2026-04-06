package io.govaryn.kernel.security;

import io.govaryn.kernel.config.GovarynKernelSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KernelPrivilegedTenantAccessEvaluatorTest {

    @Test
    void detectsPrivilegedCrossTenantAuthority() {
        KernelPrivilegedTenantAccessEvaluator evaluator = evaluatorWithPrefix("ROLE_");
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
            "user",
            "credentials",
            List.of(
                new SimpleGrantedAuthority("ROLE_admin"),
                new SimpleGrantedAuthority(KernelPrivilegedTenantAccessEvaluator.CROSS_TENANT_AUTHORITY)
            )
        );

        assertThat(evaluator.hasPrivilegedCrossTenantAccess(authentication)).isTrue();
    }

    @Test
    void returnsFalseWhenPrivilegedCrossTenantAuthorityIsMissing() {
        KernelPrivilegedTenantAccessEvaluator evaluator = evaluatorWithPrefix("ROLE_");
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
            "user",
            "credentials",
            List.of(
                new SimpleGrantedAuthority("ROLE_admin"),
                new SimpleGrantedAuthority("ROLE_support")
            )
        );

        assertThat(evaluator.hasPrivilegedCrossTenantAccess(authentication)).isFalse();
    }

    @Test
    void honorsConfiguredAuthorityPrefixForPrivilegedCrossTenantAuthority() {
        KernelPrivilegedTenantAccessEvaluator evaluator = evaluatorWithPrefix("SCOPE_");
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
            "user",
            "credentials",
            List.of(new SimpleGrantedAuthority("SCOPE_tenant_cross_access"))
        );

        assertThat(evaluator.hasPrivilegedCrossTenantAccess(authentication)).isTrue();
    }

    private static KernelPrivilegedTenantAccessEvaluator evaluatorWithPrefix(String authorityPrefix) {
        GovarynKernelSecurityProperties properties = new GovarynKernelSecurityProperties();
        properties.setAuthorityPrefix(authorityPrefix);
        return new KernelPrivilegedTenantAccessEvaluator(properties);
    }
}
