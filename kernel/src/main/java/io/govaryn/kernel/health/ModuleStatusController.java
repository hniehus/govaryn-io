package io.govaryn.kernel.health;

import io.govaryn.kernel.config.GovarynKernelSecurityProperties;
import io.govaryn.kernel.health.security.ModuleStatusAuthorizationContract;
import io.govaryn.kernel.security.authorization.framework.KernelAuthorizationEnforcer;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationAction;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ModuleStatusController {

    private final ModuleStatusService moduleStatusService;
    private final KernelAuthorizationEnforcer authorizationEnforcer;
    private final GovarynKernelSecurityProperties securityProperties;

    public ModuleStatusController(
        ModuleStatusService moduleStatusService,
        KernelAuthorizationEnforcer authorizationEnforcer,
        GovarynKernelSecurityProperties securityProperties
    ) {
        this.moduleStatusService = moduleStatusService;
        this.authorizationEnforcer = authorizationEnforcer;
        this.securityProperties = securityProperties;
    }

    @GetMapping("/modules/status")
    public ResponseEntity<ModuleStatusResponse> moduleStatus() {
        // Security-disabled mode keeps current open behavior for local/dev scenarios.
        if (securityProperties.isEnabled()) {
            authorizationEnforcer.enforce(
                ModuleStatusAuthorizationContract.MODULE_ID,
                AuthorizationAction.READ,
                ModuleStatusAuthorizationContract.RESOURCE_TYPE,
                null,
                Map.of()
            );
        }
        return ResponseEntity.ok(moduleStatusService.currentStatus());
    }
}
