# java-heap-workbench

`java-heap-workbench` is a browser workspace for asking an agent questions about Java heap dumps. Its focused split-pane layout keeps the Agent chat on the left and A2UI-rendered visual analysis on the right. It supports Ollama locally and OpenRouter through its OpenAI-compatible chat-completions API, with the existing `java-heap-mcp` HTTP API providing heap tools.

## Build and run

Requirements: Node.js 20+ and a running `java-heap-mcp` server.

```bash
cd java-heap-workbench
npm run build
npm run dev
```

Open <http://127.0.0.1:4173>. The app is intentionally dependency-light: `npm install` is not required. `npm run build` copies the browser application to `dist/`; `npm run dev` serves that build.

Start the MCP server from the sibling project:

```bash
cd ../java-heap-mcp
JAVA_HEAP_MCP_WEB_UI_ENABLED=true ./scripts/run-server.sh
```

Use the MCP server's existing web UI at <http://127.0.0.1:7777> to load a `.hprof` file into a named project. Return to the workbench and select that project.

## Connect Ollama or OpenRouter

Install Ollama, start it, and pull a tool-capable chat model:

```bash
ollama serve
ollama pull nemotron-3-nano:4b
```

If the browser blocks requests, allow the workbench origin when starting Ollama:

```bash
OLLAMA_ORIGINS=http://127.0.0.1:4173 ollama serve
```

Open **Connections** in the workbench and choose a provider. Ollama uses `http://127.0.0.1:11434` and a locally pulled model. OpenRouter uses `https://openrouter.ai/api/v1`, requires an API key, and accepts free coding models such as:

- `cohere/north-mini-code:free`
- `nvidia/nemotron-3.5-lightning:free`
- `qwen/qwen3-coder:free`
- `openrouter/free` (automatic free-model routing)

The workbench includes these model IDs in the OpenRouter model picker. OpenRouter free-model availability can change; confirm the current list at [OpenRouter's free models](https://openrouter.ai/models?variant=free). The API key is stored in browser local storage for this local-only UI, so do not use this setup for a shared or untrusted browser profile.

## How the integration works

- `src/ag-ui.js` emits `RUN_STARTED`, text, tool-call, state, finish, and error events. This is the seam for replacing the local loop with an AG-UI transport/server.
- `src/main.js` gives the selected Ollama or OpenRouter model the MCP analysis tools. Tool calls are routed to `/api/overview`, `/api/histogram`, `/api/dominators`, `/api/leaks`, `/api/oql`, `/api/inspect`, and `/api/gc-roots`, then returned to the model for follow-up reasoning.
- `src/agent-prompt.js` supplies the agent with the tool contract and Calcite investigation rules. Attribute questions must be grounded by selecting an object with OQL and inspecting its returned object id; the agent must not claim success without tool evidence.
- `src/a2ui.js` renders the resulting declarative components: `summary`, `chart` (bar, line, or pie), `table`, and `markdown`. The renderer also converts legacy markdown tables into real table components. It uses safe DOM strings and SVG charts, so no chart CDN is needed.
- The agent prompt requests JSON visual responses with a `components` array. If a model still returns a markdown table, the workbench parses it as a fallback; numeric tool results are visualized immediately while the model is preparing its final response.

## Test an interaction

1. Load `my_little_heap_dump.hprof` (or another dump) in the MCP web UI.
2. Select the active project in the workbench.
3. Click **Largest retained classes**, or ask “Show me the top 10 classes by retained heap and visualize them as a bar chart.”
4. Watch the run details for AG-UI events and the visual report for A2UI cards/table/chart output.
5. Try an OQL request, for example: `Find String objects where the count is greater than 10.` The agent emits `SELECT * FROM java.lang.String s WHERE s.count > 10`; result limits are sent separately to the MCP tool.

The browser calls only local services. No heap data or model prompts leave the machine unless you configure URLs that point elsewhere.

OQL is the workbench name for the embedded [Apache Calcite SQL](https://calcite.apache.org/docs/reference.html) heap-query language. Only read-only `SELECT` statements are supported. Formulate every OQL query as a Calcite `SELECT`; use SQL joins, grouping, aggregates, ordering, pagination, and lateral table functions. The server may retain MAT compatibility internally, but the agent should not generate MAT OQL syntax.

### Advanced Calcite OQL patterns

The agent can use these Calcite SQL techniques for deeper investigations:

- Each Java class is a Calcite table of direct instances; use the `instanceof` schema/table for subclasses. The special `this` column identifies the heap object, and Java fields are SQL columns.
- Use aliases and SQL expressions such as `COUNT`, `SUM`, `GROUP BY`, `HAVING`, `ORDER BY`, `JOIN`, `LATERAL TABLE(...)`, `UNNEST`, and `LIMIT/OFFSET`.
- Use `this['fieldName']` for dynamic fields and Calcite heap functions such as `toString`, `retainedSize`, `shallowSize`, `getField`, `getValues`, and `getMapEntries`.
- Heap functions use SQL function syntax, for example `toString(s.this)`; do not use Java-like alias method syntax such as `s.toString(s.this)`.
- To find the most common String values, use `SELECT toString(s.this) AS unique_value, COUNT(*) AS count FROM "java.lang.String" s GROUP BY toString(s.this) ORDER BY COUNT(*) DESC`.

Examples:

```sql
SELECT DISTINCT OBJECTS classof(s)
FROM "java\\.lang\\.S.*" s
```

```sql
SELECT s AS String, s.value AS characters, inbounds(s).@length AS inbound_count
FROM java.lang.String s
WHERE s.@retainedHeapSize > 1048576
```

```sql
SELECT *
FROM OBJECTS (
  SELECT s, s.value AS value
  FROM java.lang.String s
) v
```

Native `JOIN`, `GROUP BY`, `COUNT`, `SUM`, `ORDER BY`, and `LIMIT/OFFSET` are supported by the embedded [MAT Calcite Plugin](https://github.com/vlsi/mat-calcite-plugin) through the same `heap_run_oql` endpoint. The service still applies its tool-level result limit and offset after query execution.
