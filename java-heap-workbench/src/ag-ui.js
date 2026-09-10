// Small AG-UI-compatible event bus. The UI can later swap this for an AG-UI
// transport without changing its A2UI rendering or MCP orchestration layers.
export const AG_EVENTS = Object.freeze({ RUN_STARTED: 'RUN_STARTED', TEXT_MESSAGE_CONTENT: 'TEXT_MESSAGE_CONTENT', TOOL_CALL_START: 'TOOL_CALL_START', TOOL_CALL_END: 'TOOL_CALL_END', STATE_SNAPSHOT: 'STATE_SNAPSHOT', RUN_FINISHED: 'RUN_FINISHED', RUN_ERROR: 'RUN_ERROR' });

export class AgUiRun {
  constructor(onEvent) { this.onEvent = onEvent; this.id = crypto.randomUUID(); }
  emit(type, data = {}) { this.onEvent({ type, runId: this.id, timestamp: Date.now(), ...data }); }
  start(input) { this.emit(AG_EVENTS.RUN_STARTED, { input }); }
  text(content) { this.emit(AG_EVENTS.TEXT_MESSAGE_CONTENT, { content }); }
  toolStart(name, args) { this.emit(AG_EVENTS.TOOL_CALL_START, { name, args }); }
  toolEnd(name, result) { this.emit(AG_EVENTS.TOOL_CALL_END, { name, result }); }
  state(snapshot) { this.emit(AG_EVENTS.STATE_SNAPSHOT, { snapshot }); }
  finish() { this.emit(AG_EVENTS.RUN_FINISHED); }
  error(message) { this.emit(AG_EVENTS.RUN_ERROR, { message }); }
}
