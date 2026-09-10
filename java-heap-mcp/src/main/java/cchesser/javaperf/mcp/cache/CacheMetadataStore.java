package cchesser.javaperf.mcp.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class CacheMetadataStore {
    private static final TypeReference<Map<String, CachedHeapDumpRecord>> TYPE = new TypeReference<>() { };

    private final Path metadataFile;
    private final ObjectMapper mapper;

    public CacheMetadataStore(Path cacheRoot, ObjectMapper mapper) {
        this.metadataFile = cacheRoot.resolve("metadata.json");
        this.mapper = mapper;
    }

    public synchronized Optional<CachedHeapDumpRecord> get(String fingerprint) {
        return readAll().values().stream()
                .filter(record -> record.fingerprint().equals(fingerprint))
                .findFirst();
    }

    public synchronized Optional<CachedHeapDumpRecord> getByProject(String projectName) {
        return Optional.ofNullable(readAll().get(projectName));
    }

    public synchronized Collection<CachedHeapDumpRecord> list() {
        return readAll().values();
    }

    public synchronized CachedHeapDumpRecord upsert(CachedHeapDumpRecord record) {
        Map<String, CachedHeapDumpRecord> all = readAll();
        all.put(record.projectName(), record);
        writeAll(all);
        return record;
    }

    public synchronized CachedHeapDumpRecord touchLoaded(CachedHeapDumpRecord existing) {
        CachedHeapDumpRecord updated = new CachedHeapDumpRecord(
                existing.projectName(),
                existing.fingerprint(),
                existing.sourcePath(),
                existing.cacheFileName(),
                existing.fileSizeBytes(),
                existing.lastModifiedTime(),
                existing.createdAt(),
                Instant.now(),
                existing.fullFileHash(),
                existing.cachedDumpSizeBytes()
        );
        return upsert(updated);
    }

    private Map<String, CachedHeapDumpRecord> readAll() {
        try {
            if (!Files.exists(metadataFile)) {
                return new LinkedHashMap<>();
            }
            return mapper.readValue(metadataFile.toFile(), TYPE);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read cache metadata from " + metadataFile, exception);
        }
    }

    private void writeAll(Map<String, CachedHeapDumpRecord> records) {
        try {
            Files.createDirectories(metadataFile.getParent());
            Path tempFile = metadataFile.resolveSibling(metadataFile.getFileName() + ".tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(tempFile.toFile(), records);
            Files.move(tempFile, metadataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write cache metadata to " + metadataFile, exception);
        }
    }
}
