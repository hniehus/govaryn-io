package io.govaryn.kernel.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KernelTenantScopeTest {

    @Test
    void normalizesTenantIdsDeterministically() {
        KernelTenantScope scope = new KernelTenantScope(List.of(" tenant-b ", "tenant-a", "tenant-b", "", "  "));

        assertThat(scope.permittedTenantIds()).containsExactly("tenant-a", "tenant-b");
    }

    @Test
    void reportsSingleTenantWhenExactlyOneTenantIsPresent() {
        KernelTenantScope scope = new KernelTenantScope(List.of("tenant-only"));

        assertThat(scope.hasSingleTenant()).isTrue();
        assertThat(scope.singleTenantId()).contains("tenant-only");
        assertThat(scope.permits("tenant-only")).isTrue();
        assertThat(scope.permits("tenant-other")).isFalse();
    }
}
