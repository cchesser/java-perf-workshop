package cchesser.javaperf.mcp.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import cchesser.javaperf.mcp.heap.HeapDumpManager;
import cchesser.javaperf.mcp.heap.HeapOperationException;
import cchesser.javaperf.mcp.heap.OqlGrammar;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public final class WebUiServer implements AutoCloseable {
    private static final int STREAM_BUFFER_BYTES = 1024 * 1024;

    private final HttpServer server;
    private final ObjectMapper mapper;
    private final HeapDumpManager manager;
    private final Path uploadRoot;
    private final long maxUploadBytes;

    public WebUiServer(int port, Path cacheRoot, long maxUploadBytes, ObjectMapper mapper, HeapDumpManager manager) throws IOException {
        this.mapper = mapper;
        this.manager = manager;
        this.uploadRoot = cacheRoot.resolve("uploads");
        this.maxUploadBytes = maxUploadBytes;
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        this.server.setExecutor(Executors.newCachedThreadPool());
        registerRoutes();
    }

    public void start() {
        server.start();
    }

    private void registerRoutes() {
        registerRoute("/", this::handleRoot);
        registerRoute("/api/projects", exchange -> handleJsonGet(exchange, manager::listProjects));
        registerRoute("/api/dumps", exchange -> handleJsonGet(exchange, manager::listDumps));
        registerRoute("/api/load", exchange -> handleJsonPost(exchange, body ->
                manager.loadDump(textOrNull(body, "project"), text(body, "path"))));
        registerRoute("/api/load-upload", this::handleLoadUpload);
        registerRoute("/api/open-project", exchange -> handleJsonPost(exchange, body ->
                manager.openProject(text(body, "project"))));
        registerRoute("/api/unload", exchange -> handleJsonPost(exchange, body ->
                manager.unloadDump(text(body, "handle"))));
        registerRoute("/api/overview", exchange -> handleJsonPost(exchange, body ->
                manager.overview(text(body, "handle"), integer(body, "limit"))));
        registerRoute("/api/histogram", exchange -> handleJsonPost(exchange, body ->
                manager.histogram(text(body, "handle"), textOrNull(body, "sort"), integer(body, "limit"))));
        registerRoute("/api/dominators", exchange -> handleJsonPost(exchange, body ->
                manager.dominators(text(body, "handle"), integer(body, "root"), integer(body, "limit"))));
        registerRoute("/api/leaks", exchange -> handleJsonPost(exchange, body ->
                manager.findLeakSuspects(text(body, "handle"), integer(body, "limit"))));
        registerRoute("/api/inspect", exchange -> handleJsonPost(exchange, body ->
                manager.inspectObject(text(body, "handle"), requiredInteger(body, "objectId"), integer(body, "limit"))));
        registerRoute("/api/gc-roots", exchange -> handleJsonPost(exchange, body ->
                manager.findPathsToGcRoots(text(body, "handle"), requiredInteger(body, "objectId"),
                        booleanOrDefault(body, "excludeWeakRefs", false), integer(body, "limit"))));
        registerRoute("/api/oql", exchange -> handleJsonPost(exchange, body ->
                manager.runOql(text(body, "handle"), text(body, "query"), integer(body, "limit"), integer(body, "offset"))));
        registerRoute("/api/oql-grammar", exchange -> handleJsonGet(exchange, OqlGrammar::describe));
        registerRoute("/api/oql/grammar", exchange -> handleJsonGet(exchange, OqlGrammar::describe));
    }

    private void registerRoute(String path, HttpHandler handler) {
        server.createContext(path, exchange -> {
            long startedAt = System.nanoTime();
            try {
                handler.handle(exchange);
            } finally {
                long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
                System.err.printf("[java-heap-mcp] %s %s -> %d (%d ms)%n",
                        exchange.getRequestMethod(), exchange.getRequestURI(), exchange.getResponseCode(), elapsedMillis);
            }
        });
    }

    private void handleRoot(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        try (InputStream resource = getClass().getResourceAsStream("/web/index.html")) {
            if (resource == null) {
                writeJson(exchange, 500, Map.of("error", "Missing web UI resource"));
                return;
            }
            byte[] bytes = resource.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        } finally {
            exchange.close();
        }
    }

    private void handleJsonGet(HttpExchange exchange, Supplier<Object> handler) throws IOException {
        if (handleCorsPreflight(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        try {
            writeJson(exchange, 200, handler.get());
        } catch (HeapOperationException exception) {
            logFailure(exchange, exception);
            writeJson(exchange, 400, operationError(exception));
        } catch (IllegalArgumentException exception) {
            logFailure(exchange, exception);
            writeJson(exchange, 400, Map.of("error", exception.getMessage()));
        } catch (Exception exception) {
            logFailure(exchange, exception);
            writeJson(exchange, 500, Map.of("error", exception.getMessage()));
        }
    }

    private void handleJsonPost(HttpExchange exchange, PostHandler handler) throws IOException {
        if (handleCorsPreflight(exchange)) {
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        try {
            JsonNode body = readJsonBody(exchange);
            writeJson(exchange, 200, handler.handle(body));
        } catch (HeapOperationException exception) {
            logFailure(exchange, exception);
            writeJson(exchange, 400, operationError(exception));
        } catch (IllegalArgumentException exception) {
            logFailure(exchange, exception);
            writeJson(exchange, 400, Map.of("error", exception.getMessage()));
        } catch (Exception exception) {
            logFailure(exchange, exception);
            writeJson(exchange, 500, Map.of("error", exception.getMessage()));
        }
    }

    private void handleLoadUpload(HttpExchange exchange) throws IOException {
        if (handleCorsPreflight(exchange)) {
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, Map.of("error", "Method not allowed"));
            return;
        }
        try {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            String project = query.get("project");
            String fileName = query.get("filename");
            if (fileName == null || fileName.isBlank()) {
                throw new IllegalArgumentException("Missing required query parameter: filename");
            }
            String effectiveProject = project == null || project.isBlank() ? null : project.trim();
            Path projectDir = uploadRoot.resolve(safeSegment(effectiveProject == null ? "incoming" : effectiveProject));
            Files.createDirectories(projectDir);

            String cleanName = safeFileName(fileName);
            Path uploadedPath = projectDir.resolve(System.currentTimeMillis() + "-" + cleanName);
            Path tempPath = uploadedPath.resolveSibling(uploadedPath.getFileName() + ".tmp");
            try (InputStream body = exchange.getRequestBody()) {
                streamToFile(body, tempPath);
            }
            Files.move(tempPath, uploadedPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            writeJson(exchange, 200, manager.loadDump(effectiveProject, uploadedPath.toString()));
        } catch (HeapOperationException exception) {
            logFailure(exchange, exception);
            writeJson(exchange, 400, operationError(exception));
        } catch (IllegalArgumentException exception) {
            logFailure(exchange, exception);
            writeJson(exchange, 400, Map.of("error", exception.getMessage()));
        } catch (Exception exception) {
            logFailure(exchange, exception);
            writeJson(exchange, 500, Map.of("error", exception.getMessage()));
        }
    }

    private JsonNode readJsonBody(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        if (bytes.length == 0) {
            return mapper.nullNode();
        }
        return mapper.readTree(bytes);
    }

    private void writeJson(HttpExchange exchange, int statusCode, Object body) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(body);
        addCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static Map<String, Object> operationError(HeapOperationException exception) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("error", exception.getMessage());
        body.put("errorCode", exception.errorCode().name());
        if (exception.getCause() != null && exception.getCause().getMessage() != null) {
            body.put("cause", exception.getCause().getMessage());
        }
        return body;
    }

    private boolean handleCorsPreflight(HttpExchange exchange) throws IOException {
        if (!"OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            addCorsHeaders(exchange);
            return false;
        }
        addCorsHeaders(exchange);
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
        return true;
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Access-Control-Max-Age", "86400");
    }

    private static void logFailure(HttpExchange exchange, Exception exception) {
        System.err.printf("[java-heap-mcp] %s %s failed: %s%n",
                exchange.getRequestMethod(), exchange.getRequestURI(), exception.getMessage());
        exception.printStackTrace(System.err);
    }

    private static String text(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return value.asText();
    }

    private static String textOrNull(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String result = value.asText();
        return result.isBlank() ? null : result;
    }

    private static Integer integer(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asInt();
    }

    private static int requiredInteger(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        if (value.isIntegralNumber()) {
            return value.asInt();
        }
        if (value.isTextual()) {
            try {
                return Integer.parseInt(value.asText().trim());
            } catch (NumberFormatException ignored) {
                // Fall through to the consistent missing/invalid-field response.
            }
        }
        throw new IllegalArgumentException("Field must be an integer: " + field);
    }

    private static boolean booleanOrDefault(JsonNode body, String field, boolean defaultValue) {
        JsonNode value = body.get(field);
        return value == null || value.isNull() ? defaultValue : value.asBoolean(defaultValue);
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return Map.of();
        }
        return java.util.Arrays.stream(rawQuery.split("&"))
                .map(part -> part.split("=", 2))
                .collect(java.util.stream.Collectors.toMap(
                        pair -> decodeQueryComponent(pair[0]),
                        pair -> pair.length > 1 ? decodeQueryComponent(pair[1]) : "",
                        (left, right) -> right
                ));
    }

    private static String decodeQueryComponent(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String safeSegment(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String safeFileName(String value) {
        String normalized = value.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        String base = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
        if (base.isBlank()) {
            return "uploaded.hprof";
        }
        return base.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void streamToFile(InputStream input, Path destination) throws IOException {
        byte[] buffer = new byte[STREAM_BUFFER_BYTES];
        long totalWritten = 0L;
        try (var output = Files.newOutputStream(destination, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                totalWritten += read;
                if (maxUploadBytes > 0 && totalWritten > maxUploadBytes) {
                    throw new IllegalArgumentException("Upload exceeds configured max bytes: " + maxUploadBytes);
                }
                output.write(buffer, 0, read);
            }
        } catch (Exception exception) {
            Files.deleteIfExists(destination);
            throw exception;
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }

    @FunctionalInterface
    private interface PostHandler {
        Object handle(JsonNode body);
    }
}
