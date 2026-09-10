package cchesser.javaperf.mcp.config;

import java.nio.file.Path;
import java.time.Duration;

public record ServerConfig(
        Path cacheRoot,
        int defaultRowLimit,
        int maxRowLimit,
        Duration toolTimeout,
        int gcRootPathLimit,
        boolean webUiEnabled,
        int webUiPort,
        long webUiMaxUploadBytes
) {
    private static final String CACHE_ENV = "JAVA_HEAP_MCP_CACHE_DIR";
    private static final String DEFAULT_LIMIT_ENV = "JAVA_HEAP_MCP_DEFAULT_LIMIT";
    private static final String MAX_LIMIT_ENV = "JAVA_HEAP_MCP_MAX_LIMIT";
    private static final String TOOL_TIMEOUT_SECONDS_ENV = "JAVA_HEAP_MCP_TOOL_TIMEOUT_SECONDS";
    private static final String GC_ROOT_PATH_LIMIT_ENV = "JAVA_HEAP_MCP_GC_ROOT_PATH_LIMIT";
    private static final String WEB_UI_ENABLED_ENV = "JAVA_HEAP_MCP_WEB_UI_ENABLED";
    private static final String WEB_UI_PORT_ENV = "JAVA_HEAP_MCP_WEB_UI_PORT";
    private static final String WEB_UI_MAX_UPLOAD_BYTES_ENV = "JAVA_HEAP_MCP_WEB_UI_MAX_UPLOAD_BYTES";

    public static ServerConfig fromEnvironment() {
        Path cacheRoot = Path.of(System.getenv().getOrDefault(
                CACHE_ENV,
                Path.of(System.getProperty("user.home"), ".cache", "java-heap-mcp").toString()
        ));

        return new ServerConfig(
                cacheRoot,
                intEnv(DEFAULT_LIMIT_ENV, 50),
                intEnv(MAX_LIMIT_ENV, 250),
                Duration.ofSeconds(intEnv(TOOL_TIMEOUT_SECONDS_ENV, 120)),
                intEnv(GC_ROOT_PATH_LIMIT_ENV, 10),
                boolEnv(WEB_UI_ENABLED_ENV, false),
                intEnv(WEB_UI_PORT_ENV, 7777),
                longEnv(WEB_UI_MAX_UPLOAD_BYTES_ENV, 0L)
        );
    }

    public int normalizeLimit(Integer requestedLimit) {
        int limit = requestedLimit == null ? defaultRowLimit : requestedLimit;
        return Math.max(1, Math.min(limit, maxRowLimit));
    }

    private static int intEnv(String env, int defaultValue) {
        String value = System.getenv(env);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    private static boolean boolEnv(String env, boolean defaultValue) {
        String value = System.getenv(env);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return "1".equals(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
    }

    private static long longEnv(String env, long defaultValue) {
        String value = System.getenv(env);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Long.parseLong(value);
    }
}
