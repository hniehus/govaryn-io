package io.govaryn.kernel.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ModuleStatusController {

    private final ModuleStatusService moduleStatusService;

    public ModuleStatusController(ModuleStatusService moduleStatusService) {
        this.moduleStatusService = moduleStatusService;
    }

    @GetMapping("/modules/status")
    public ResponseEntity<ModuleStatusResponse> moduleStatus() {
        return ResponseEntity.ok(moduleStatusService.currentStatus());
    }
}
