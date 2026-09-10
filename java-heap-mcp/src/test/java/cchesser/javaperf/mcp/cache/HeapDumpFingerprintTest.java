package cchesser.javaperf.mcp.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class HeapDumpFingerprintTest {
    @TempDir
    Path tempDir;

    @Test
    void fingerprintIncludesPathSizeAndTimestamp() throws Exception {
        Path first = tempDir.resolve("first.hprof");
        Path second = tempDir.resolve("second.hprof");
        Files.writeString(first, "same-content");
        Files.writeString(second, "same-content");

        HeapDumpFingerprint firstFingerprint = HeapDumpFingerprint.from(first);
        HeapDumpFingerprint secondFingerprint = HeapDumpFingerprint.from(second);

        assertThat(firstFingerprint.fingerprint()).isNotEqualTo(secondFingerprint.fingerprint());
        assertThat(HeapDumpFingerprint.sha256File(first)).isEqualTo(HeapDumpFingerprint.sha256File(second));
    }
}
