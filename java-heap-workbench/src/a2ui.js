// A2UI is represented as declarative components. Agents return {components: []}
// and this renderer turns those components into safe DOM, charts included.
const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[c]));
const number = value => Number(value || 0).toLocaleString();
let graphMarkerSequence = 0;
const displayValue = value => {
  if (value === null || value === undefined) return '';
  if (typeof value === 'object') {
    try { return JSON.stringify(value); } catch { return String(value); }
  }
  return String(value);
};
const uniqueComponents = components => {
  const seen = new Set();
  return (components || []).filter(component => {
    const key = JSON.stringify(component);
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
};

export function renderMarkdown(value) {
  const lines = String(value ?? '').split('\n');
  let html = '', list = null;
  const inline = renderInlineMarkdown;
  const closeList = () => { if (list) { html += `</${list}>`; list = null; } };
  for (const rawLine of lines) {
    const line = inline(rawLine.trim());
    const heading = line.match(/^(#{1,6})\s+(.+)$/);
    const ordered = line.match(/^\d+[.)]\s+(.+)$/);
    const unordered = line.match(/^[-*+]\s+(.+)$/);
    if (!line) { closeList(); html += '<br>'; continue; }
    if (heading) { closeList(); const level = heading[1].length; html += `<h${level}>${heading[2]}</h${level}>`; continue; }
    if (ordered || unordered) { const nextList = ordered ? 'ol' : 'ul'; if (list !== nextList) { closeList(); list = nextList; html += `<${list}>`; } html += `<li>${(ordered || unordered)[1]}</li>`; continue; }
    closeList(); html += `<p>${line}</p>`;
  }
  closeList();
  return html;
}

export function renderInlineMarkdown(value) {
  const source = String(value ?? '').replace(/\\([\\`*_])/g, '$1');
  const escaped = esc(source);
  return escaped.replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/__([^_]+)__/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
    .replace(/_([^_]+)_/g, '<em>$1</em>');
}

export function normalizeAgentResponse(payload) {
  if (payload?.components) return { ...payload, components: uniqueComponents(payload.components) };
  return { components: [{ type: 'summary', title: 'Agent response', value: payload?.text || 'No visual response returned.', tone: 'neutral' }] };
}

const suggestedActions = [
  { label: 'Overall Memory Usage', prompt: 'Get a general overview of heap sizes and object counts.' },
  { label: 'Top Memory Consumers', prompt: 'Identify the top memory-consuming classes by retained size, shallow size, and object count.' },
  { label: 'Dominant Objects', prompt: 'Find the dominant objects or object types retaining the most heap memory.' },
  { label: 'Leak Detection', prompt: 'Run leak suspect analysis and summarize the evidence.' },
  { label: 'Deep Inspection', prompt: 'Perform a deep heap inspection using OQL and inspect relevant objects and paths to GC roots.' }
];

export function extractSuggestedActions(text, supplied = []) {
  const source = String(text || '');
  const actions = supplied.length ? supplied : suggestedActions.filter(action => new RegExp(action.label.replace(/\s+/g, '\\s+'), 'i').test(source));
  return actions.map(action => ({ label: action.label, prompt: action.prompt || action.action || action.query })).filter(action => action.label && action.prompt);
}

export function parseAgentContent(content) {
  const source = String(content || '').trim();
  const fenced = source.match(/```(?:json)?\s*([\s\S]*?)```/i);
  const objectStart = source.indexOf('{');
  const objectEnd = source.lastIndexOf('}');
  const candidate = fenced ? fenced[1].trim() : objectStart >= 0 && objectEnd > objectStart ? source.slice(objectStart, objectEnd + 1) : '';
  if (candidate.startsWith('{')) {
    try {
      const payload = JSON.parse(candidate);
      if (payload && typeof payload === 'object' && ('components' in payload || 'message' in payload || 'text' in payload)) {
        const text = payload.message || payload.text || '';
        return { view: { ...payload, components: Array.isArray(payload.components) ? payload.components : [] }, text, actions: extractSuggestedActions(text, payload.actions || []) };
      }
    } catch { /* Fall through to markdown/plain text rendering. */ }
  }
  if (fenced || candidate) return { view: { components: [] }, text: 'Analysis complete.', actions: [] };
  const lines = source.split('\n').map(line => line.trim()).filter(Boolean);
  const tableLines = lines.filter(line => line.startsWith('|') && line.endsWith('|'));
  if (tableLines.length >= 3) {
    const cells = line => line.slice(1, -1).split('|').map(value => value.trim());
    const columns = cells(tableLines[0]);
    const rows = tableLines.slice(2).map(line => cells(line)).filter(row => row.length === columns.length).map(row => Object.fromEntries(columns.map((column, index) => [column, row[index]])));
    if (rows.length) return { view: { components: [{ type: 'table', title: 'Agent results', columns, rows }] }, text: lines.filter(line => !tableLines.includes(line)).join('\n') };
  }
  return { view: { components: [{ type: 'markdown', text: source || 'Analysis complete.' }] }, text: source, actions: extractSuggestedActions(source) };
}

export function renderA2UI(target, payload) {
  if (target.dataset.busy === 'true') return;
  const view = normalizeAgentResponse(payload);
  target.innerHTML = (view.components || []).map(component => renderComponent(component)).join('');
  target.querySelectorAll('[data-chart]').forEach(node => drawChart(node, JSON.parse(node.dataset.chart)));
  target.querySelectorAll('[data-graph]').forEach(node => drawGraph(node, JSON.parse(node.dataset.graph)));
}

export function hydrateVisualData(view, fallback) {
  const primary = normalizeAgentResponse(view);
  const backup = normalizeAgentResponse(fallback);
  const fallbackTable = backup.components.find(component => component.type === 'table' && rowsFor(component).length);
  const fallbackChart = backup.components.find(component => component.type === 'chart' && chartDataFor(component).length);
  const fallbackGraph = backup.components.find(component => component.type === 'graph' && component.nodes?.length);
  const components = primary.components.map(component => {
    if (component.type === 'table' && fallbackTable) {
      const rows = rowsFor(component);
      const columnsMatchRows = component.columns?.length && rows.some(row => component.columns.some(column => Object.prototype.hasOwnProperty.call(row, column)));
      if (!rows.length || !columnsMatchRows) return { ...component, columns: fallbackTable.columns, rows: rowsFor(fallbackTable) };
    }
    if (component.type === 'chart' && !chartDataFor(component).length && fallbackChart) {
      return { ...component, data: chartDataFor(fallbackChart) };
    }
    if (component.type === 'graph' && !component.nodes?.length && fallbackGraph) return fallbackGraph;
    return component;
  });
  if (fallbackGraph && !components.some(component => component.type === 'graph')) components.push(fallbackGraph);
  return { ...primary, components: components.length ? components : backup.components };
}

const rowsFor = component => {
  const rows = component?.rows ?? component?.data ?? component?.entries ?? component?.results ?? [];
  return Array.isArray(rows) ? rows : Array.isArray(rows.rows) ? rows.rows : [];
};
const chartDataFor = component => {
  const data = component?.data ?? component?.points ?? component?.values ?? component?.rows ?? [];
  return Array.isArray(data) ? data : Array.isArray(data.rows) ? data.rows : [];
};

function renderComponent(c) {
  if (c.type === 'summary') return `<article class="summary-card ${esc(c.tone || 'neutral')}"><span>${esc(c.title)}</span><strong>${esc(c.value)}</strong><small>${esc(c.detail || '')}</small></article>`;
  if (c.type === 'markdown') return `<article class="message markdown">${renderMarkdown(c.text)}</article>`;
  if (c.type === 'table') {
    const rows = rowsFor(c);
    const cols = c.columns || Object.keys(rows[0] || {});
    const renderedRows = rows.map(row => {
      const objectId = row?.objectId;
      const actionable = objectId !== undefined && objectId !== null && objectId !== '';
      const attributes = actionable ? ` data-object-id="${esc(objectId)}" data-class-name="${esc(row.className || row.displayName || '')}" tabindex="0" role="button"` : '';
      return `<tr${attributes}>${cols.map(x => `<td>${renderInlineMarkdown(displayValue(row[x]))}</td>`).join('')}</tr>`;
    }).join('');
    return `<article class="panel a2ui-table"><header><h3>${esc(c.title || 'Results')}</h3><span>${number(rows.length)} rows</span></header><div class="scroll"><table><thead><tr>${cols.map(x => `<th>${esc(x)}</th>`).join('')}</tr></thead><tbody>${renderedRows}</tbody></table></div></article>`;
  }
  if (c.type === 'chart') return `<article class="panel chart-card"><header><h3>${esc(c.title || 'Chart')}</h3><span>${esc(c.subtitle || '')}</span></header><div class="chart" data-chart='${esc(JSON.stringify(c))}'></div></article>`;
  if (c.type === 'graph') return `<article class="panel graph-card"><header><h3>${esc(c.title || 'Object references')}</h3><span>${number((c.nodes || []).length)} objects</span></header><div class="graph" data-graph='${esc(JSON.stringify(c))}'></div></article>`;
  return '';
}

function drawChart(node, chart) {
  const points = chartDataFor(chart).map(x => ({ label: x.label ?? x.name ?? x.className ?? x.displayName ?? '', value: Number(x.value ?? x.retainedHeapBytes ?? x.retainedBytes ?? x.retainedHeap ?? x.shallowHeapBytes ?? x.count ?? x.objectCount ?? 0) })).slice(0, 12);
  if (!points.length) { node.innerHTML = '<div class="empty">No chart data</div>'; return; }
  const max = Math.max(...points.map(x => x.value), 1), width = 720, height = 230, pad = 38;
  if (chart.chartType === 'pie') {
    const total = points.reduce((a, x) => a + Math.max(0, x.value), 0);
    if (!total) { node.innerHTML = '<div class="empty">No positive chart values</div>'; return; }
    let angle = -Math.PI / 2;
    const colors = ['#58a6ff', '#3fb950', '#d29922', '#f85149', '#bc8cff', '#39c5cf'];
    const paths = points.map((p, i) => { const value = Math.max(0, p.value); const next = angle + value / total * Math.PI * 2; const large = next - angle > Math.PI ? 1 : 0; const arc = `${Math.cos(angle) * 72 + 120},${Math.sin(angle) * 72 + 120} ${Math.cos(next) * 72 + 120},${Math.sin(next) * 72 + 120}`; const d = `M120,120 L${Math.cos(angle) * 72 + 120},${Math.sin(angle) * 72 + 120} A72,72 0 ${large} 1 ${arc.split(' ')[1]} Z`; angle = next; return `<path d="${d}" fill="${colors[i % colors.length]}"/><text x="${260 + (i % 2) * 210}" y="${35 + Math.floor(i / 2) * 24}" fill="#c9d1d9" font-size="12">${esc(p.label)} · ${Math.round(value / total * 100)}%</text>`; }).join('');
    node.innerHTML = `<svg viewBox="0 0 520 250" role="img" aria-label="${esc(chart.title)}">${paths}</svg>`; return;
  }
  if (chart.chartType === 'line') {
    const pointsPath = points.map((p, i) => `${pad + i * ((width - pad * 2) / Math.max(points.length - 1, 1))},${height - pad - p.value / max * (height - pad * 2)}`).join(' ');
    const dots = points.map((p, i) => { const x = pad + i * ((width - pad * 2) / Math.max(points.length - 1, 1)), y = height - pad - p.value / max * (height - pad * 2); return `<circle cx="${x}" cy="${y}" r="4" fill="#58a6ff"><title>${esc(p.label)}: ${number(p.value)}</title></circle><text x="${x}" y="${height - 12}" text-anchor="middle" fill="#8b949e" font-size="10">${esc(p.label).slice(0, 12)}</text>`; }).join('');
    node.innerHTML = `<svg viewBox="0 0 ${width} ${height}" role="img" aria-label="${esc(chart.title)}"><line x1="${pad}" y1="${height - pad}" x2="${width - pad}" y2="${height - pad}" stroke="#30363d"/><polyline points="${pointsPath}" fill="none" stroke="#58a6ff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>${dots}</svg>`; return;
  }
  const rowHeight = 28, chartHeight = Math.max(230, points.length * rowHeight + 18), labelWidth = 210, plotWidth = width - labelWidth - 28;
  const bars = points.map((p, i) => { const y = 9 + i * rowHeight, barW = p.value / max * plotWidth, label = String(p.label).length > 28 ? `${String(p.label).slice(0, 27)}…` : p.label; return `<text x="8" y="${y + 14}" fill="#c9d1d9" font-size="11">${esc(label)}</text><rect x="${labelWidth}" y="${y + 2}" width="${Math.max(2, barW)}" height="18" rx="3" fill="#58a6ff"><title>${esc(p.label)}: ${number(p.value)}</title></rect><text x="${Math.min(labelWidth + barW + 7, width - 8)}" y="${y + 15}" fill="#8b949e" font-size="10">${number(p.value)}</text>`; }).join('');
  node.innerHTML = `<svg viewBox="0 0 ${width} ${chartHeight}" role="img" aria-label="${esc(chart.title)}">${bars}</svg>`;
}

function drawGraph(node, graph) {
  const sourceNodes = Array.isArray(graph.nodes) ? graph.nodes : [];
  const sourceEdges = Array.isArray(graph.edges) ? graph.edges : [];
  if (!sourceNodes.length) { node.innerHTML = '<div class="empty">No object references</div>'; return; }
  const nodes = sourceNodes.slice(0, 30).map((item, index) => ({ id: String(item.id ?? item.objectId ?? index), label: item.label ?? item.className ?? item.displayName ?? item.id ?? 'object', objectId: item.objectId ?? (Number.isFinite(Number(item.id)) ? item.id : null), level: item.level }));
  const nodeIds = new Set(nodes.map(item => item.id));
  const edges = sourceEdges.filter(edge => nodeIds.has(String(edge.source)) && nodeIds.has(String(edge.target))).slice(0, 60).map(edge => ({ source: String(edge.source), target: String(edge.target), label: edge.label || '' }));
  const root = nodes.find(item => item.level === 0) || nodes[0];
  const levels = new Map([[root.id, 0]]);
  for (let pass = 0; pass < nodes.length; pass += 1) edges.forEach(edge => { if (levels.has(edge.source) && !levels.has(edge.target)) levels.set(edge.target, Math.min(3, levels.get(edge.source) + 1)); });
  nodes.forEach(item => { if (!levels.has(item.id)) levels.set(item.id, Math.min(3, item.level ?? 1)); });
  const grouped = new Map(); nodes.forEach(item => { const level = levels.get(item.id); if (!grouped.has(level)) grouped.set(level, []); grouped.get(level).push(item); });
  const width = 900, columnWidth = 210, nodeWidth = 170, nodeHeight = 42, rowGap = 18;
  const height = Math.max(190, ...Array.from(grouped.values(), group => group.length * (nodeHeight + rowGap) + 30));
  const positions = new Map(); grouped.forEach((group, level) => { const total = group.length * (nodeHeight + rowGap) - rowGap; const start = Math.max(15, (height - total) / 2); group.forEach((item, index) => positions.set(item.id, { x: 18 + level * columnWidth, y: start + index * (nodeHeight + rowGap) })); });
  const markerId = `graph-arrow-${++graphMarkerSequence}`;
  const lines = edges.map(edge => { const from = positions.get(edge.source), to = positions.get(edge.target); if (!from || !to) return ''; const x1 = from.x + nodeWidth, y1 = from.y + nodeHeight / 2, x2 = to.x, y2 = to.y + nodeHeight / 2; return `<path d="M${x1},${y1} C${x1 + 35},${y1} ${x2 - 35},${y2} ${x2},${y2}" fill="none" stroke="#58a6ff" stroke-opacity=".65" marker-end="url(#${markerId})"><title>${esc(edge.label)}</title></path>`; }).join('');
  const boxes = nodes.map(item => { const position = positions.get(item.id), label = String(item.label).length > 24 ? `${String(item.label).slice(0, 23)}…` : item.label; const action = item.objectId !== null && item.objectId !== undefined ? ` data-object-id="${esc(item.objectId)}" data-class-name="${esc(item.label)}" tabindex="0" role="button"` : ''; return `<g class="graph-node" transform="translate(${position.x},${position.y})"${action}><rect width="${nodeWidth}" height="${nodeHeight}" rx="6" fill="${item.id === root.id ? '#1f6feb' : '#21262d'}" stroke="#58a6ff"/><text x="10" y="18" fill="#f0f6fc" font-size="11">${esc(label)}</text><text x="10" y="33" fill="#8b949e" font-size="10">#${esc(item.objectId ?? item.id)}</text></g>`; }).join('');
  node.innerHTML = `<svg viewBox="0 0 ${width} ${height}" role="img" aria-label="${esc(graph.title || 'Object reference graph')}"><defs><marker id="${markerId}" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8 Z" fill="#58a6ff"/></marker></defs>${lines}${boxes}</svg>`;
}
