package io.govaryn.kernel.backend.standard;

import io.govaryn.kernel.api.KernelCurrentSecurityContext;
import io.govaryn.kernel.security.KernelTenantResolutionException;
import io.govaryn.kernel.security.KernelTenantResolutionFailure;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class KernelStandardRecordService {

    private final KernelCurrentSecurityContext currentSecurityContext;
    private final Map<String, Map<String, KernelStandardRecord>> recordsByTenant = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public KernelStandardRecordService(KernelCurrentSecurityContext currentSecurityContext) {
        this.currentSecurityContext = currentSecurityContext;
    }

    public Optional<KernelStandardRecord> readOne(String recordId) {
        return Optional.ofNullable(tenantRecords().get(recordId));
    }

    public List<KernelStandardRecord> list() {
        return tenantRecords().values().stream()
            .sorted(Comparator.comparing(KernelStandardRecord::id))
            .toList();
    }

    public KernelStandardRecord create(String value) {
        String id = "record-" + sequence.incrementAndGet();
        KernelStandardRecord record = new KernelStandardRecord(id, value.trim());
        tenantRecords().put(id, record);
        return record;
    }

    public Optional<KernelStandardRecord> update(String recordId, String value) {
        Map<String, KernelStandardRecord> tenantRecords = tenantRecords();
        if (!tenantRecords.containsKey(recordId)) {
            return Optional.empty();
        }
        KernelStandardRecord updated = new KernelStandardRecord(recordId, value.trim());
        tenantRecords.put(recordId, updated);
        return Optional.of(updated);
    }

    public boolean delete(String recordId) {
        return tenantRecords().remove(recordId) != null;
    }

    private Map<String, KernelStandardRecord> tenantRecords() {
        return recordsByTenant.computeIfAbsent(activeTenantId(), ignored -> new ConcurrentHashMap<>());
    }

    private String activeTenantId() {
        return currentSecurityContext.currentActiveTenant()
            .map(activeTenant -> activeTenant.tenantId())
            .orElseThrow(() -> new KernelTenantResolutionException(
                KernelTenantResolutionFailure.TENANT_CONTEXT_REQUIRED,
                "Tenant-protected record operation requires an active tenant"
            ));
    }
}
