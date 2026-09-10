package cchesser.javaperf.mcp.heap;

import java.nio.file.Path;
import java.util.List;

public interface HeapAnalysisService extends AutoCloseable {
    LoadedHeap open(Path heapDumpPath);

    HeapOverview overview(LoadedHeap heap, int limit);

    OqlQueryResult runOql(LoadedHeap heap, String query, int limit, int offset);

    List<HistogramEntry> histogram(LoadedHeap heap, String sortBy, int limit);

    List<DominatorEntry> dominators(LoadedHeap heap, Integer rootObjectId, int limit);

    ObjectInspection inspectObject(LoadedHeap heap, int objectId, int limit);

    List<GcRootPath> findPathsToGcRoots(LoadedHeap heap, int objectId, boolean excludeWeakRefs, int limit);

    Object leakSuspects(LoadedHeap heap, int limit);

    @Override
    void close();
}
