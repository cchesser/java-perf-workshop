# `java-heap-workbench` open-source dependencies

`java-heap-workbench` is a static browser application. Its [`package.json`](../package.json) has no runtime or development package dependencies, so there is no npm dependency tree to maintain.

## Browser and service boundaries

```mermaid
flowchart LR
    User["Heap analyst"] --> UI["Browser UI\nindex.html + styles.css"]
    UI --> Main["src/main.js\nworkspace state + orchestration"]
    Main --> MCP["java-heap-mcp\nHTTP API :7777"]
    MCP --> Heap["Eclipse MAT +\nApache Calcite analysis"]
    Heap --> Dump[("heap dump\n+ MAT indexes")]

    Main --> Provider{Model provider}
    Provider --> Ollama["Ollama\nlocal /api/chat"]
    Provider --> OpenRouter["OpenRouter\n/chat/completions"]
    Main --> Agent["src/agent-prompt.js\ntool definitions + agent rules"]
    Agent --> Provider
    Provider -->|tool calls| Main
    Main -->|callTool + handle| MCP

    Main --> Events["src/ag-ui.js\nAG-UI-compatible run events"]
    Main --> OQL["src/oql.js\nread-only Calcite SELECT guard"]
    Main --> A2UI["src/a2ui.js\nresponse parsing + rendering"]
    A2UI --> Visuals["summary cards, charts, tables,\nreference graphs, markdown"]
    Visuals --> UI

    Build["build.mjs"] --> Dist["dist/"]
    UI -. source files .-> Build
    Dist -. served locally on :4173 .-> UI

    classDef app fill:#1f6feb,color:#fff,stroke:#58a6ff;
    classDef service fill:#238636,color:#fff,stroke:#3fb950;
    classDef model fill:#9e6a03,color:#fff,stroke:#d29922;
    classDef data fill:#6e40c9,color:#fff,stroke:#bc8cff;
    class UI,Main,Agent,Events,OQL,A2UI,Build,Dist app;
    class MCP,Heap service;
    class Provider,Ollama,OpenRouter model;
    class Dump,Visuals data;
```

## Analysis run flow

```mermaid
sequenceDiagram
    actor User
    participant UI as main.js
    participant Model as Ollama or OpenRouter
    participant MCP as java-heap-mcp HTTP API
    participant Render as a2ui.js

    User->>UI: Ask about selected project
    UI->>MCP: GET /api/projects
    UI->>MCP: POST /api/open-project (when needed)
    UI->>Model: chat request with tool definitions
    Model-->>UI: tool call (overview, histogram, OQL, ...)
    UI->>UI: validate OQL when tool is heap_run_oql
    UI->>MCP: POST /api/{operation} with active handle
    MCP-->>UI: JSON analysis result
    UI->>Model: append tool result and continue run
    Model-->>UI: JSON message with components
    UI->>Render: parse, hydrate, and render A2UI components
    Render-->>User: cards, charts, tables, or object graph
```

## Project dependencies and integrations

| Dependency or platform API | Why it is used | Value to the project |
| --- | --- | --- |
| [Node.js](https://nodejs.org/) built-in `fs/promises`, `http`, `path`, and `url` modules | Runs [`build.mjs`](../build.mjs), copies source files into `dist/`, and serves the app during local development. | Keeps the build and development server small, dependency-free, and easy to run. |
| Browser Fetch API | Calls the `java-heap-mcp` HTTP endpoints and the configured model endpoint. | Provides the network layer for project selection, heap analysis, and agent requests without a frontend framework. |
| Browser Web Storage API (`localStorage`) | Stores provider, model, endpoint, and connection settings. | Preserves local workbench configuration between browser sessions. |
| Browser DOM, SVG, Canvas, and `crypto.randomUUID()` APIs | Builds the interface, renders charts/reference graphs, and identifies AG-UI-compatible runs. | Provides the UI, visual analysis output, and event tracking without a UI framework or chart library. |
| [`java-heap-mcp`](../../java-heap-mcp/) HTTP API | Supplies project handles, heap loading state, overviews, histograms, dominators, OQL, object inspection, GC-root paths, and leak suspects. | Keeps heap parsing and analysis in the Java service while the workbench remains a lightweight client. |
| [Ollama](https://ollama.com/) integration | Optionally sends chat/tool requests to a locally hosted model through `/api/chat`. | Supports private, local agent-assisted heap investigations. |
| [OpenRouter](https://openrouter.ai/) integration | Optionally sends OpenAI-compatible chat/tool requests to `/chat/completions`. | Allows the same workbench flow to use hosted models selected by the user. |

## Internal modules

The workbench’s reusable behavior is implemented in its own small ES modules rather than imported packages:

| Module | Why it is used | Value to the project |
| --- | --- | --- |
| [`src/main.js`](../src/main.js) | Coordinates UI state, model calls, MCP calls, and analysis runs. | Connects the user workflow end to end. |
| [`src/oql.js`](../src/oql.js) | Performs a lightweight read-only Calcite `SELECT` validation before requests leave the browser. | Prevents common invalid or mutating query requests. |
| [`src/ag-ui.js`](../src/ag-ui.js) | Emits AG-UI-compatible run lifecycle and tool events. | Gives the UI a transport-neutral progress/event model. |
| [`src/a2ui.js`](../src/a2ui.js) | Parses model responses and renders summaries, charts, tables, and graphs. | Turns tool results into visual reports without a component framework. |

Ollama and OpenRouter are service integrations rather than npm dependencies; the workbench only uses their HTTP APIs. MAT, Calcite, and the Java dependency set belong to `java-heap-mcp`, not to the browser bundle.
