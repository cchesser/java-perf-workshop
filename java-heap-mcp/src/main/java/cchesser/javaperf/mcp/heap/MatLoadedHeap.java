package cchesser.javaperf.mcp.heap;

import org.eclipse.mat.snapshot.ISnapshot;

import java.nio.file.Path;
import java.util.Map;

public record MatLoadedHeap(
        Path heapDumpPath,
        ISnapshot snapshot,
        Map<String, Object> snapshotSummary
) implements LoadedHeap {
    @Override
    public Object nativeSnapshot() {
        return snapshot;
    }

    @Override
    public void dispose() {
        try {
            org.eclipse.mat.snapshot.SnapshotFactory.dispose(snapshot);
        } catch (RuntimeException ignored) {
            // Best-effort disposal during shutdown or handle close.
        }
    }
}
