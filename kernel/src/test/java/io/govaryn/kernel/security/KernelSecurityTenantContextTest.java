package io.govaryn.kernel.security;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KernelSecurityTenantContextTest {

    @Test
    void rejectsActiveTenantOutsideGrantedScopeWhenCrossTenantPrivilegeIsNotPresent() {
        KernelSecurityIdentity principal = new KernelSecurityIdentity(
            "user-1",
            null,
            "alice",
            List.of("ROLE_admin")
        );
        KernelTenantScope tenantScope = new KernelTenantScope(List.of("tenant-a"));
        KernelActiveTenantContext activeTenant = new KernelActiveTenantContext("tenant-b");

        assertThatThrownBy(() -> new KernelSecurityTenantContext(
            principal,
            tenantScope,
            activeTenant,
            false,
            Map.of(),
            Map.of()
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("activeTenant");
    }

    @Test
    void allowsActiveTenantOutsideGrantedScopeWhenCrossTenantPrivilegeIsPresent() {
        KernelSecurityIdentity principal = new KernelSecurityIdentity(
            "user-1",
            null,
            "alice",
            List.of("ROLE_admin", KernelPrivilegedTenantAccessEvaluator.CROSS_TENANT_AUTHORITY)
        );
        KernelTenantScope tenantScope = new KernelTenantScope(List.of("tenant-a"));
        KernelActiveTenantContext activeTenant = new KernelActiveTenantContext("tenant-b");

        KernelSecurityTenantContext context = new KernelSecurityTenantContext(
            principal,
            tenantScope,
            activeTenant,
            true,
            Map.of(),
            Map.of()
        );

        assertThat(context.activeTenantId()).isEqualTo("tenant-b");
        assertThat(context.tenantScope().permittedTenantIds()).containsExactly("tenant-a");
    }

    @Test
    void exposesPrincipalAndActiveTenantConvenienceAccessors() {
        KernelSecurityIdentity principal = new KernelSecurityIdentity(
            "user-1",
            "https://issuer.example",
            "alice",
            List.of("ROLE_support", "ROLE_admin")
        );
        KernelTenantScope tenantScope = new KernelTenantScope(List.of("tenant-a"));
        KernelActiveTenantContext activeTenant = new KernelActiveTenantContext("tenant-a");

        KernelSecurityTenantContext context = new KernelSecurityTenantContext(
            principal,
            tenantScope,
            activeTenant,
            false,
            Map.of("scope", "records:read"),
            Map.of("authenticationType", "JwtAuthenticationToken")
        );

        assertThat(context.userId()).isEqualTo("user-1");
        assertThat(context.authorities()).containsExactly("ROLE_admin", "ROLE_support");
        assertThat(context.activeTenantId()).isEqualTo("tenant-a");
    }
}
