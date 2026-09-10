package cchesser.javaperf.mcp.heap;

import java.nio.file.Path;
import java.util.Map;

public interface LoadedHeap {
    Path heapDumpPath();

    Map<String, Object> snapshotSummary();

    Object nativeSnapshot();

    void dispose();
}
