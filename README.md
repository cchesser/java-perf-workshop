# Java Performance Workshop

A tutorial workshop that will dive in understanding "what's going on in your
JVM". This workshop is utilizing a basic web service that is included, which
isn't exactly optimal in how it performs. The goal is to use this service as an
interactive example and identify its poor performing elements. To learn more
about this web service that is included, see [the site](https://jvmperf.net/).

## Projects in this repository

| Project | Purpose | How to run |
| --- | --- | --- |
| `java-perf-workshop-server` | An intentionally inefficient Dropwizard web service used by the performance workshop. | Build the reactor, start WireMock for the remote dependency, then run the shaded server JAR. |
| `java-perf-workshop-tester` | Gatling and Scala simulations for exercising the workshop service under load. | Start the workshop server, then run the Gatling Maven goal. |
| `java-heap-mcp` | A Java MCP server for loading and analyzing `.hprof` heap dumps utilizing components of Eclipse MCP. | Run `scripts/run-server.sh` for MCP stdio, or enable its local HTTP UI. |
| `java-heap-workbench` | A browser UI that connects a local agent to the heap-analysis MCP server and renders visual results. | Build and serve it with the npm scripts after starting `java-heap-mcp`. |
| `java-heap-agent-example` | Reference material for an agent skill that explains how to query heap information with your coding agent | Read the skill documentation; it is not a Maven module or standalone service. |

The three Java services are part of the root Maven reactor. Build and test them
from the repository root with:

```bash
mvn test
```

The workshop server skips Docker image creation by default. To run the original
performance workshop locally, start the mocked upstream service first:

```bash
mvn dependency:copy \
  -Dartifact=com.github.tomakehurst:wiremock-standalone:2.27.2 \
  -Dmdep.stripVersion=true \
  -DoutputDirectory=.
java -jar wiremock-standalone.jar \
  --port 9090 \
  --root-dir java-perf-workshop-server/src/test/resources
```

In another terminal, build and start the workshop server:

```bash
mvn -pl java-perf-workshop-server package
java -jar java-perf-workshop-server/target/java-perf-workshop-server-2.0-SNAPSHOT.jar \
  server server.yml
```

The service is then available at <http://127.0.0.1:8080>. With the server and
WireMock running, execute the load simulations with:

```bash
mvn -pl java-perf-workshop-tester gatling:test
```

Results are written under `java-perf-workshop-tester/target/gatling/`.

## Java Heap Workbench

Java Heap Workbench combines a Java heap-analysis MCP server with a local-agent web interface. Load a `.hprof` dump, ask questions in natural language, and receive visual analysis such as summary cards, charts, tables, and OQL results.

### Repository modules

| Module | Purpose | Documentation |
| --- | --- | --- |
| `java-heap-mcp` | Java 25/Maven server backed by Eclipse Memory Analyzer (MAT). It loads heap dumps, manages cached projects, and exposes OQL and heap-analysis tools over MCP plus a local HTTP API. | [`java-heap-mcp/README.md`](java-heap-mcp/README.md) |
| `java-heap-workbench` | Browser UI that connects Ollama or OpenRouter to the MCP HTTP API, emits AG-UI-compatible run events, and renders A2UI-style cards, charts, and tables. | [`java-heap-workbench/README.md`](java-heap-workbench/README.md) |

The normal request path is:

```text
Browser workbench → Ollama or OpenRouter model → MCP HTTP API → Eclipse MAT → heap dump
        ↑                    ↓
        └──── visual A2UI response + AG-UI run events
```

The MCP server also supports its native stdio protocol for MCP clients. The workbench uses the server's local HTTP API because it runs directly in the browser.

### Prerequisites

- Java 25 or newer
- Maven (the repository includes a local `.m2` directory configuration)
- Node.js 20 or newer
- [Ollama](https://ollama.com/) with a tool-capable chat model, or an [OpenRouter](https://openrouter.ai/) API key
- A Java `.hprof` heap dump

### Complete setup

Run each long-lived process in its own terminal.

#### 1. Build and test the MCP server

From the repository root:

```bash
cd java-heap-mcp
mvn -Dmaven.repo.local=../.m2 test
```

The Maven build downloads the required MAT bundles into `java-heap-mcp/target/mat/`. See the [MCP module README](java-heap-mcp/README.md) for supported tools and configuration variables.

#### 2. Start the MCP HTTP service

The workbench needs the optional HTTP API enabled:

```bash
cd java-heap-mcp
JAVA_HEAP_MCP_WEB_UI_ENABLED=true \
JAVA_HEAP_MCP_WEB_UI_PORT=7777 \
./scripts/run-server.sh
```

This starts the MCP server and makes its local web/API service available at <http://127.0.0.1:7777>. The server's built-in page can be used to upload and index a heap dump. Keep this process running.

#### 3. Start Ollama (optional)

In another terminal:

```bash
ollama pull nemotron-3-nano:4b
OLLAMA_ORIGINS=http://127.0.0.1:4173 ollama serve
```

If Ollama is already running as a system service, configure its allowed origins using the platform-specific Ollama configuration instead. The browser must be allowed to call `http://127.0.0.1:11434` from the workbench origin.

#### 4. Build and start the workbench

In a third terminal:

```bash
cd java-heap-workbench
npm run build
npm run dev
```

Open <http://127.0.0.1:4173>. The workbench has no runtime npm dependencies; `npm install` is not required. The build creates the ignored `java-heap-workbench/dist/` directory.

#### 5. Connect the pieces

1. Open <http://127.0.0.1:7777>.
2. Select a `.hprof` file, optionally enter a project name, and load it.
3. Open <http://127.0.0.1:4173>.
4. Select the loaded project from the workbench project selector.
5. Open **Connections** if the defaults need changing:
   - MCP: `http://127.0.0.1:7777`
   - Provider: `Ollama (local)` or `OpenRouter`
   - Ollama URL: `http://127.0.0.1:11434` (when using Ollama)
   - OpenRouter API URL: `https://openrouter.ai/api/v1` (when using OpenRouter)
   - Model: `nemotron-3-nano:4b` for Ollama, or one of the indexed OpenRouter free coding models
6. Use a quick investigation or enter a question in the chat box.

The agent can call overview, histogram, dominator, leak-suspect, OQL, object inspection, and GC-root path operations. Tool results are returned to the model and also rendered as visual A2UI components. Questions about class attributes are grounded with a valid MAT OQL object selection followed by object inspection, so the response includes actual fields and values when the heap exposes them.

Agent-generated OQL uses the embedded Apache Calcite SQL dialect. Use one read-only `SELECT` statement with SQL grouping, aggregates, ordering, joins, and other supported clauses; do not generate MAT-only syntax. Heap functions are SQL functions, so use `toString(s.this)`, never Java-like alias method syntax such as `s.toString(s.this)`. The workbench sends a tool-level row limit separately, but SQL `LIMIT`/`OFFSET` may be used when they are part of the query's intended semantics.

### Testing the complete flow

Test the frontend build:

```bash
cd java-heap-workbench
npm test
npm run build
```

Test a real interaction after both services are running:

```text
Show me the top 10 classes by retained heap and visualize them as a bar chart.
```

Other useful prompts:

- `Give me a high-level overview of this heap and highlight the biggest retention risks.`
- `Find leak suspects and summarize the evidence in cards and a table.`
- `Run an OQL query for the most common String values and show the results.`

The workbench emits AG-UI-compatible `RUN_STARTED`, tool-call, text, state, finish, and error events. The A2UI renderer currently supports summary cards, tables, bar charts, pie charts, and markdown-style text responses.

### Troubleshooting

- **MCP offline:** confirm the Java server was started with `JAVA_HEAP_MCP_WEB_UI_ENABLED=true` and that port `7777` is available.
- **No project appears:** load or open a project in the MCP page first; cached projects without active handles must be opened before analysis.
- **Model request fails:** verify the selected provider, endpoint, model name, and (for OpenRouter) API key. Ollama also requires `OLLAMA_ORIGINS=http://127.0.0.1:4173`.
- **Large dump is slow:** MAT indexing and retained-heap calculations can take time. Keep the MCP process running while analysis completes.
- **Browser is using stale settings:** open **Connections**, save the correct URLs/model, and retry.

For module-specific environment variables, MCP tools, licensing, and implementation details, see the [MCP README](java-heap-mcp/README.md) and [workbench README](java-heap-workbench/README.md).

## References

* [mat-calcite-plugin](https://github.com/vlsi/mat-calcite-plugin): Including some of the source files for the java-heap-mcp integration of Apache Calcite.
* 
