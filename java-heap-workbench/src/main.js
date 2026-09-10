import { AgUiRun } from './ag-ui.js';
import { extractSuggestedActions, hydrateVisualData, parseAgentContent, renderMarkdown, renderA2UI } from './a2ui.js';
import { validateOql } from './oql.js';
import { AGENT_SYSTEM_PROMPT } from './agent-prompt.js';
import { planToolVisuals } from './visualization-policy.js';

const savedModel = localStorage.getItem('heap.model');

const defaultModel = 'qwen3.5:9b'; // 'nemotron-3-nano:4b';

const OPENROUTER_MODELS = [
    ['cohere/north-mini-code:free', 'Cohere North Mini Code (free)'],
    ['nvidia/nemotron-3.5-lightning:free', 'NVIDIA Nemotron 3.5 Lightning (free)'],
    ['qwen/qwen3-coder:free', 'Qwen3 Coder (free)'],
    ['openrouter/free', 'OpenRouter Free Models Router']
];
const state = { provider: localStorage.getItem('heap.provider') || 'ollama', mcp: localStorage.getItem('heap.mcp') || 'http://127.0.0.1:7777', ollama: localStorage.getItem('heap.ollama') || 'http://127.0.0.1:11434', openrouter: localStorage.getItem('heap.openrouter') || 'https://openrouter.ai/api/v1', openrouterKey: localStorage.getItem('heap.openrouterKey') || '', model: savedModel && savedModel !== 'qwen2.5:7b' ? savedModel : defaultModel, projects: [], project: null, handle: null, running: false, startedAt: 0, timer: null };
const $ = (selector, root = document) => root.querySelector(selector);
const app = $('#app');
app.innerHTML = `<div class="shell"><aside class="sidebar"><div class="brand"><img class="mark" src="/src/logo.svg" alt="" /> java-heap-workbench</div><nav class="nav"><a class="active" href="#workspace">Workspace</a><a href="#settings">Connections</a></nav><div class="connection"><div><i class="dot"></i><span id="mcp-status">checking MCP</span></div></div></aside><main class="main"><header class="topbar"><div><div class="eyebrow">HEAP EXPLORATION</div><h1>Analysis workspace</h1></div></header><section class="content"><div class="hero"><div><div class="eyebrow">PROJECT / <span id="project-name">no heap loaded</span></div><h2>See what your heap is holding.</h2><p>Ask your local agent to investigate memory behavior and turn findings into a visual report.</p></div><div class="controls"><select id="project-select" class="select"><option value="">Select project…</option></select><button id="refresh">Refresh</button></div></div><div id="metrics" class="grid"></div><div class="dashboard-grid"><div id="visuals" class="panel"><header><h3>Heap signals</h3></header><div class="empty">Load a heap dump in the MCP server, then select its project.</div></div><div class="panel"><header><h3>Quick investigations</h3><span>Ask the agent</span></header><div class="prompt-list"><button class="prompt" data-prompt="Give me a high-level overview of this heap and highlight the biggest retention risks.">What is retaining memory?</button><button class="prompt" data-prompt="Show me the top 10 classes by retained heap.">Largest retained classes</button><button class="prompt" data-prompt="Find leak suspects and summarize the evidence.">Find leak suspects</button><button class="prompt" data-prompt="Show me the top 20 string values.">Inspect String values</button></div></div></div><div class="chat"><div class="panel chat-panel"><div id="messages"><div class="message"><strong>Agent</strong><br>Ask a question about the selected heap to begin.</div></div><div id="run-progress" class="run-progress" hidden><span class="spinner"></span><span id="run-progress-label">Thinking</span><span id="run-elapsed">0s</span></div><div class="composer"><textarea id="prompt" placeholder="Ask about the selected heap…" rows="1"></textarea><button id="send">Run</button></div></div><div class="panel"><header><h3>Run details</h3><span id="run-state">idle</span></header><div id="event-log" class="muted">AG-UI events will appear here.</div><hr style="border-color:var(--line);border-width:1px 0 0;margin:18px 0"><div class="dropzone">Heap files are loaded by java-heap-mcp<br><small>Use its existing web UI or MCP client.</small></div></div></div><div id="settings" class="panel" hidden><header><h3>Model connections</h3><button id="close-settings" class="secondary">Close</button></header><div class="connection-form"><label>Provider<select id="provider" class="select"><option value="ollama">Ollama (local)</option><option value="openrouter">OpenRouter</option></select></label><label>MCP base URL<input id="mcp-url" class="select" value="${state.mcp}"></label><label id="ollama-setting">Ollama URL<input id="ollama-url" class="select" value="${state.ollama}"></label><label id="ollama-model-setting">Ollama model<input id="ollama-model" class="select" list="model-options" value="${state.model}"></label><label id="openrouter-setting">OpenRouter API URL<input id="openrouter-url" class="select" value="${state.openrouter}"></label><label id="key-setting">OpenRouter API key<input id="openrouter-key" class="select" type="password" value="${state.openrouterKey}" autocomplete="off"></label><label id="openrouter-model-setting">OpenRouter model<input id="openrouter-model" class="select" list="model-options" value="${state.model}"><datalist id="model-options"></datalist></label><label class="connection-submit"><span></span><button id="save-settings">Save connections</button></label></div></div></section></main></div>`;
const openrouterModel = $('#openrouter-model');
const modelControl = document.createElement('span');
modelControl.className = 'field-control';
const freeModelsLink = document.createElement('a');
freeModelsLink.href = 'https://openrouter.ai/models?variant=free&output_modalities=text&order=most-popular';
freeModelsLink.target = '_blank';
freeModelsLink.rel = 'noopener noreferrer';
freeModelsLink.textContent = 'Discover Free Models';
openrouterModel.parentNode.insertBefore(modelControl, openrouterModel);
modelControl.append(openrouterModel, freeModelsLink);
$('#provider').value = state.provider;
const updateProviderFields = () => { const openrouter = $('#provider').value === 'openrouter'; const modelField = openrouter ? $('#openrouter-model') : $('#ollama-model'); $('#ollama-setting').hidden = openrouter; $('#ollama-model-setting').hidden = openrouter; $('#openrouter-setting').hidden = !openrouter; $('#key-setting').hidden = !openrouter; $('#openrouter-model-setting').hidden = !openrouter; const models = openrouter ? OPENROUTER_MODELS : [[defaultModel, 'Nemotron 3 Nano (local)']]; $('#model-options').innerHTML = models.map(([value, label]) => `<option value="${value}">${label}</option>`).join(''); if (!models.some(([value]) => value === modelField.value)) modelField.value = models[0][0]; };
updateProviderFields();
const visualPanel = $('#visuals');
visualPanel.innerHTML = '<header><h3>Visual analysis</h3></header><div id="visual-output" class="visual-output"><div id="visual-live"><div class="empty">Ask the Agent to investigate the selected heap. Charts, tables, and summary cards will appear here.</div></div><div id="visual-history" aria-live="polite"></div></div>';
const quickPanel = document.querySelector('.dashboard-grid > .panel:nth-child(2)');
quickPanel.classList.add('chat-prompts');
document.querySelector('.chat-panel').insertBefore(quickPanel, $('#messages'));

const api = async (path, method = 'GET', body) => { const response = await fetch(`${state.mcp}${path}`, { method, headers: body ? { 'Content-Type': 'application/json' } : {}, body: body && JSON.stringify(body) }); const data = await response.json(); if (!response.ok || data.error) throw new Error([data.error || `MCP request failed (${response.status})`, data.errorCode, data.cause].filter(Boolean).join(' — ')); return data; };
const visualState = { nextId: 0, entries: new Map(), observer: null };
const rowsFromResult = result => {
    if (Array.isArray(result)) return result;
    const candidates = [result, result?.structuredContent, result?.result];
    for (const candidate of candidates) {
        for (const key of ['rows', 'entries', 'results', 'data']) {
            if (Array.isArray(candidate?.[key])) return candidate[key];
        }
    }
    return [];
};
const resultValue = result => result?.structuredContent || result?.result || result || {};
const columnsFromResult = (result, rows) => {
    const value = resultValue(result);
    return Array.isArray(value.columns) && value.columns.length ? value.columns : Object.keys(rows[0] || {});
};
const showVisualLoading = () => { $('#visual-output').dataset.busy = 'true'; $('#visual-live').innerHTML = ''; $('#visual-live').hidden = false; };
const previousVisualId = node => { let previous = node.previousElementSibling; while (previous) { if (previous.dataset.visualId) return previous.dataset.visualId; previous = previous.previousElementSibling; } return null; };
const activateVisual = id => { if (!id || !visualState.entries.has(id)) return; visualState.entries.forEach((entry, entryId) => entry.classList.toggle('active', entryId === id)); document.querySelectorAll('.message[data-visual-id], .message[data-visual-target]').forEach(node => node.classList.toggle('visual-selected', node.dataset.visualId === id || node.dataset.visualTarget === id)); };
const bindResponse = node => { node.tabIndex = 0; node.setAttribute('role', 'button'); const select = () => activateVisual(node.dataset.visualId || previousVisualId(node)); node.onclick = select; node.onkeydown = event => { if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); select(); } }; };
const runSuggestedAction = prompt => runAgent(prompt).catch(error => { removeThinking(); addMessage(`Error: ${error.message}`); log(error.message); endRun('Failed', 'error'); });
const addMessage = (text, kind = '', actions = []) => { const node = document.createElement('div'); node.className = `message ${kind}`; node.innerHTML = `<strong>${kind === 'user' ? 'you' : 'Agent'}</strong>${renderMarkdown(text)}`; if (kind !== 'user' && actions.length) { const actionBar = document.createElement('div'); actionBar.className = 'response-actions'; actions.forEach(action => { const button = document.createElement('button'); button.className = 'prompt'; button.type = 'button'; button.textContent = action.label; button.onclick = () => { if (!state.running) runSuggestedAction(action.prompt); }; actionBar.append(button); }); node.append(actionBar); } $('#messages').append(node); if (kind !== 'user') bindResponse(node); node.scrollIntoView({ block: 'nearest' }); return node; };
const commitVisual = responseNode => { const live = $('#visual-live'); if (!live || !live.innerHTML.trim()) return; const id = `visual-${++visualState.nextId}`; const entry = document.createElement('section'); entry.className = 'visual-entry'; entry.dataset.visualId = id; entry.innerHTML = live.innerHTML; $('#visual-history').append(entry); visualState.entries.set(id, entry); responseNode.dataset.visualId = id; responseNode.classList.add('has-visual'); live.hidden = true; activateVisual(id); };
const log = text => { $('#event-log').textContent = text; };
const setRunProgress = (label, mode = 'working') => { $('#run-progress').hidden = false; $('#run-progress').dataset.mode = mode; $('#run-progress-label').textContent = label; $('#run-state').textContent = label.toLowerCase(); };
const updateThinking = label => { const status = $('#thinking-status'); if (status) status.textContent = label; };
const beginRun = () => { state.running = true; state.startedAt = Date.now(); $('#send').disabled = true; $('#send').textContent = 'Working…'; $('#prompt').disabled = true; document.querySelectorAll('.prompt').forEach(button => { button.disabled = true; }); showVisualLoading(); setRunProgress('Thinking…'); state.timer = setInterval(() => { $('#run-elapsed').textContent = `${Math.floor((Date.now() - state.startedAt) / 1000)}s`; }, 250); };
const endRun = (label, mode = 'complete') => { state.running = false; clearInterval(state.timer); state.timer = null; $('#send').disabled = false; $('#send').textContent = 'Run'; $('#prompt').disabled = false; document.querySelectorAll('.prompt').forEach(button => { button.disabled = false; }); setRunProgress(label, mode); setTimeout(() => { if (!state.running) $('#run-progress').hidden = true; }, 1800); };
const removeThinking = () => { const node = $('#thinking-message'); if (node) node.remove(); $('#visual-output').dataset.busy = 'false'; };
const addThinking = () => { removeThinking(); const node = document.createElement('div'); node.id = 'thinking-message'; node.className = 'message thinking'; node.innerHTML = '<strong>Agent</strong><br><span class="thinking-copy"><span id="thinking-status">Planning the investigation…</span><span class="thinking-dots"><i></i><i></i><i></i></span></span>'; $('#messages').append(node); node.scrollIntoView({ block: 'nearest' }); };
visualState.observer = new IntersectionObserver(entries => {
    const visible = entries.filter(entry => entry.isIntersecting && entry.target.dataset.visualId)
        .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];
    if (visible && !state.running) activateVisual(visible.target.dataset.visualId);
});

async function refreshProjects() { try { state.projects = await api('/api/projects'); const select = $('#project-select'); select.innerHTML = '<option value="">Select project…</option>'; state.projects.forEach(p => { const option = new Option(`${p.project}${p.activeHandle ? '' : ' (cached)'}`, p.project); select.add(option); }); const active = state.projects.find(p => p.activeHandle); if (active) { select.value = active.project; selectProject(active.project); } $('#mcp-status').textContent = 'MCP connected'; } catch (error) { $('#mcp-status').textContent = 'MCP offline'; log(error.message); } }
async function selectProject(name) { const project = state.projects.find(p => p.project === name); if (!project) return; try { const opened = project.activeHandle ? project : await api('/api/open-project', 'POST', { project: name }); state.project = opened.project; state.handle = opened.handle || opened.activeHandle; $('#project-name').textContent = state.project; await loadOverview(); } catch (error) { log(error.message); } }
async function loadOverview() { const overview = await api('/api/overview', 'POST', { handle: state.handle, limit: 12 }); const values = overview?.snapshot || {}; $('#metrics').innerHTML = [['used heap', values.usedHeapSize, 'blue'], ['objects', values.numberOfObjects, 'green'], ['classes', values.numberOfClasses, 'yellow'], ['class loaders', values.numberOfClassLoaders, 'red']].map(([label, value, tone]) => `<article class="summary-card ${tone}"><span>${label}</span><strong>${Number(value || 0).toLocaleString()}</strong><small>from current MAT snapshot</small></article>`).join(''); }

const toolDefinitions = [
    { type: 'function', function: { name: 'heap_get_overview', description: 'Get heap size, object, class, and GC overview.', parameters: { type: 'object', properties: {} } } },
    { type: 'function', function: { name: 'heap_get_oql_grammar', description: 'Get the supported Apache Calcite SQL grammar, heap schema, functions, and examples before constructing an OQL query.', parameters: { type: 'object', properties: {} } } },
    { type: 'function', function: { name: 'heap_get_histogram', description: 'Get classes ranked by heap usage.', parameters: { type: 'object', properties: { sort: { type: 'string' }, limit: { type: 'integer' } } } } },
    { type: 'function', function: { name: 'heap_get_dominators', description: 'Get objects/classes ranked by retained heap.', parameters: { type: 'object', properties: { limit: { type: 'integer' } } } } },
    { type: 'function', function: { name: 'heap_find_leak_suspects', description: 'Find suspected memory leaks.', parameters: { type: 'object', properties: { limit: { type: 'integer' } } } } },
    { type: 'function', function: { name: 'heap_run_oql', description: 'Run only a read-only Apache Calcite SQL SELECT statement (the OQL language) against the selected heap. Use SQL joins, grouping, aggregates, ordering, and pagination.', parameters: { type: 'object', required: ['query'], properties: { query: { type: 'string' }, limit: { type: 'integer' } } } } },
    { type: 'function', function: { name: 'heap_inspect_object', description: 'Inspect a heap object returned by OQL, including its class, fields, sizes, and references.', parameters: { type: 'object', required: ['objectId'], properties: { objectId: { type: 'integer' }, limit: { type: 'integer' } } } } },
    { type: 'function', function: { name: 'heap_find_path_to_gc_roots', description: 'Find reference paths from an object to GC roots.', parameters: { type: 'object', required: ['objectId'], properties: { objectId: { type: 'integer' }, excludeWeakRefs: { type: 'boolean' }, limit: { type: 'integer' } } } } }
];
async function callTool(name, args) { args = typeof args === 'string' ? JSON.parse(args) : (args || {}); if (name === 'heap_get_oql_grammar') return api('/api/oql-grammar'); if (name === 'heap_run_oql') { console.info('[java-heap-workbench] Generated heap query:', args.query); const validationError = validateOql(args.query); if (validationError) return { error: 'OQL_VALIDATION_ERROR', message: validationError, query: args.query, correction: 'Use one read-only SELECT query. Prefer Apache Calcite SQL for JOIN/GROUP BY/HAVING/COUNT/SUM/ORDER BY/LIMIT/OFFSET and use MAT OQL for MAT-native expressions; do not add a semicolon.' }; } const routes = { heap_get_overview: ['/api/overview', {}], heap_get_histogram: ['/api/histogram', { sort: args.sort || 'retained', limit: args.limit || 10 }], heap_get_dominators: ['/api/dominators', { limit: args.limit || 10 }], heap_find_leak_suspects: ['/api/leaks', { limit: args.limit || 10 }], heap_run_oql: ['/api/oql', { query: args.query, limit: args.limit || 25, offset: 0 }], heap_inspect_object: ['/api/inspect', { objectId: args.objectId, limit: args.limit || 25 }], heap_find_path_to_gc_roots: ['/api/gc-roots', { objectId: args.objectId, excludeWeakRefs: args.excludeWeakRefs !== false, limit: args.limit || 25 }] }; const route = routes[name]; if (!route) throw new Error(`Unsupported tool: ${name}`); const [path, body] = route; return api(path, 'POST', { handle: state.handle, ...body }); }
function visualFromTool(name, result) { if (name === 'heap_get_overview') { const snapshot = result.snapshot || {}, classes = result.topClasses || [], dominators = result.topDominators || []; return { components: [{ type: 'summary', title: 'Used heap', value: Number(snapshot.usedHeapSize || 0).toLocaleString(), detail: `${Number(snapshot.numberOfObjects || 0).toLocaleString()} objects`, tone: 'blue' }, { type: 'summary', title: 'Classes', value: Number(snapshot.numberOfClasses || 0).toLocaleString(), detail: `${Number(snapshot.numberOfClassLoaders || 0).toLocaleString()} class loaders`, tone: 'yellow' }, { type: 'chart', title: 'Top retained classes', chartType: 'bar', data: classes.map(x => ({ label: x.className, value: x.retainedHeapBytes || x.shallowHeapBytes || 0 })) }, { type: 'table', title: 'Top retained contributors', columns: ['className', 'objectCount', 'retainedHeapBytes'], rows: classes }, { type: 'table', title: 'Top dominators', columns: ['className', 'displayName', 'retainedHeapBytes'], rows: dominators }] }; } if (name === 'heap_get_histogram' || name === 'heap_get_dominators') { const rows = rowsFromResult(result); return { components: [{ type: 'chart', title: name === 'heap_get_histogram' ? 'Retained heap by class' : 'Top dominators', chartType: 'bar', data: rows.map(x => ({ label: x.className || x.name || x.displayName || 'object', value: x.retainedHeapBytes || x.retainedHeap || x.retainedBytes || x.usedHeap || x.shallowHeapBytes || x.shallowHeap || x.objectCount || x.count || 0 })) }, { type: 'table', title: 'Analysis rows', rows }] }; } if (name === 'heap_find_leak_suspects') { const rows = Array.isArray(result) ? result : result.suspects || result.rows || []; return { components: [{ type: 'summary', title: 'Leak suspects', value: rows.length, detail: 'candidates returned by MAT', tone: 'red' }, { type: 'table', title: 'Suspect evidence', rows }] }; } if (name === 'heap_inspect_object') { const fields = Array.isArray(result.fields) ? result.fields : result.fields?.rows || []; const references = Array.isArray(result.outboundReferences) ? result.outboundReferences : []; return { components: [{ type: 'summary', title: 'Inspected object', value: result.className || result.displayName || `#${result.objectId}`, detail: `${Number(result.retainedHeapBytes || 0).toLocaleString()} retained bytes`, tone: 'blue' }, { type: 'table', title: 'Attributes and fields', rows: fields }, { type: 'table', title: 'Outbound references', rows: references }] }; } if (name === 'heap_find_path_to_gc_roots') { const rows = Array.isArray(result) ? result : result.paths || result.rows || []; return { components: [{ type: 'summary', title: 'Paths to GC roots', value: rows.length, detail: 'reference paths returned by MAT', tone: 'yellow' }, { type: 'table', title: 'GC root paths', rows }] }; } return { components: [{ type: 'summary', title: name === 'heap_run_oql' ? 'OQL result' : 'Analysis result', value: rowsFromResult(result).length || 'ready', detail: 'tool result returned', tone: 'blue' }, { type: 'table', title: 'Results', rows: rowsFromResult(result) }] }; }

function referenceGraph(result) {
    const rootId = result?.objectId;
    const references = Array.isArray(result?.outboundReferences) ? result.outboundReferences : [];
    const nodes = [{ id: String(rootId), objectId: rootId, label: result.className || result.displayName || `#${rootId}`, level: 0 }];
    const edges = [];
    references.slice(0, 24).forEach(reference => {
        if (reference?.objectId === undefined || reference?.objectId === null) return;
        const id = String(reference.objectId);
        if (!nodes.some(node => node.id === id)) nodes.push({ id, objectId: reference.objectId, label: reference.className || reference.displayName || `#${id}`, level: 1 });
        edges.push({ source: String(rootId), target: id, label: reference.displayName || reference.className || 'reference' });
    });
    return { type: 'graph', title: 'Outbound object references', nodes, edges };
}

function appendToolVisual(name, result, toolVisuals) {
    const toolView = visualFromTool(name, result);
    if (name === 'heap_inspect_object') toolView.components.push(referenceGraph(result));
    toolVisuals.components.push(...toolView.components);
}

async function runAgent(question) {
    if (!state.handle) throw new Error('Select or open a loaded heap project first.');
    beginRun();
    addMessage(question, 'user');
    addThinking();
    const run = new AgUiRun(event => {
        if (event.type === 'RUN_STARTED') setRunProgress('Thinking…');
        if (event.type === 'TOOL_CALL_START') setRunProgress(`Calling ${event.name}…`);
        if (event.type === 'TOOL_CALL_END') setRunProgress('Reading tool result…');
        if (event.type === 'TEXT_MESSAGE_CONTENT') setRunProgress('Writing visual response…');
    });
    run.start({ question });
    const messages = [{ role: 'system', content: AGENT_SYSTEM_PROMPT }, { role: 'user', content: question }];
    let lastTool = null;
    let oqlFailed = false;
    let oqlSucceeded = false;
    const toolVisuals = { components: [] };
    const maxTurns = 4;
    const graphRequest = /\bgraph\b/i.test(question) && /directed|object[- ]reference|outbound/i.test(question);
    if (graphRequest) {
        updateThinking('Selecting the largest dominator for the object graph…');
        try {
            const dominatorArgs = { limit: 10 };
            run.toolStart('heap_get_dominators', dominatorArgs);
            const dominatorResult = await callTool('heap_get_dominators', dominatorArgs);
            run.toolEnd('heap_get_dominators', dominatorResult);
            if (dominatorResult?.error) throw new Error(dominatorResult.message || dominatorResult.error);
            appendToolVisual('heap_get_dominators', dominatorResult, toolVisuals);
            renderA2UI($('#visual-live'), toolVisuals);
            const dominatorRows = Array.isArray(dominatorResult) ? dominatorResult : dominatorResult.entries || dominatorResult.rows || [];
            const root = dominatorRows.filter(row => row?.objectId !== undefined && row?.objectId !== null)
                .sort((left, right) => Number(right.retainedHeapBytes || right.retainedBytes || 0) - Number(left.retainedHeapBytes || left.retainedBytes || 0))[0];
            if (root) {
                updateThinking(`Inspecting dominator object ${root.objectId} for outbound references…`);
                const inspectArgs = { objectId: root.objectId, limit: 25 };
                run.toolStart('heap_inspect_object', inspectArgs);
                const inspectResult = await callTool('heap_inspect_object', inspectArgs);
                run.toolEnd('heap_inspect_object', inspectResult);
                if (inspectResult?.error) throw new Error(inspectResult.message || inspectResult.error);
                appendToolVisual('heap_inspect_object', inspectResult, toolVisuals);
                renderA2UI($('#visual-live'), toolVisuals);
                messages.push({ role: 'system', content: `The workbench preloaded the directed graph workflow. Use this evidence to summarize the graph and return the exact A2UI graph shape with top-level nodes and edges. Dominator: ${JSON.stringify(root)}. Inspection: ${JSON.stringify(inspectResult)}` });
                updateThinking('Directed graph is visible; asking the agent to summarize the evidence…');
            }
        } catch (error) {
            updateThinking(`Graph preflight was unavailable; asking the agent to continue (${error.message})…`);
        }
    }
    for (let turn = 0; turn < maxTurns; turn++) {
        const turnStatus = turn === 0 ? 'Planning the investigation…' : `Planning the next step (${turn + 1}/${maxTurns})…`;
        setRunProgress(turn === 0 ? 'Thinking…' : 'Planning next step…');
        updateThinking(turnStatus);
        const openrouter = state.provider === 'openrouter';
        const endpoint = openrouter ? `${state.openrouter.replace(/\/$/, '')}/chat/completions` : `${state.ollama}/api/chat`;
        const headers = { 'Content-Type': 'application/json' };
        if (openrouter) { if (!state.openrouterKey) throw new Error('OpenRouter API key is required. Open **Connections** to add it.'); headers.Authorization = `Bearer ${state.openrouterKey}`; headers['X-Title'] = 'java-heap-workbench'; }
        updateThinking('Asking the agent to choose the next evidence step');
        const response = await fetch(endpoint, { method: 'POST', headers, body: JSON.stringify({ model: state.model, stream: false, messages, tools: toolDefinitions }) });
        if (!response.ok) throw new Error(`${openrouter ? 'OpenRouter' : 'Ollama'} request failed (${response.status})`);
        const data = await response.json();
        const message = openrouter ? (data.choices?.[0]?.message || {}) : (data.message || {});
        messages.push(message);
        const calls = message.tool_calls || [];
        if (!calls.length) {
            if (oqlFailed && !oqlSucceeded) {
                const clarification = 'I’m having trouble correctly determining the data to query. What specific class, field, relationship, or filter should I investigate?';
                run.text(clarification);
                removeThinking();
                addMessage(clarification);
                run.finish();
                endRun('Needs clarification');
                return;
            }
            updateThinking('Preparing the final visual summary');
            const parsed = parseAgentContent(message.content || 'Analysis complete.');
            run.text(parsed.text || 'Visual response ready.');
            removeThinking();
            const hydratedView = hydrateVisualData(parsed.view, toolVisuals);
            const visualComponents = hydratedView.components.filter(component => component.type !== 'markdown');
            const accumulatedComponents = [...toolVisuals.components, ...visualComponents]
                .filter((component, index, components) => components.findIndex(item => JSON.stringify(item) === JSON.stringify(component)) === index);
            const finalView = accumulatedComponents.length ? { ...hydratedView, components: accumulatedComponents } : toolVisuals;
            if (finalView.components.length) renderA2UI($('#visual-live'), finalView);
            const responseNode = addMessage(parsed.text || 'Visual response ready.', '', parsed.actions || extractSuggestedActions(parsed.text));
            if (visualComponents.length) {
                commitVisual(responseNode);
                visualState.observer.observe(responseNode);
            }
            run.finish();
            endRun('Complete');
            return;
        }
        for (const call of calls) {
            const name = call.function.name;
            const args = call.function.arguments || {};
            run.toolStart(name, args);
            lastTool = name;
            const toolLabel = name.replace(/^heap_/, '').replaceAll('_', ' ');
            updateThinking(`Calling ${toolLabel}…`);
            let result;
            try {
                result = await callTool(name, args);
            } catch (error) {
                result = { error: error.message, errorCode: error.code || 'TOOL_ERROR' };
            }
            run.toolEnd(name, result);
            updateThinking(result?.error ? `${toolLabel} returned an error; evaluating the next step…` : `${toolLabel} returned evidence; updating the visual…`);
            messages.push({ role: 'tool', content: JSON.stringify(result), ...(openrouter && call.id ? { tool_call_id: call.id } : {}) });
            if (result?.error) {
                if (name === 'heap_run_oql') oqlFailed = true;
                const retryAvailable = name === 'heap_run_oql' && turn < maxTurns - 1;
                if (retryAvailable) {
                    updateThinking(`The OQL attempt failed; retrying with simpler Calcite SQL (${turn + 2}/${maxTurns})…`);
                    addMessage(`The OQL attempt failed. I’m re-evaluating it and will retry with simpler Calcite SQL (attempt ${turn + 2} of ${maxTurns}).`, '');
                }
                continue;
            }
            if (name === 'heap_run_oql') oqlSucceeded = true;
            const toolView = visualFromTool(name, result);
            if (name === 'heap_run_oql') {
                const rows = rowsFromResult(result);
                const value = resultValue(result);
                const components = [{ type: 'table', title: 'OQL results', columns: columnsFromResult(result, rows), rows }];
                if (!rows.length) {
                    const totalRows = Number(value.totalRows);
                    components.push({ type: 'markdown', text: Number.isFinite(totalRows) && totalRows === 0
                        ? 'The OQL query completed successfully but returned no rows.'
                        : 'The OQL query completed, but the returned result did not contain tabular rows.' });
                }
                toolView.components = components;
            }
            if (name === 'heap_inspect_object') toolView.components.push(referenceGraph(result));
            toolView.components.push(...planToolVisuals(question, name, args, result));
            toolVisuals.components.push(...toolView.components);
            renderA2UI($('#visual-live'), toolVisuals);
            updateThinking('Visual updated; continuing the analysis…');
        }
    }
    removeThinking();
    if (oqlFailed && !oqlSucceeded) {
        addMessage('I’m having trouble correctly determining the data to query. What specific class, field, relationship, or filter should I investigate?');
    } else if (lastTool) addMessage('The analysis tools returned results. See the visual report above.');
    run.finish();
    endRun('Complete');
}

const inspectObjectRow = row => {
    if (!row || state.running) return;
    const objectId = row.dataset.objectId;
    const className = row.dataset.className || 'the selected object';
    runSuggestedAction(`Inspect object ${objectId} (${className}) and explain its fields, outbound references, retained memory, and path to GC roots.`);
};
$('#visual-output').addEventListener('click', event => inspectObjectRow(event.target.closest('tr[data-object-id], g.graph-node[data-object-id]')));
$('#visual-output').addEventListener('keydown', event => {
    if (event.key === 'Enter' || event.key === ' ') inspectObjectRow(event.target.closest('tr[data-object-id], g.graph-node[data-object-id]'));
});

const content = $('.content');
const navLinks = [...document.querySelectorAll('.nav a')];
const showView = view => {
    const connections = view === 'connections';
    content.classList.toggle('connections-view', connections);
    $('#settings').hidden = !connections;
    navLinks.forEach(link => link.classList.toggle('active', connections ? link.hash === '#settings' : link.hash !== '#settings'));
};

$('#refresh').onclick = refreshProjects; $('#project-select').onchange = e => selectProject(e.target.value); $('#send').onclick = () => { if (state.running) return; const prompt = $('#prompt').value.trim(); if (!prompt) return; $('#prompt').value = ''; runSuggestedAction(prompt); }; $('#prompt').onkeydown = e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); $('#send').click(); } }; document.querySelectorAll('.prompt').forEach(button => button.onclick = () => { if (!state.running) runSuggestedAction(button.dataset.prompt); }); navLinks.forEach(link => link.onclick = event => { event.preventDefault(); showView(link.hash === '#settings' ? 'connections' : 'workspace'); }); $('#close-settings').onclick = () => showView('workspace'); $('#provider').onchange = updateProviderFields; $('#save-settings').onclick = () => { state.provider = $('#provider').value; state.mcp = $('#mcp-url').value.replace(/\/$/, ''); state.ollama = $('#ollama-url').value.replace(/\/$/, ''); state.openrouter = $('#openrouter-url').value.replace(/\/$/, ''); state.openrouterKey = $('#openrouter-key').value.trim(); state.model = (state.provider === 'openrouter' ? $('#openrouter-model') : $('#ollama-model')).value.trim(); localStorage.setItem('heap.provider', state.provider); localStorage.setItem('heap.mcp', state.mcp); localStorage.setItem('heap.ollama', state.ollama); localStorage.setItem('heap.openrouter', state.openrouter); localStorage.setItem('heap.openrouterKey', state.openrouterKey); localStorage.setItem('heap.model', state.model); showView('workspace'); refreshProjects(); }; refreshProjects();
