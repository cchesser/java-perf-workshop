package cchesser.javaperf.mcp.server;

import cchesser.javaperf.mcp.cache.CacheMetadataStore;
import cchesser.javaperf.mcp.config.ServerConfig;
import cchesser.javaperf.mcp.heap.HeapDumpManager;
import cchesser.javaperf.mcp.heap.MatHeapService;
import cchesser.javaperf.mcp.mcp.McpServerEngine;
import cchesser.javaperf.mcp.mcp.ToolRegistry;
import cchesser.javaperf.mcp.util.JsonSupport;
import cchesser.javaperf.mcp.web.WebUiServer;

public final class HeapMcpServerApplication {
    private HeapMcpServerApplication() {
    }

    public static void main(String[] args) throws Exception {
        ServerConfig config = ServerConfig.fromEnvironment();
        JsonSupport jsonSupport = JsonSupport.create();
        CacheMetadataStore metadataStore = new CacheMetadataStore(config.cacheRoot(), jsonSupport.mapper());
        MatHeapService heapService = new MatHeapService(config);
        HeapDumpManager heapDumpManager = new HeapDumpManager(config, metadataStore, heapService);
        ToolRegistry toolRegistry = new ToolRegistry(heapDumpManager, jsonSupport.mapper(), config);
        WebUiServer webUiServer = null;
        if (config.webUiEnabled()) {
            webUiServer = new WebUiServer(
                    config.webUiPort(),
                    config.cacheRoot(),
                    config.webUiMaxUploadBytes(),
                    jsonSupport.mapper(),
                    heapDumpManager
            );
            webUiServer.start();
            System.err.println("java-heap-mcp web UI available at http://127.0.0.1:" + config.webUiPort() + "/");
        }

        try (McpServerEngine engine = new McpServerEngine(jsonSupport.mapper(), toolRegistry, System.in, System.out)) {
            engine.run();
        } finally {
            if (webUiServer != null) {
                webUiServer.close();
            }
            heapDumpManager.close();
        }
    }
}
