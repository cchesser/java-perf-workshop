# java-heap-mcp

`java-heap-mcp` is a Java MCP server for loading and analyzing Java heap dumps with Eclipse Memory Analyzer (MAT) components. It exposes focused MCP tools for dump lifecycle, OQL/SQL, histograms, dominators, object inspection, GC-root paths, and leak-suspect analysis.

## Add the MCP server to Codex

The server speaks MCP over stdio, so it can be registered directly with the local Codex CLI. From the repository root, run:

```bash
codex mcp add javaHeapMcp -- /absolute/path/to/java-heap-mcp/scripts/run-server.sh
```

Use the real absolute path to this repository. Verify the registration with `codex mcp list`, then start a new Codex session if needed. Codex can use the heap tools through stdio; the workbench uses the same server's optional HTTP API instead.

The equivalent `~/.codex/config.toml` entry is:

```toml
[mcp_servers.javaHeapMcp]
command = "/absolute/path/to/java-heap-mcp/scripts/run-server.sh"
args = []
```

If the server is not executable, run `chmod +x scripts/run-server.sh` once. The server writes diagnostics to stderr and keeps protocol responses on stdout.

## Current Shape

- Java 25+ Maven project
- Long-lived multi-dump cache with stable dump handles
- Service-managed cache under `~/.cache/java-heap-mcp` by default
- Named project workflow (each project tracks its cached heap dump + active handle)
- MAT plugin jars downloaded from the official Eclipse distribution during the Maven build
- Headless Apache Calcite heap schema embedded from `mat-calcite-plugin`
- Official MCP Java SDK server with stdio transport and tool/schema negotiation
- Optional built-in web UI for local loading and analysis

## Engineering Governance

- Project principles and quality gates are defined in `.specify/memory/constitution.md`.
- Feature specs, plans, and tasks are expected to align with those constitution requirements.

## Tools

- `heap_load_dump`
- `heap_open_project`
- `heap_list_projects`
- `heap_list_dumps`
- `heap_unload_dump`
- `heap_get_overview`
- `heap_run_oql`
- `heap_get_oql_grammar` (no heap handle required; returns the supported Calcite SQL grammar, heap schema, functions, and examples)
- `heap_get_histogram`
- `heap_get_dominators`
- `heap_inspect_object`
- `heap_find_path_to_gc_roots`
- `heap_find_leak_suspects`

## Build

```bash
mvn -Dmaven.repo.local=.m2 test
```

## Run

```bash
./scripts/run-server.sh
```

Environment variables:

- `JAVA_HEAP_MCP_CACHE_DIR`
- `JAVA_HEAP_MCP_DEFAULT_LIMIT`
- `JAVA_HEAP_MCP_MAX_LIMIT`
- `JAVA_HEAP_MCP_TOOL_TIMEOUT_SECONDS`
- `JAVA_HEAP_MCP_GC_ROOT_PATH_LIMIT`
- `JAVA_HEAP_MCP_WEB_UI_ENABLED` (`true`/`false`, default `false`)
- `JAVA_HEAP_MCP_WEB_UI_PORT` (default `7777`)
- `JAVA_HEAP_MCP_WEB_UI_MAX_UPLOAD_BYTES` (default `0` = unlimited)

To enable the web UI:

```bash
JAVA_HEAP_MCP_WEB_UI_ENABLED=true JAVA_HEAP_MCP_WEB_UI_PORT=7777 ./scripts/run-server.sh
```

Then open: `http://127.0.0.1:7777/`

The HTTP API includes CORS headers and browser preflight support so the separate `java-heap-workbench` dev server can connect from `http://127.0.0.1:4173`.

If MAT reports `NoClassDefFoundError: Could not initialize class` for `QueryRegistry` or `Icons`, stop and restart the MCP server after any runtime/classpath change. These are JVM-level class-initialization failures and cannot be repaired by refreshing the browser.

`heap_run_oql` first executes queries through the embedded Calcite engine. This enables SQL joins, filters, grouping, ordering, lateral table functions, and MAT-aware functions such as `retainedSize`, `shallowSize`, `getField`, `getValues`, and `getMapEntries`. For example:

```sql
select toString(file) as file_name, count(*) as count
from java.net.URL
group by toString(file)
order by count(*) desc
```

Calcite uses double-quoted identifiers for fully qualified Java class names when needed (for example, `from "java.util.HashMap"`). Existing MAT OQL remains supported as a fallback. OQL diagnostics are written to the MCP process stderr. Each request logs the normalized single-line query plus its limit and offset; parse/runtime failures log the same query with the full exception.

Agents can call `heap_get_oql_grammar` before writing a query. The same read-only description is available from the HTTP API at `GET /api/oql-grammar` (also `GET /api/oql/grammar`). It describes the server's one-`SELECT` contract, the MAT Calcite heap schema, reference/table/collection functions, and working examples. The payload links to the [Apache Calcite SQL reference](https://calcite.apache.org/docs/reference.html) and [MAT Calcite plugin](https://github.com/vlsi/mat-calcite-plugin) for complete background.

## Project Workflow

1. Load a heap into a named project:
   - MCP: `heap_load_dump` with `{ "project": "my-service", "path": "/abs/path/dump.hprof" }`
   - Web UI: choose the `.hprof` file with the browser file picker and click **Load**
   - If no project is supplied, the service assigns a short Docker-style name such as `calm_tesla`.
2. Re-open an existing cached project:
   - MCP: `heap_open_project` with `{ "project": "my-service" }`
3. List projects and active handles:
   - MCP: `heap_list_projects`
4. Analyze with overview, dominators, leak suspects, histogram, and OQL using the returned handle.

## Notes

- MAT is downloaded and unpacked into `target/mat/` by Maven because the bundles are published through Eclipse update-site/RCP distribution artifacts rather than Maven Central.
- Web UI uploads are streamed directly to disk in fixed-size chunks before indexing/load (not buffered fully in memory).
- The current automated tests cover cache metadata, fingerprinting, and manager/tooling behavior. End-to-end MAT integration tests against real HPROF dumps are the next step.

## Licensing

- Eclipse Memory Analyzer (MAT) JARs are build outputs under `target/mat/` and are not stored in source control.
- Third-party licensing details are documented in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
- Included license texts are in `licenses/`:
  - `licenses/EPL-2.0.txt`
  - `licenses/MIT-DEFLATE.txt`
