package io.govaryn.kernel.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KernelRouteTenantSelectionResolverTest {

    private final KernelRouteTenantSelectionResolver resolver = new KernelRouteTenantSelectionResolver();

    @Test
    void resolvesRouteTenantFromTenantPathVariable() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reference/tenants/tenant-a/documents");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("tenantId", "tenant-a"));

        KernelRouteTenantSelection selection = resolver.resolve(request, handler("regularHandler"));

        assertThat(selection.routeTenantId()).isEqualTo("tenant-a");
        assertThat(selection.tenantProtectedOperation()).isTrue();
    }

    @Test
    void marksAnnotatedHandlerAsTenantProtectedWithoutExplicitRouteTenant() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reference/protected");

        KernelRouteTenantSelection selection = resolver.resolve(request, handler("annotatedTenantProtectedHandler"));

        assertThat(selection.routeTenantId()).isNull();
        assertThat(selection.tenantProtectedOperation()).isTrue();
    }

    @Test
    void returnsUnprotectedSelectionWhenNoTenantSignalExists() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reference/unprotected");

        KernelRouteTenantSelection selection = resolver.resolve(request, handler("regularHandler"));

        assertThat(selection.routeTenantId()).isNull();
        assertThat(selection.tenantProtectedOperation()).isFalse();
    }

    @Test
    void treatsBlankRouteTenantAsMissingButStillProtectedWhenTenantVariableIsPresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/reference/tenants//documents");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("tenantId", "   "));

        KernelRouteTenantSelection selection = resolver.resolve(request, handler("regularHandler"));

        assertThat(selection.routeTenantId()).isNull();
        assertThat(selection.tenantProtectedOperation()).isTrue();
    }

    private HandlerMethod handler(String methodName) throws NoSuchMethodException {
        return new HandlerMethod(
            new FixtureController(),
            FixtureController.class.getDeclaredMethod(methodName)
        );
    }

    @SuppressWarnings("unused")
    private static final class FixtureController {

        @KernelTenantProtectedOperation
        void annotatedTenantProtectedHandler() {
        }

        void regularHandler() {
        }
    }
}
