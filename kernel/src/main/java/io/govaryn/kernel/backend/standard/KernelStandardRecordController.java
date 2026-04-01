package io.govaryn.kernel.backend.standard;

import io.govaryn.kernel.security.authorization.framework.KernelAuthorizationEnforcer;
import io.govaryn.kernel.security.authorization.framework.model.AuthorizationAction;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/kernel/records")
public class KernelStandardRecordController {

    private final KernelStandardRecordService recordService;
    private final KernelAuthorizationEnforcer authorizationEnforcer;

    public KernelStandardRecordController(
        KernelStandardRecordService recordService,
        KernelAuthorizationEnforcer authorizationEnforcer
    ) {
        this.recordService = recordService;
        this.authorizationEnforcer = authorizationEnforcer;
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<KernelStandardRecord> readOne(@PathVariable String recordId) {
        authorizationEnforcer.enforce(
            KernelStandardRecordContract.MODULE_ID,
            AuthorizationAction.READ,
            KernelStandardRecordContract.RESOURCE_TYPE,
            recordId,
            Map.of()
        );
        return recordService.readOne(recordId)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Record not found"));
    }

    @GetMapping
    public ResponseEntity<List<KernelStandardRecord>> list() {
        authorizationEnforcer.enforce(
            KernelStandardRecordContract.MODULE_ID,
            AuthorizationAction.LIST,
            KernelStandardRecordContract.RESOURCE_TYPE,
            null,
            Map.of()
        );
        return ResponseEntity.ok(recordService.list());
    }

    @PostMapping
    public ResponseEntity<KernelStandardRecord> create(@Valid @RequestBody KernelStandardRecordWriteRequest request) {
        authorizationEnforcer.enforce(
            KernelStandardRecordContract.MODULE_ID,
            AuthorizationAction.CREATE,
            KernelStandardRecordContract.RESOURCE_TYPE,
            null,
            Map.of()
        );
        KernelStandardRecord created = recordService.create(request.value());
        return ResponseEntity.status(CREATED).body(created);
    }

    @PutMapping("/{recordId}")
    public ResponseEntity<KernelStandardRecord> update(
        @PathVariable String recordId,
        @Valid @RequestBody KernelStandardRecordWriteRequest request
    ) {
        authorizationEnforcer.enforce(
            KernelStandardRecordContract.MODULE_ID,
            AuthorizationAction.UPDATE,
            KernelStandardRecordContract.RESOURCE_TYPE,
            recordId,
            Map.of()
        );
        return recordService.update(recordId, request.value())
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Record not found"));
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> delete(@PathVariable String recordId) {
        authorizationEnforcer.enforce(
            KernelStandardRecordContract.MODULE_ID,
            AuthorizationAction.DELETE,
            KernelStandardRecordContract.RESOURCE_TYPE,
            recordId,
            Map.of()
        );
        if (!recordService.delete(recordId)) {
            throw new ResponseStatusException(NOT_FOUND, "Record not found");
        }
        return ResponseEntity.noContent().build();
    }
}
