package cchesser.javaperf.mcp.heap;

import java.util.List;
import java.util.Map;

public record HeapOverview(
        Map<String, Object> snapshot,
        List<HistogramEntry> topClasses,
        List<DominatorEntry> topDominators
) {
}
