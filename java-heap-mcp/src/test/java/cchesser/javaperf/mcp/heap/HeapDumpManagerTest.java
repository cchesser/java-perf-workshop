package cchesser.javaperf.mcp.heap;

import cchesser.javaperf.mcp.cache.CacheMetadataStore;
import cchesser.javaperf.mcp.config.ServerConfig;
import cchesser.javaperf.mcp.util.JsonSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HeapDumpManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void loadAndUnloadTracksSessionsAndMetadata() throws Exception {
        Path heapDump = tempDir.resolve("sample.hprof");
        Files.writeString(heapDump, "not-a-real-heap");

        ServerConfig config = new ServerConfig(tempDir.resolve("cache"), 10, 50, Duration.ofSeconds(5), 5, false, 7777, 0L);
        CacheMetadataStore metadataStore = new CacheMetadataStore(config.cacheRoot(), JsonSupport.create().mapper());
        try (HeapDumpManager manager = new HeapDumpManager(config, metadataStore, new FakeHeapAnalysisService())) {
            Map<String, Object> loadResult = manager.loadDump("proj-a", heapDump.toString());
            String handle = (String) loadResult.get("handle");

            assertThat(handle).startsWith("heap-");
            assertThat(loadResult).containsEntry("project", "proj-a");
            assertThat(manager.listDumps()).hasSize(1);
            assertThat(manager.listProjects()).hasSize(1);

            Map<String, Object> unloadResult = manager.unloadDump(handle);
            assertThat(unloadResult).containsEntry("unloaded", true);

            Map<String, Object> reopened = manager.openProject("proj-a");
            assertThat(reopened.get("project")).isEqualTo("proj-a");
            assertThat(reopened.get("handle")).isInstanceOf(String.class);
        }
    }

    private static final class FakeHeapAnalysisService implements HeapAnalysisService {
        @Override
        public LoadedHeap open(Path heapDumpPath) {
            return new LoadedHeap() {
                @Override
                public Path heapDumpPath() {
                    return heapDumpPath;
                }

                @Override
                public Map<String, Object> snapshotSummary() {
                    return Map.of("path", heapDumpPath.toString(), "numberOfObjects", 3);
                }

                @Override
                public Object nativeSnapshot() {
                    return Map.of();
                }

                @Override
                public void dispose() {
                }
            };
        }

        @Override
        public HeapOverview overview(LoadedHeap heap, int limit) {
            return new HeapOverview(heap.snapshotSummary(), List.of(), List.of());
        }

        @Override
        public OqlQueryResult runOql(LoadedHeap heap, String query, int limit, int offset) {
            return new OqlQueryResult(query, List.of("value"), List.of(Map.of("value", 1)), offset, limit, false, 1);
        }

        @Override
        public List<HistogramEntry> histogram(LoadedHeap heap, String sortBy, int limit) {
            return List.of();
        }

        @Override
        public List<DominatorEntry> dominators(LoadedHeap heap, Integer rootObjectId, int limit) {
            return List.of();
        }

        @Override
        public ObjectInspection inspectObject(LoadedHeap heap, int objectId, int limit) {
            return new ObjectInspection(objectId, 0L, "Object", "Object", 1L, 1L, null, List.of(), List.of(), List.of(), List.of());
        }

        @Override
        public List<GcRootPath> findPathsToGcRoots(LoadedHeap heap, int objectId, boolean excludeWeakRefs, int limit) {
            return List.of();
        }

        @Override
        public Object leakSuspects(LoadedHeap heap, int limit) {
            return Map.of();
        }

        @Override
        public void close() {
        }
    }
}
