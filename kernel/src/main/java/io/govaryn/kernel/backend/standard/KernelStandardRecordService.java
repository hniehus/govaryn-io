package io.govaryn.kernel.backend.standard;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class KernelStandardRecordService {

    private final Map<String, KernelStandardRecord> records = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public Optional<KernelStandardRecord> readOne(String recordId) {
        return Optional.ofNullable(records.get(recordId));
    }

    public List<KernelStandardRecord> list() {
        return records.values().stream()
            .sorted(Comparator.comparing(KernelStandardRecord::id))
            .toList();
    }

    public KernelStandardRecord create(String value) {
        String id = "record-" + sequence.incrementAndGet();
        KernelStandardRecord record = new KernelStandardRecord(id, value.trim());
        records.put(id, record);
        return record;
    }

    public Optional<KernelStandardRecord> update(String recordId, String value) {
        if (!records.containsKey(recordId)) {
            return Optional.empty();
        }
        KernelStandardRecord updated = new KernelStandardRecord(recordId, value.trim());
        records.put(recordId, updated);
        return Optional.of(updated);
    }

    public boolean delete(String recordId) {
        return records.remove(recordId) != null;
    }
}
