package cchesser.javaperf.mcp.mcp;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public record McpTool(
        String name,
        String description,
        Map<String, Object> inputSchema,
        ToolHandler handler
) {
    @FunctionalInterface
    public interface ToolHandler {
        Object invoke(JsonNode arguments);
    }
}
