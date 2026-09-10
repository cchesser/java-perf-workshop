#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CLASSPATH_FILE="$ROOT_DIR/target/runtime.classpath"

log() {
  printf '[java-heap-mcp] %s\n' "$*" >&2
}

WEB_UI_ENABLED="${JAVA_HEAP_MCP_WEB_UI_ENABLED:-false}"
WEB_UI_PORT="${JAVA_HEAP_MCP_WEB_UI_PORT:-7777}"

cd "$ROOT_DIR"

log "Preparing the runtime dependency classpath..."
mvn -q -Dmaven.repo.local=.m2 -DincludeScope=runtime -Dmdep.outputFile="$CLASSPATH_FILE" dependency:build-classpath

log "Compiling the server..."
mvn -q -Dmaven.repo.local=.m2 -DskipTests compile

CLASSPATH="target/classes:target/mat/*:$(cat "$CLASSPATH_FILE")"

log "Starting the MCP server (stdio transport)."
if [[ "$WEB_UI_ENABLED" =~ ^[Tt][Rr][Uu][Ee]$ ]]; then
  log "Web UI: http://127.0.0.1:${WEB_UI_PORT}/"
  log "HTTP API base: http://127.0.0.1:${WEB_UI_PORT}/api/"
  log "Available API routes: /projects, /dumps, /load, /load-upload, /open-project, /unload, /overview, /histogram, /dominators, /leaks, /oql"
else
  log "Web UI and HTTP APIs are disabled (set JAVA_HEAP_MCP_WEB_UI_ENABLED=true to enable them)."
fi
log "Server is launching 🚀..."
exec java -cp "$CLASSPATH" cchesser.javaperf.mcp.server.HeapMcpServerApplication
