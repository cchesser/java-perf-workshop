package cchesser.javaperf.mcp.cache;

import java.nio.file.Path;
import java.time.Instant;

public record CachedHeapDumpRecord(
        String projectName,
        String fingerprint,
        String sourcePath,
        String cacheFileName,
        long fileSizeBytes,
        Instant lastModifiedTime,
        Instant createdAt,
        Instant lastLoadedAt,
        String fullFileHash,
        long cachedDumpSizeBytes
) {
    public Path sourcePathAsPath() {
        return Path.of(sourcePath);
    }
}
