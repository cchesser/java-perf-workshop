package cchesser.javaperf.mcp.cache;

import cchesser.javaperf.mcp.util.JsonSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CacheMetadataStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsAndReloadsMetadata() {
        CacheMetadataStore store = new CacheMetadataStore(tempDir, JsonSupport.create().mapper());
        CachedHeapDumpRecord record = new CachedHeapDumpRecord(
                "proj-a",
                "fp-1",
                "/tmp/heap.hprof",
                "heap.hprof",
                12L,
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-01-01T00:00:01Z"),
                Instant.parse("2025-01-01T00:00:02Z"),
                "hash",
                12L
        );

        store.upsert(record);

        CacheMetadataStore reloaded = new CacheMetadataStore(tempDir, JsonSupport.create().mapper());
        assertThat(reloaded.get("fp-1")).contains(record);
        assertThat(reloaded.getByProject("proj-a")).contains(record);
    }
}
