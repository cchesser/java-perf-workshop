package cchesser.javaperf.mcp.cache;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

public record HeapDumpFingerprint(
        String fingerprint,
        Path sourcePath,
        long fileSizeBytes,
        Instant lastModifiedTime
) {
    public static HeapDumpFingerprint from(Path sourcePath) throws IOException {
        Path normalized = sourcePath.toAbsolutePath().normalize();
        long fileSize = Files.size(normalized);
        Instant lastModified = Files.getLastModifiedTime(normalized).toInstant();
        String payload = normalized + "|" + fileSize + "|" + lastModified.toEpochMilli();
        return new HeapDumpFingerprint(sha256(payload.getBytes()), normalized, fileSize, lastModified);
    }

    public String sourceFileName() {
        return sourcePath.getFileName().toString();
    }

    public static String sha256File(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) >= 0) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }
}
