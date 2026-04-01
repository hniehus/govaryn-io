package io.govaryn.kernel.security.authorization.framework;

import io.govaryn.kernel.security.authorization.framework.model.AuthorizationDecision;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Structured audit logging for denied authorization attempts.
 */
@Component
public class AuthorizationAuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationAuditLogger.class);
    private static final int MAX_VALUE_LENGTH = 128;

    public void logDenied(AuthorizationRequest request, AuthorizationDecision decision, Throwable evaluationError) {
        if (decision == null || decision.allowed()) {
            return;
        }

        log.warn(
            "event=authorization_deny_audit timestamp={} userId={} tenantId={} module={} resourceType={} action={} resourceId={} decision={} denyReason={} requestRef={} errorType={}",
            Instant.now(),
            userId(request),
            tenantId(request),
            requestValue(request, RequestField.MODULE),
            requestValue(request, RequestField.RESOURCE_TYPE),
            requestValue(request, RequestField.ACTION),
            requestValue(request, RequestField.RESOURCE_ID),
            "DENY",
            sanitize(decision.denyReason().name()),
            resolveRequestReference(),
            evaluationError == null ? "none" : sanitize(evaluationError.getClass().getSimpleName())
        );
    }

    private String userId(AuthorizationRequest request) {
        if (request == null || request.securityContext() == null) {
            return "none";
        }
        return sanitize(request.securityContext().userId());
    }

    private String tenantId(AuthorizationRequest request) {
        if (request == null || request.securityContext() == null) {
            return "none";
        }
        return sanitize(request.securityContext().tenantId());
    }

    private String requestValue(AuthorizationRequest request, RequestField field) {
        if (request == null) {
            return "none";
        }
        return switch (field) {
            case MODULE -> sanitize(request.moduleId());
            case RESOURCE_TYPE -> sanitize(request.resourceType());
            case RESOURCE_ID -> sanitize(request.resourceId());
            case ACTION -> request.action() == null ? "none" : sanitize(request.action().name());
        };
    }

    private String resolveRequestReference() {
        return firstNonBlank(
            MDC.get("traceId"),
            MDC.get("requestId"),
            MDC.get("correlationId"),
            MDC.get("x-request-id"),
            MDC.get("X-Request-Id")
        );
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return sanitize(value);
            }
        }
        return "none";
    }

    private String sanitize(String value) {
        if (value == null) {
            return "none";
        }
        String sanitized = value.replaceAll("[\\r\\n\\t\\x00-\\x1F]", " ").trim();
        if (sanitized.isEmpty()) {
            return "none";
        }
        if (sanitized.length() > MAX_VALUE_LENGTH) {
            return sanitized.substring(0, MAX_VALUE_LENGTH);
        }
        return sanitized;
    }

    private enum RequestField {
        MODULE,
        RESOURCE_TYPE,
        RESOURCE_ID,
        ACTION
    }
}
