package cchesser.javaperf.mcp.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cchesser.javaperf.mcp.config.ServerConfig;
import cchesser.javaperf.mcp.heap.HeapDumpManager;
import cchesser.javaperf.mcp.heap.HeapOperationException;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Adapts heap operations to the official MCP Java SDK tool model. */
public final class ToolRegistry {
    private final Map<String, McpTool> tools;
    private final ObjectMapper mapper;

    public ToolRegistry(HeapDumpManager manager, ObjectMapper mapper, ServerConfig config) {
        this.mapper = mapper;
        this.tools = registerTools(manager, config);
    }

    public List<McpServerFeatures.SyncToolSpecification> specifications() {
        return tools.values().stream().map(this::specification).toList();
    }

    private McpServerFeatures.SyncToolSpecification specification(McpTool tool) {
        McpSchema.Tool definition = McpSchema.Tool.builder()
                .name(tool.name())
                .description(tool.description())
                .inputSchema(schema(tool.inputSchema()))
                .build();

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(definition)
                .callHandler((exchange, request) -> {
                    long startedAt = System.nanoTime();
                    System.err.printf("[java-heap-mcp] MCP tool %s called%n", tool.name());
                    try {
                        Object result = tool.handler().invoke(mapper.valueToTree(request.arguments()));
                        logToolCompletion(tool.name(), startedAt, "ok");
                        return McpSchema.CallToolResult.builder()
                                .addTextContent(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result))
                                .structuredContent(result)
                                .isError(false)
                                .build();
                    } catch (HeapOperationException exception) {
                        logToolCompletion(tool.name(), startedAt, exception.errorCode().name());
                        return errorResult(exception.errorCode().name(), exception.getMessage());
                    } catch (Exception exception) {
                        logToolCompletion(tool.name(), startedAt, "INTERNAL");
                        return errorResult("INTERNAL", exception.getMessage());
                    }
                })
                .build();
    }

    private static void logToolCompletion(String toolName, long startedAt, String outcome) {
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        System.err.printf("[java-heap-mcp] MCP tool %s -> %s (%d ms)%n", toolName, outcome, elapsedMillis);
    }

    @SuppressWarnings("unchecked")
    private static McpSchema.JsonSchema schema(Map<String, Object> schema) {
        return new McpSchema.JsonSchema(
                (String) schema.get("type"),
                (Map<String, Object>) schema.getOrDefault("properties", Map.of()),
                (List<String>) schema.getOrDefault("required", List.of()),
                null,
                Map.of(),
                Map.of()
        );
    }

    private McpSchema.CallToolResult errorResult(String errorCode, String message) {
        Map<String, Object> errorBody = Map.of(
                "errorCode", errorCode,
                "message", message == null ? errorCode : message
        );
        try {
            return McpSchema.CallToolResult.builder()
                    .addTextContent(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(errorBody))
                    .structuredContent(errorBody)
                    .isError(true)
                    .build();
        } catch (Exception exception) {
            return McpSchema.CallToolResult.builder()
                    .addTextContent(String.valueOf(errorBody))
                    .isError(true)
                    .build();
        }
    }

    private Map<String, McpTool> registerTools(HeapDumpManager manager, ServerConfig config) {
        Map<String, McpTool> registered = new LinkedHashMap<>();
        registered.put("heap_load_dump", new McpTool("heap_load_dump", "Load a Java heap dump into a named project, build or reuse MAT indexes, and return a session handle.", McpSchemaHelper.objectSchema(Map.of("project", McpSchemaHelper.string(), "path", McpSchemaHelper.string()), List.of("path")), a -> manager.loadDump(textOrNull(a, "project"), text(a, "path"))));
        registered.put("heap_open_project", new McpTool("heap_open_project", "Open an existing cached project and return an active heap handle.", McpSchemaHelper.objectSchema(Map.of("project", McpSchemaHelper.string()), List.of("project")), a -> manager.openProject(text(a, "project"))));
        registered.put("heap_list_projects", new McpTool("heap_list_projects", "List named projects that have cached heap dumps and show active handles.", McpSchemaHelper.objectSchema(Map.of(), List.of()), a -> manager.listProjects()));
        registered.put("heap_list_dumps", new McpTool("heap_list_dumps", "List cached heap dumps, their metadata, and any currently active handles.", McpSchemaHelper.objectSchema(Map.of(), List.of()), a -> manager.listDumps()));
        registered.put("heap_unload_dump", new McpTool("heap_unload_dump", "Unload an active heap handle while preserving its on-disk cache entry and indexes.", McpSchemaHelper.objectSchema(Map.of("handle", McpSchemaHelper.string()), List.of("handle")), a -> manager.unloadDump(text(a, "handle"))));
        registered.put("heap_get_overview", new McpTool("heap_get_overview", "Return snapshot summary, top classes, and top retained-memory dominators for a loaded heap.", McpSchemaHelper.objectSchema(Map.of("handle", McpSchemaHelper.string(), "limit", McpSchemaHelper.integer()), List.of("handle")), a -> manager.overview(text(a, "handle"), integer(a, "limit"))));
        registered.put("heap_run_oql", new McpTool("heap_run_oql", "Execute an OQL query and return bounded, LLM-friendly rows with pagination.", McpSchemaHelper.objectSchema(Map.of("handle", McpSchemaHelper.string(), "query", McpSchemaHelper.string(), "limit", McpSchemaHelper.integer(), "offset", McpSchemaHelper.integer()), List.of("handle", "query")), a -> manager.runOql(text(a, "handle"), text(a, "query"), integer(a, "limit"), integer(a, "offset"))));
        registered.put("heap_get_oql_grammar", new McpTool("heap_get_oql_grammar", "Return the read-only Apache Calcite SQL grammar, MAT heap schema, functions, and examples accepted by heap_run_oql.", McpSchemaHelper.objectSchema(Map.of(), List.of()), a -> cchesser.javaperf.mcp.heap.OqlGrammar.describe()));
        registered.put("heap_get_histogram", new McpTool("heap_get_histogram", "Return a bounded class histogram ordered by retained heap, shallow heap, or object count.", McpSchemaHelper.objectSchema(Map.of("handle", McpSchemaHelper.string(), "sort", McpSchemaHelper.string(), "limit", McpSchemaHelper.integer()), List.of("handle")), a -> manager.histogram(text(a, "handle"), textOrNull(a, "sort"), integer(a, "limit"))));
        registered.put("heap_get_dominators", new McpTool("heap_get_dominators", "Return retained-memory dominators from the heap root or from a supplied object root.", McpSchemaHelper.objectSchema(Map.of("handle", McpSchemaHelper.string(), "root", McpSchemaHelper.integer(), "limit", McpSchemaHelper.integer()), List.of("handle")), a -> manager.dominators(text(a, "handle"), integer(a, "root"), integer(a, "limit"))));
        registered.put("heap_inspect_object", new McpTool("heap_inspect_object", "Inspect a heap object including class, sizes, fields, and inbound/outbound references.", McpSchemaHelper.objectSchema(Map.of("handle", McpSchemaHelper.string(), "objectId", McpSchemaHelper.integer(), "limit", McpSchemaHelper.integer()), List.of("handle", "objectId")), a -> manager.inspectObject(text(a, "handle"), requiredInteger(a, "objectId"), integer(a, "limit"))));
        registered.put("heap_find_path_to_gc_roots", new McpTool("heap_find_path_to_gc_roots", "Find bounded object-reference paths from the target object to GC roots.", McpSchemaHelper.objectSchema(Map.of("handle", McpSchemaHelper.string(), "objectId", McpSchemaHelper.integer(), "excludeWeakRefs", McpSchemaHelper.bool(), "limit", McpSchemaHelper.integer()), List.of("handle", "objectId")), a -> manager.findPathsToGcRoots(text(a, "handle"), requiredInteger(a, "objectId"), booleanValue(a, "excludeWeakRefs"), integer(a, "limit"))));
        registered.put("heap_find_leak_suspects", new McpTool("heap_find_leak_suspects", "Run MAT leak-suspect analysis when available and return a bounded summary.", McpSchemaHelper.objectSchema(Map.of("handle", McpSchemaHelper.string(), "limit", McpSchemaHelper.integer()), List.of("handle")), a -> manager.findLeakSuspects(text(a, "handle"), integer(a, "limit"))));
        return registered;
    }

    private static String text(JsonNode a, String f) { JsonNode v = a.get(f); if (v == null || v.isNull() || v.asText().isBlank()) throw new IllegalArgumentException("Missing required argument: " + f); return v.asText(); }
    private static String textOrNull(JsonNode a, String f) { JsonNode v = a.get(f); return v == null || v.isNull() ? null : v.asText(); }
    private static Integer integer(JsonNode a, String f) { JsonNode v = a.get(f); return v == null || v.isNull() ? null : v.asInt(); }
    private static int requiredInteger(JsonNode a, String f) { JsonNode v = a.get(f); if (v == null || v.isNull()) throw new IllegalArgumentException("Missing required argument: " + f); return v.asInt(); }
    private static boolean booleanValue(JsonNode a, String f) { JsonNode v = a.get(f); return v != null && !v.isNull() && v.asBoolean(false); }

    private static final class McpSchemaHelper {
        private McpSchemaHelper() { }
        static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) { return Map.of("type", "object", "properties", properties, "required", required); }
        static Map<String, Object> string() { return Map.of("type", "string"); }
        static Map<String, Object> integer() { return Map.of("type", "integer"); }
        static Map<String, Object> bool() { return Map.of("type", "boolean"); }
    }
}
