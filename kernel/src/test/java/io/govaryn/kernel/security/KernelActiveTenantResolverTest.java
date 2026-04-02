package io.govaryn.kernel.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KernelActiveTenantResolverTest {

    private final KernelActiveTenantResolver resolver = new KernelActiveTenantResolver(new KernelTenantAccessValidator());

    @Test
    void resolvesSingleTenantScopeWhenRouteTenantIsNotSpecified() {
        KernelTenantResolutionRequest request = new KernelTenantResolutionRequest(
            new KernelTenantScope(List.of("tenant-a")),
            null,
            true,
            false
        );

        assertThat(resolver.resolve(request))
            .get()
            .extracting(KernelActiveTenantContext::tenantId)
            .isEqualTo("tenant-a");
    }

    @Test
    void allowsMatchingExplicitRouteTenantForSingleTenantScope() {
        KernelTenantResolutionRequest request = new KernelTenantResolutionRequest(
            new KernelTenantScope(List.of("tenant-a")),
            "tenant-a",
            true,
            false
        );

        assertThat(resolver.resolve(request))
            .get()
            .extracting(KernelActiveTenantContext::tenantId)
            .isEqualTo("tenant-a");
    }

    @Test
    void deniesMismatchingExplicitRouteTenantForSingleTenantScope() {
        KernelTenantResolutionRequest request = new KernelTenantResolutionRequest(
            new KernelTenantScope(List.of("tenant-a")),
            "tenant-b",
            true,
            false
        );

        assertThatThrownBy(() -> resolver.resolve(request))
            .isInstanceOf(KernelTenantResolutionException.class)
            .extracting(exception -> ((KernelTenantResolutionException) exception).failure())
            .isEqualTo(KernelTenantResolutionFailure.REQUESTED_TENANT_NOT_PERMITTED);
    }

    @Test
    void requiresExplicitRouteTenantForTenantProtectedRequestsWithMultiTenantScope() {
        KernelTenantResolutionRequest request = new KernelTenantResolutionRequest(
            new KernelTenantScope(List.of("tenant-a", "tenant-b")),
            null,
            true,
            false
        );

        assertThatThrownBy(() -> resolver.resolve(request))
            .isInstanceOf(KernelTenantResolutionException.class)
            .extracting(exception -> ((KernelTenantResolutionException) exception).failure())
            .isEqualTo(KernelTenantResolutionFailure.EXPLICIT_TENANT_SELECTION_REQUIRED);
    }

    @Test
    void allowsExplicitRouteTenantWithinMultiTenantScope() {
        KernelTenantResolutionRequest request = new KernelTenantResolutionRequest(
            new KernelTenantScope(List.of("tenant-a", "tenant-b")),
            "tenant-b",
            true,
            false
        );

        assertThat(resolver.resolve(request))
            .get()
            .extracting(KernelActiveTenantContext::tenantId)
            .isEqualTo("tenant-b");
    }

    @Test
    void deniesExplicitRouteTenantOutsideScopeWithoutPrivilege() {
        KernelTenantResolutionRequest request = new KernelTenantResolutionRequest(
            new KernelTenantScope(List.of("tenant-a", "tenant-b")),
            "tenant-z",
            true,
            false
        );

        assertThatThrownBy(() -> resolver.resolve(request))
            .isInstanceOf(KernelTenantResolutionException.class)
            .extracting(exception -> ((KernelTenantResolutionException) exception).failure())
            .isEqualTo(KernelTenantResolutionFailure.REQUESTED_TENANT_NOT_PERMITTED);
    }

    @Test
    void allowsExplicitRouteTenantOutsideScopeWhenPrivilegedCrossTenantAccessIsPresent() {
        KernelTenantResolutionRequest request = new KernelTenantResolutionRequest(
            new KernelTenantScope(List.of("tenant-a")),
            "tenant-z",
            true,
            true
        );

        assertThat(resolver.resolve(request))
            .get()
            .extracting(KernelActiveTenantContext::tenantId)
            .isEqualTo("tenant-z");
    }

    @Test
    void privilegedCrossTenantAccessStillRequiresExplicitRouteTenant() {
        KernelTenantResolutionRequest request = new KernelTenantResolutionRequest(
            new KernelTenantScope(List.of("tenant-a", "tenant-b")),
            null,
            true,
            true
        );

        assertThatThrownBy(() -> resolver.resolve(request))
            .isInstanceOf(KernelTenantResolutionException.class)
            .extracting(exception -> ((KernelTenantResolutionException) exception).failure())
            .isEqualTo(KernelTenantResolutionFailure.EXPLICIT_TENANT_SELECTION_REQUIRED);
    }

    @Test
    void tenantProtectedRequestWithoutScopeAndWithoutExplicitTenantIsDenied() {
        KernelTenantResolutionRequest request = new KernelTenantResolutionRequest(
            KernelTenantScope.empty(),
            null,
            true,
            false
        );

        assertThatThrownBy(() -> resolver.resolve(request))
            .isInstanceOf(KernelTenantResolutionException.class)
            .extracting(exception -> ((KernelTenantResolutionException) exception).failure())
            .isEqualTo(KernelTenantResolutionFailure.TENANT_CONTEXT_REQUIRED);
    }

    @Test
    void nonTenantProtectedRequestWithoutExplicitTenantMayProceedWithoutActiveTenant() {
        KernelTenantResolutionRequest request = new KernelTenantResolutionRequest(
            new KernelTenantScope(List.of("tenant-a", "tenant-b")),
            null,
            false,
            false
        );

        assertThat(resolver.resolve(request)).isEmpty();
    }
}
