package cchesser.javaperf.mcp.heap;

import java.util.List;

public record ObjectInspection(
        int objectId,
        long address,
        String className,
        String displayName,
        long shallowHeapBytes,
        long retainedHeapBytes,
        Integer immediateDominatorId,
        List<String> gcRootTypes,
        List<ObjectFieldValue> fields,
        List<ObjectReference> inboundReferences,
        List<ObjectReference> outboundReferences
) {
}
