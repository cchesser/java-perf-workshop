package cchesser.javaperf.mcp.heap;

import cchesser.javaperf.mcp.cache.CachedHeapDumpRecord;

import java.time.Instant;
import java.util.Map;

public record HeapSession(
        String handle,
        CachedHeapDumpRecord cacheRecord,
        LoadedHeap loadedHeap,
        Instant loadedAt
) {
    public Map<String, Object> snapshotSummary() {
        return loadedHeap.snapshotSummary();
    }
}
