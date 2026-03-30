package io.govaryn.kernel.security.authorization;

import io.govaryn.kernel.security.authorization.model.AuthorizationDecision;
import io.govaryn.kernel.security.authorization.model.AuthorizationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AuthorizationDecisionLogger {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationDecisionLogger.class);
    private static final int REF_HASH_LENGTH = 12;
    private static final int MAX_VALUE_LENGTH = 64;

    public void logDecision(
        AuthorizationRequest request,
        AuthorizationDecision decision,
        String policyRevision,
        Throwable evaluationError
    ) {
        log.info(
            "event=authorization_decision result={} subjectRef={} action={} resourceType={} resourceIdRef={} context={} policyRevision={} reasonCode={} matchedRuleId={} requestRef={} errorType={}",
            sanitize(decision.result().name()),
            subjectRef(request),
            requestValue(request, RequestField.ACTION),
            requestValue(request, RequestField.RESOURCE_TYPE),
            resourceRef(request),
            sanitizeContext(request),
            sanitize(policyRevision),
            sanitize(decision.reasonCode().name()),
            sanitize(decision.matchedRuleId() == null ? "none" : decision.matchedRuleId()),
            resolveRequestReference(),
            evaluationError == null ? "none" : sanitize(evaluationError.getClass().getSimpleName())
        );
    }

    private String subjectRef(AuthorizationRequest request) {
        if (request == null || request.subject() == null) {
            return "none";
        }
        return hashedRef(request.subject().subjectId());
    }

    private String resourceRef(AuthorizationRequest request) {
        if (request == null || request.resourceId() == null) {
            return "none";
        }
        return hashedRef(request.resourceId());
    }

    private String sanitizeContext(AuthorizationRequest request) {
        if (request == null || request.context() == null || request.context().isEmpty()) {
            return "none";
        }
        return request.context().entrySet().stream()
            .filter(entry -> entry.getKey() != null && entry.getValue() != null)
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> sanitize(entry.getKey()) + "=" + sanitize(entry.getValue()))
            .collect(Collectors.joining(",", "{", "}"));
    }

    private String requestValue(AuthorizationRequest request, RequestField field) {
        if (request == null) {
            return "none";
        }
        return switch (field) {
            case ACTION -> sanitize(request.action());
            case RESOURCE_TYPE -> sanitize(request.resourceType());
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

    private String hashedRef(String raw) {
        if (raw == null || raw.isBlank()) {
            return "none";
        }
        return "sha256:" + sha256Hex(raw).substring(0, REF_HASH_LENGTH);
    }

    private String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest unavailable", ex);
        }
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
        ACTION,
        RESOURCE_TYPE
    }
}
