package cchesser.javaperf.mcp.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

/** Owns the official MCP SDK server and keeps the process alive until stdio closes. */
public final class McpServerEngine implements AutoCloseable {
    private final McpSyncServer server;
    private final CountDownLatch inputClosed = new CountDownLatch(1);

    public McpServerEngine(ObjectMapper mapper, ToolRegistry registry, InputStream input, OutputStream output) {
        JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(mapper);
        StdioServerTransportProvider transport = new StdioServerTransportProvider(
                jsonMapper, new EofAwareInputStream(input, inputClosed), output);
        this.server = McpServer.sync(transport)
                .serverInfo("java-heap-mcp", "0.1.0")
                .tools(registry.specifications())
                .jsonMapper(jsonMapper)
                .immediateExecution(true)
                .build();
    }

    public void run() throws InterruptedException {
        inputClosed.await();
    }

    @Override
    public void close() {
        server.closeGracefully();
        inputClosed.countDown();
    }

    private static final class EofAwareInputStream extends InputStream {
        private final InputStream delegate;
        private final CountDownLatch eof;

        private EofAwareInputStream(InputStream delegate, CountDownLatch eof) {
            this.delegate = delegate;
            this.eof = eof;
        }

        @Override public int read() throws IOException { int value = delegate.read(); if (value < 0) eof.countDown(); return value; }
        @Override public int read(byte[] bytes, int offset, int length) throws IOException { int value = delegate.read(bytes, offset, length); if (value < 0) eof.countDown(); return value; }
        @Override public void close() throws IOException { delegate.close(); eof.countDown(); }
    }
}
