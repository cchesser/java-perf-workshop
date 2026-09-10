package cchesser.javaperf.mcp.heap;

import cchesser.javaperf.mcp.cache.CacheMetadataStore;
import cchesser.javaperf.mcp.cache.CachedHeapDumpRecord;
import cchesser.javaperf.mcp.cache.HeapDumpFingerprint;
import cchesser.javaperf.mcp.config.ServerConfig;
import cchesser.javaperf.mcp.util.Timeouts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public final class HeapDumpManager implements AutoCloseable {
    private static final String[] PROJECT_ADJECTIVES = {
            "bold", "bright", "calm", "clever", "eager", "fuzzy", "gentle", "happy",
            "keen", "lively", "mighty", "noble", "quick", "quiet", "sunny", "wise"
    };
    private static final String[] PROJECT_NAMES = {
            "ada", "curie", "darwin", "einstein", "hopper", "lovelace", "morse", "newton",
            "noether", "pascal", "tesla", "turing", "watson", "wright"
    };

    private final ServerConfig config;
    private final CacheMetadataStore metadataStore;
    private final HeapAnalysisService heapAnalysisService;
    private final Map<String, HeapSession> sessions = new ConcurrentHashMap<>();

    public HeapDumpManager(ServerConfig config, CacheMetadataStore metadataStore, HeapAnalysisService heapAnalysisService) {
        this.config = config;
        this.metadataStore = metadataStore;
        this.heapAnalysisService = heapAnalysisService;
    }

    public Map<String, Object> loadDump(String dumpPath) {
        return loadDump(null, dumpPath);
    }

    public Map<String, Object> loadDump(String projectName, String dumpPath) {
        Path sourcePath = Path.of(dumpPath).toAbsolutePath().normalize();
        if (!Files.exists(sourcePath)) {
            throw new HeapOperationException(HeapErrorCode.INVALID_PATH, "Heap dump path does not exist: " + sourcePath);
        }
        if (!Files.isReadable(sourcePath)) {
            throw new HeapOperationException(HeapErrorCode.INVALID_PATH, "Heap dump path is not readable: " + sourcePath);
        }
        String normalizedProject = normalizeProjectName(projectName);

        return Timeouts.call("heap_load_dump", config.toolTimeout(), () -> {
            closeActiveSessionsForProject(normalizedProject);
            HeapDumpFingerprint fingerprint = HeapDumpFingerprint.from(sourcePath);
            CachedHeapDumpRecord cacheRecord = prepareCacheEntry(normalizedProject, fingerprint);
            LoadedHeap loadedHeap = heapAnalysisService.open(cacheDumpPath(cacheRecord));
            String handle = "heap-" + UUID.randomUUID();
            HeapSession session = new HeapSession(handle, metadataStore.touchLoaded(cacheRecord), loadedHeap, Instant.now());
            sessions.put(handle, session);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("project", normalizedProject);
            response.put("handle", handle);
            response.put("fingerprint", cacheRecord.fingerprint());
            response.put("sourcePath", cacheRecord.sourcePath());
            response.put("loadedAt", session.loadedAt());
            response.put("summary", session.snapshotSummary());
            return response;
        });
    }

    public Map<String, Object> openProject(String projectName) {
        String normalizedProject = normalizeProjectName(projectName);
        CachedHeapDumpRecord record = metadataStore.getByProject(normalizedProject)
                .orElseThrow(() -> new HeapOperationException(HeapErrorCode.NOT_FOUND, "Unknown project: " + normalizedProject));

        return Timeouts.call("heap_open_project", config.toolTimeout(), () -> {
            closeActiveSessionsForProject(normalizedProject);
            LoadedHeap loadedHeap = heapAnalysisService.open(cacheDumpPath(record));
            String handle = "heap-" + UUID.randomUUID();
            HeapSession session = new HeapSession(handle, metadataStore.touchLoaded(record), loadedHeap, Instant.now());
            sessions.put(handle, session);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("project", normalizedProject);
            response.put("handle", handle);
            response.put("fingerprint", record.fingerprint());
            response.put("sourcePath", record.sourcePath());
            response.put("loadedAt", session.loadedAt());
            response.put("summary", session.snapshotSummary());
            return response;
        });
    }

    public List<Map<String, Object>> listDumps() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (CachedHeapDumpRecord record : metadataStore.list().stream()
                .sorted(Comparator
                        .comparing(CachedHeapDumpRecord::projectName)
                        .thenComparing(CachedHeapDumpRecord::lastLoadedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList()) {
            Optional<String> activeHandle = sessions.values().stream()
                    .filter(session -> session.cacheRecord().projectName().equals(record.projectName()))
                    .map(HeapSession::handle)
                    .findFirst();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("project", record.projectName());
            item.put("fingerprint", record.fingerprint());
            item.put("sourcePath", record.sourcePath());
            item.put("fileSizeBytes", record.fileSizeBytes());
            item.put("cachedDumpSizeBytes", record.cachedDumpSizeBytes());
            item.put("lastModifiedTime", record.lastModifiedTime());
            item.put("createdAt", record.createdAt());
            item.put("lastLoadedAt", record.lastLoadedAt());
            item.put("activeHandle", activeHandle.orElse(null));
            item.put("cachePath", cacheDumpPath(record).toString());
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> listProjects() {
        return metadataStore.list().stream()
                .sorted(Comparator.comparing(CachedHeapDumpRecord::projectName))
                .map(record -> {
                    Optional<String> activeHandle = sessions.values().stream()
                            .filter(session -> session.cacheRecord().projectName().equals(record.projectName()))
                            .map(HeapSession::handle)
                            .findFirst();
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("project", record.projectName());
                    item.put("fingerprint", record.fingerprint());
                    item.put("sourcePath", record.sourcePath());
                    item.put("lastLoadedAt", record.lastLoadedAt());
                    item.put("activeHandle", activeHandle.orElse(null));
                    return item;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> unloadDump(String handle) {
        HeapSession removed = sessions.remove(handle);
        if (removed == null) {
            throw new HeapOperationException(HeapErrorCode.NOT_FOUND, "Unknown heap handle: " + handle);
        }
        removed.loadedHeap().dispose();
        return Map.of(
                "project", removed.cacheRecord().projectName(),
                "handle", handle,
                "unloaded", true,
                "cachePreserved", true,
                "fingerprint", removed.cacheRecord().fingerprint()
        );
    }

    public HeapOverview overview(String handle, Integer limit) {
        HeapSession session = requireSession(handle);
        return Timeouts.call("heap_get_overview", config.toolTimeout(),
                () -> heapAnalysisService.overview(session.loadedHeap(), config.normalizeLimit(limit)));
    }

    public OqlQueryResult runOql(String handle, String query, Integer limit, Integer offset) {
        HeapSession session = requireSession(handle);
        return Timeouts.call("heap_run_oql", config.toolTimeout(),
                () -> heapAnalysisService.runOql(session.loadedHeap(), query, config.normalizeLimit(limit), Math.max(0, offset == null ? 0 : offset)));
    }

    public List<HistogramEntry> histogram(String handle, String sortBy, Integer limit) {
        HeapSession session = requireSession(handle);
        return Timeouts.call("heap_get_histogram", config.toolTimeout(),
                () -> heapAnalysisService.histogram(session.loadedHeap(), sortBy, config.normalizeLimit(limit)));
    }

    public List<DominatorEntry> dominators(String handle, Integer rootObjectId, Integer limit) {
        HeapSession session = requireSession(handle);
        return Timeouts.call("heap_get_dominators", config.toolTimeout(),
                () -> heapAnalysisService.dominators(session.loadedHeap(), rootObjectId, config.normalizeLimit(limit)));
    }

    public ObjectInspection inspectObject(String handle, int objectId, Integer limit) {
        HeapSession session = requireSession(handle);
        return Timeouts.call("heap_inspect_object", config.toolTimeout(),
                () -> heapAnalysisService.inspectObject(session.loadedHeap(), objectId, config.normalizeLimit(limit)));
    }

    public List<GcRootPath> findPathsToGcRoots(String handle, int objectId, boolean excludeWeakRefs, Integer limit) {
        HeapSession session = requireSession(handle);
        int effectiveLimit = Math.min(config.gcRootPathLimit(), config.normalizeLimit(limit));
        return Timeouts.call("heap_find_path_to_gc_roots", config.toolTimeout(),
                () -> heapAnalysisService.findPathsToGcRoots(session.loadedHeap(), objectId, excludeWeakRefs, effectiveLimit));
    }

    public Object findLeakSuspects(String handle, Integer limit) {
        HeapSession session = requireSession(handle);
        return Timeouts.call("heap_find_leak_suspects", config.toolTimeout(),
                () -> heapAnalysisService.leakSuspects(session.loadedHeap(), config.normalizeLimit(limit)));
    }

    private CachedHeapDumpRecord prepareCacheEntry(String projectName, HeapDumpFingerprint fingerprint) throws IOException {
        Path entryDir = config.cacheRoot().resolve("entries").resolve(fingerprint.fingerprint());
        Files.createDirectories(entryDir);
        Path cachedDumpPath = entryDir.resolve(fingerprint.sourceFileName());

        if (!Files.exists(cachedDumpPath)) {
            try {
                Files.createLink(cachedDumpPath, fingerprint.sourcePath());
            } catch (UnsupportedOperationException | IOException linkFailure) {
                Files.copy(fingerprint.sourcePath(), cachedDumpPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        String fullFileHash = HeapDumpFingerprint.sha256File(fingerprint.sourcePath());
        CachedHeapDumpRecord record = new CachedHeapDumpRecord(
                projectName,
                fingerprint.fingerprint(),
                fingerprint.sourcePath().toString(),
                cachedDumpPath.getFileName().toString(),
                fingerprint.fileSizeBytes(),
                fingerprint.lastModifiedTime(),
                Instant.now(),
                null,
                fullFileHash,
                Files.size(cachedDumpPath)
        );
        return metadataStore.getByProject(projectName).map(existing -> new CachedHeapDumpRecord(
                projectName,
                fingerprint.fingerprint(),
                fingerprint.sourcePath().toString(),
                cachedDumpPath.getFileName().toString(),
                fingerprint.fileSizeBytes(),
                fingerprint.lastModifiedTime(),
                existing.createdAt(),
                existing.lastLoadedAt(),
                fullFileHash,
                fingerprint.fileSizeBytes()
        )).map(metadataStore::upsert).orElseGet(() -> metadataStore.upsert(record));
    }

    private Path cacheDumpPath(CachedHeapDumpRecord record) {
        return config.cacheRoot().resolve("entries").resolve(record.fingerprint()).resolve(record.cacheFileName());
    }

    private HeapSession requireSession(String handle) {
        HeapSession session = sessions.get(handle);
        if (session == null) {
            throw new HeapOperationException(HeapErrorCode.NOT_FOUND, "Unknown heap handle: " + handle);
        }
        return session;
    }

    private void closeActiveSessionsForProject(String projectName) {
        List<String> handles = sessions.values().stream()
                .filter(session -> session.cacheRecord().projectName().equals(projectName))
                .map(HeapSession::handle)
                .toList();
        for (String handle : handles) {
            HeapSession removed = sessions.remove(handle);
            if (removed != null) {
                removed.loadedHeap().dispose();
            }
        }
    }

    private String normalizeProjectName(String projectName) {
        if (projectName == null || projectName.isBlank()) {
            return memorableProjectName();
        }
        return projectName.trim();
    }

    private String memorableProjectName() {
        for (int attempt = 0; attempt < 100; attempt++) {
            String candidate = PROJECT_ADJECTIVES[ThreadLocalRandom.current().nextInt(PROJECT_ADJECTIVES.length)]
                    + "_"
                    + PROJECT_NAMES[ThreadLocalRandom.current().nextInt(PROJECT_NAMES.length)];
            if (metadataStore.getByProject(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "heap_" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public void close() {
        sessions.values().forEach(session -> session.loadedHeap().dispose());
        sessions.clear();
        heapAnalysisService.close();
    }
}
