package io.govaryn.kernel.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;
import java.util.Map;

/**
 * Resolves route tenant information for request-level tenant context establishment.
 */
@Component
public class KernelRouteTenantSelectionResolver {

    private static final List<String> TENANT_ROUTE_VARIABLE_KEYS = List.of("tenantId", "tenant_id", "tid");

    public KernelRouteTenantSelection resolve(HttpServletRequest request, Object handler) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        Map<?, ?> routeVariables = routeVariables(request);
        String routeTenantId = null;
        boolean routeTenantSelectorPresent = false;
        for (String key : TENANT_ROUTE_VARIABLE_KEYS) {
            if (routeVariables.containsKey(key)) {
                routeTenantSelectorPresent = true;
                routeTenantId = routeTenantValue(routeVariables.get(key));
                if (routeTenantId != null) {
                    break;
                }
            }
        }

        boolean tenantProtectedOperation = routeTenantSelectorPresent || isTenantProtectedHandler(handler);
        return new KernelRouteTenantSelection(routeTenantId, tenantProtectedOperation);
    }

    private Map<?, ?> routeVariables(HttpServletRequest request) {
        Object routeVariables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(routeVariables instanceof Map<?, ?> map)) {
            return Map.of();
        }
        return map;
    }

    private boolean isTenantProtectedHandler(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return false;
        }
        return handlerMethod.hasMethodAnnotation(KernelTenantProtectedOperation.class)
            || handlerMethod.getBeanType().isAnnotationPresent(KernelTenantProtectedOperation.class);
    }

    private String routeTenantValue(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toString().trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
