package cchesser.javaperf.mcp.heap;

public record DominatorEntry(
        int objectId,
        String className,
        long shallowHeapBytes,
        long retainedHeapBytes,
        String displayName
) {
}
