package cchesser.javaperf.mcp.heap;

public record HistogramEntry(
        int classId,
        String className,
        long objectCount,
        long shallowHeapBytes,
        Long retainedHeapBytes
) {
}
