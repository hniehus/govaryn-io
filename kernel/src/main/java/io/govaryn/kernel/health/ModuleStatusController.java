package io.govaryn.kernel.health;

import io.govaryn.kernel.api.KernelAuthorizationOperations;
import io.govaryn.kernel.api.KernelAuthorizationService;
import io.govaryn.kernel.security.KernelRequestSecurityContext;
import io.govaryn.kernel.security.authorization.framework.model.SecurityContext;
import io.govaryn.kernel.security.authorization.model.AuthorizationDecisionResult;
import io.govaryn.kernel.security.authorization.model.AuthorizationSubject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
public class ModuleStatusController {

    private final ModuleStatusService moduleStatusService;
    private final KernelRequestSecurityContext requestSecurityContext;
    private final KernelAuthorizationService kernelAuthorizationService;

    public ModuleStatusController(
        ModuleStatusService moduleStatusService,
        KernelRequestSecurityContext requestSecurityContext,
        KernelAuthorizationService kernelAuthorizationService
    ) {
        this.moduleStatusService = moduleStatusService;
        this.requestSecurityContext = requestSecurityContext;
        this.kernelAuthorizationService = kernelAuthorizationService;
    }

    @GetMapping("/modules/status")
    public ResponseEntity<ModuleStatusResponse> moduleStatus(Authentication authentication) {
        // Security-disabled mode keeps current open behavior for local/dev scenarios.
        if (authentication != null) {
            enforceModuleStatusReadPermission();
        }
        return ResponseEntity.ok(moduleStatusService.currentStatus());
    }

    private void enforceModuleStatusReadPermission() {
        try {
            SecurityContext securityContext = requestSecurityContext.current()
                .orElseThrow(() -> new IllegalArgumentException("Authenticated request context is required"));
            AuthorizationSubject subject = new AuthorizationSubject(
                securityContext.userId(),
                List.copyOf(securityContext.globalRoles()),
                Map.copyOf(securityContext.claims())
            );

            var decision = kernelAuthorizationService.authorize(
                subject,
                KernelAuthorizationOperations.of("read", "module-status")
            );
            if (decision.result() != AuthorizationDecisionResult.PERMIT) {
                throw new ResponseStatusException(FORBIDDEN, "Forbidden");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(FORBIDDEN, "Forbidden");
        }
    }
}
