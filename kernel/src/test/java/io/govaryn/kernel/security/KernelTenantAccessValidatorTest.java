package io.govaryn.kernel.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KernelTenantAccessValidatorTest {

    private final KernelTenantAccessValidator validator = new KernelTenantAccessValidator();

    @Test
    void allowsExplicitTenantWithinScope() {
        KernelTenantScope scope = new KernelTenantScope(List.of("tenant-a", "tenant-b"));

        assertThatCode(() -> validator.validateExplicitTenantAccess(scope, "tenant-b", false))
            .doesNotThrowAnyException();
    }

    @Test
    void deniesExplicitTenantOutsideScopeWithoutPrivilege() {
        KernelTenantScope scope = new KernelTenantScope(List.of("tenant-a"));

        assertThatThrownBy(() -> validator.validateExplicitTenantAccess(scope, "tenant-z", false))
            .isInstanceOf(KernelTenantResolutionException.class)
            .extracting(exception -> ((KernelTenantResolutionException) exception).failure())
            .isEqualTo(KernelTenantResolutionFailure.REQUESTED_TENANT_NOT_PERMITTED);
    }

    @Test
    void allowsExplicitTenantOutsideScopeWhenPrivilegedCrossTenantAccessIsPresent() {
        KernelTenantScope scope = new KernelTenantScope(List.of("tenant-a"));

        assertThatCode(() -> validator.validateExplicitTenantAccess(scope, "tenant-z", true))
            .doesNotThrowAnyException();
    }

    @Test
    void deniesExplicitTenantWhenTokenScopeIsEmptyEvenIfPrivilegedCrossTenantAccessIsPresent() {
        KernelTenantScope scope = KernelTenantScope.empty();

        assertThatThrownBy(() -> validator.validateExplicitTenantAccess(scope, "tenant-z", true))
            .isInstanceOf(KernelTenantResolutionException.class)
            .extracting(exception -> ((KernelTenantResolutionException) exception).failure())
            .isEqualTo(KernelTenantResolutionFailure.REQUESTED_TENANT_NOT_PERMITTED);
    }

    @Test
    void rejectsBlankRouteTenantForExplicitValidation() {
        KernelTenantScope scope = new KernelTenantScope(List.of("tenant-a"));

        assertThatThrownBy(() -> validator.validateExplicitTenantAccess(scope, "   ", false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("routeTenantId");
    }
}
