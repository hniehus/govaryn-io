package io.govaryn.kernel.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Locale;

@Component
public class KernelAuthenticationFailureEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(KernelAuthenticationFailureEntryPoint.class);

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException {
        String category = determineCategory(request, authException);

        logger.warn(
            "Authentication failed: category={}, method={}, path={}, exception={}",
            category,
            request.getMethod(),
            request.getRequestURI(),
            authException.getClass().getSimpleName()
        );

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private String determineCategory(HttpServletRequest request, AuthenticationException exception) {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization)) {
            return "missing_bearer_token";
        }

        String details = flattenExceptionDetails(exception);
        if (details.contains("audience")) {
            return "audience_validation_failure";
        }
        if (details.contains("issuer")
            || details.contains(" iss claim")
            || details.startsWith("iss claim")) {
            return "issuer_mismatch";
        }
        if (details.contains("expired")) {
            return "token_expired";
        }
        if (details.contains("invalid signature")
            || details.contains("signature")
            || details.contains("signed jwt rejected")) {
            return "invalid_signature";
        }
        if (details.contains("jwk")
            || details.contains("jwks")
            || details.contains("unable to resolve the configuration")
            || details.contains("temporarily unavailable")
            || details.contains("connection refused")) {
            return "provider_or_key_retrieval_failure";
        }
        return "invalid_token";
    }

    private String flattenExceptionDetails(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null) {
                builder.append(current.getMessage()).append(' ');
            }
            current = current.getCause();
        }
        return builder.toString().toLowerCase(Locale.ROOT);
    }
}
