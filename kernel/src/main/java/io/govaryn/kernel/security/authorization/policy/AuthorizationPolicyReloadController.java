package io.govaryn.kernel.security.authorization.policy;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthorizationPolicyReloadController {

    private final AuthorizationPolicyLifecycleService policyLifecycleService;

    public AuthorizationPolicyReloadController(AuthorizationPolicyLifecycleService policyLifecycleService) {
        this.policyLifecycleService = policyLifecycleService;
    }

    @PostMapping("/api/kernel/internal/authorization/policy/reload")
    public ResponseEntity<AuthorizationPolicyReloadResponse> reloadPolicy() {
        AuthorizationPolicyReloadResult result = policyLifecycleService.reloadFromConfiguredSource();
        String activeRevision = policyLifecycleService.currentActivePolicy()
            .map(ActiveAuthorizationPolicySnapshot::revision)
            .orElse("none");

        if (result.success()) {
            return ResponseEntity.ok(new AuthorizationPolicyReloadResponse(true, result.revision(), activeRevision, null));
        }

        return ResponseEntity.badRequest().body(
            new AuthorizationPolicyReloadResponse(false, null, activeRevision, sanitizeForLog(result.errorMessage()))
        );
    }

    private static String sanitizeForLog(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[\\r\\n\\t\\x00-\\x1F]", " ").trim();
    }

    public record AuthorizationPolicyReloadResponse(
        boolean success,
        String revision,
        String activeRevision,
        String errorMessage
    ) {
    }
}
