package cchesser.javaperf.mcp.heap;

import java.util.List;
import java.util.Map;

public record OqlQueryResult(
        String query,
        List<String> columns,
        List<Map<String, Object>> rows,
        int offset,
        int limit,
        boolean truncated,
        int totalRows
) {
}
