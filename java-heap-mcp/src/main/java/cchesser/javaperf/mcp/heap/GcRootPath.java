package cchesser.javaperf.mcp.heap;

import java.util.List;

public record GcRootPath(
        int terminalObjectId,
        List<ObjectReference> path
) {
}
