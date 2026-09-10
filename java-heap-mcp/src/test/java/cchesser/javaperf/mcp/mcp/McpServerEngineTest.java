package cchesser.javaperf.mcp.mcp;

import cchesser.javaperf.mcp.cache.CacheMetadataStore;
import cchesser.javaperf.mcp.config.ServerConfig;
import cchesser.javaperf.mcp.heap.HeapAnalysisService;
import cchesser.javaperf.mcp.heap.HeapDumpManager;
import cchesser.javaperf.mcp.heap.HeapOverview;
import cchesser.javaperf.mcp.heap.LoadedHeap;
import cchesser.javaperf.mcp.heap.ObjectInspection;
import cchesser.javaperf.mcp.heap.OqlQueryResult;
import cchesser.javaperf.mcp.heap.GcRootPath;
import cchesser.javaperf.mcp.heap.HistogramEntry;
import cchesser.javaperf.mcp.heap.DominatorEntry;
import cchesser.javaperf.mcp.util.JsonSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpServerEngineTest {
    @TempDir
    Path tempDir;

    @Test
    void registersHeapToolsWithTheSdkAdapter() throws Exception {
        JsonSupport jsonSupport = JsonSupport.create();
        ServerConfig config = new ServerConfig(tempDir.resolve("cache"), 10, 50, Duration.ofSeconds(5), 5, false, 7777, 0L);
        CacheMetadataStore metadataStore = new CacheMetadataStore(config.cacheRoot(), jsonSupport.mapper());
        HeapDumpManager manager = new HeapDumpManager(config, metadataStore, new FakeHeapAnalysisService());
        ToolRegistry registry = new ToolRegistry(manager, jsonSupport.mapper(), config);

        assertThat(registry.specifications())
                .extracting(specification -> specification.tool().name())
                .contains("heap_load_dump", "heap_open_project", "heap_list_projects",
                        "heap_run_oql", "heap_get_oql_grammar", "heap_find_leak_suspects");
    }

    private static final class FakeHeapAnalysisService implements HeapAnalysisService {
        @Override
        public LoadedHeap open(Path heapDumpPath) {
            try {
                Files.createDirectories(heapDumpPath.getParent());
            } catch (Exception ignored) {
            }
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
