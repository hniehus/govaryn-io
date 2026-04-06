package io.govaryn.kernel.security;

import io.govaryn.kernel.security.authorization.framework.KernelAccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestControllerAdvice
public class KernelAccessDeniedExceptionHandler {

    @ExceptionHandler(KernelAccessDeniedException.class)
    public ResponseEntity<KernelAccessDeniedResponse> handleKernelAccessDenied(
        KernelAccessDeniedException exception,
        HttpServletRequest request
    ) {
        return forbiddenResponse("ACCESS_DENIED", request);
    }

    @ExceptionHandler(KernelTenantResolutionException.class)
    public ResponseEntity<KernelAccessDeniedResponse> handleKernelTenantResolutionDenied(
        KernelTenantResolutionException exception,
        HttpServletRequest request
    ) {
        return forbiddenResponse("ACCESS_DENIED", request);
    }

    private ResponseEntity<KernelAccessDeniedResponse> forbiddenResponse(
        String reason,
        HttpServletRequest request
    ) {
        KernelAccessDeniedResponse response = new KernelAccessDeniedResponse(
            "ACCESS_DENIED",
            "Forbidden",
            reason,
            request.getRequestURI(),
            Instant.now().toString()
        );
        return ResponseEntity.status(FORBIDDEN).body(response);
    }

    public record KernelAccessDeniedResponse(
        String code,
        String message,
        String reason,
        String path,
        String timestamp
    ) {
    }
}
