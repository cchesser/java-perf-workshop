package cchesser.javaperf.mcp.heap;

public record ObjectReference(
        int objectId,
        String className,
        long address,
        String displayName
) {
}
