package cchesser.javaperf.mcp.heap;

public record ObjectFieldValue(
        String name,
        String type,
        Object value,
        Integer referencedObjectId
) {
}
