import test from 'node:test';
import assert from 'node:assert/strict';
import { stat } from 'node:fs/promises';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { extractSuggestedActions, hydrateVisualData, normalizeAgentResponse, parseAgentContent, renderInlineMarkdown } from '../src/a2ui.js';
import { AGENT_SYSTEM_PROMPT } from '../src/agent-prompt.js';
import { OQL_AGENT_RULES, validateOql } from '../src/oql.js';
import { planToolVisuals } from '../src/visualization-policy.js';

test('workbench build contains the app entry points', async () => {
  const root = fileURLToPath(new URL('..', import.meta.url));
  await import(join(root, 'build.mjs'));
  await stat(join(root, 'dist/index.html'));
  await stat(join(root, 'dist/src/main.js'));
  await stat(join(root, 'dist/src/a2ui.js'));
});

test('agent responses become visual components', () => {
  const json = parseAgentContent('{"message":"Found the largest classes.","components":[{"type":"chart","chartType":"line","data":[{"label":"A","value":2}]}]}');
  assert.equal(json.view.components[0].type, 'chart');
  assert.equal(json.view.components[0].chartType, 'line');
  const messageOnly = parseAgentContent('{"message":"The table is shown in Visual Analysis.","components":[]}');
  assert.equal(messageOnly.text, 'The table is shown in Visual Analysis.');
  assert.deepEqual(messageOnly.view.components, []);

  const markdown = parseAgentContent('| Class | Retained |\n| --- | --- |\n| java.lang.String | 42 |');
  assert.equal(markdown.view.components[0].type, 'table');
  assert.equal(markdown.view.components[0].rows[0].Class, 'java.lang.String');
});

test('agent responses preserve pie charts and directed object graphs', () => {
  const response = normalizeAgentResponse({ components: [
    { type: 'chart', title: 'String frequency', chartType: 'pie', data: [{ label: 'alpha', value: 8 }] },
    { type: 'graph', title: 'Outbound references', nodes: [{ id: '1', objectId: 1 }, { id: '2', objectId: 2 }], edges: [{ source: '1', target: '2', label: 'value' }] }
  ] });
  assert.equal(response.components[0].chartType, 'pie');
  assert.deepEqual(response.components[1].edges[0], { source: '1', target: '2', label: 'value' });
});

test('visualization policy plans a pie chart for String frequency results', () => {
  const planned = planToolVisuals('Show the top 5 String values as a pie chart.', 'heap_run_oql', {}, {
    columns: ['val', 'cnt'],
    rows: [{ val: 'alpha', cnt: 5 }, { val: 'beta', cnt: 3 }]
  });
  assert.equal(planned[0].type, 'chart');
  assert.equal(planned[0].chartType, 'pie');
  assert.deepEqual(planned[0].data, [{ label: 'alpha', value: 5 }, { label: 'beta', value: 3 }]);
});

test('visualization policy accepts wrapped OQL result rows', () => {
  const planned = planToolVisuals('Show common strings as a pie chart.', 'heap_run_oql', {}, {
    structuredContent: { rows: [{ val: 'alpha', cnt: 2 }] }
  });
  assert.equal(planned[0].chartType, 'pie');
});

test('table cell markdown is rendered as inline markup', () => {
  const rendered = renderInlineMarkdown('**1.4KB** \\*\\*`Com.escaper.Escape`\\*\\*');
  assert.match(rendered, /<strong>1\.4KB<\/strong>/);
  assert.match(rendered, /<code>Com\.escaper\.Escape<\/code>/);
  assert.doesNotMatch(rendered, /\\\*\\\*/);
});

test('empty agent tables are hydrated from tool data', () => {
  const hydrated = hydrateVisualData(
    { components: [{ type: 'table', title: 'Top 10 Classes Ranked by Retained Heap Bytes', rows: [] }] },
    { components: [{ type: 'table', rows: [{ className: 'java.lang.String', retainedHeapBytes: 123 }] }] }
  );
  assert.equal(hydrated.components[0].rows[0].className, 'java.lang.String');
});

test('plain-text investigation guidance becomes action buttons', () => {
  const actions = extractSuggestedActions('Overall Memory Usage\nTop Memory Consumers (Classes)\nDominant Objects\nLeak Detection\nDeep Inspection');
  assert.deepEqual(actions.map(action => action.label), ['Overall Memory Usage', 'Top Memory Consumers', 'Dominant Objects', 'Leak Detection', 'Deep Inspection']);
});

test('duplicate visual components render only once', () => {
  const view = normalizeAgentResponse({ components: [
    { type: 'chart', title: 'Retained classes', chartType: 'bar', data: [{ label: 'A', value: 1 }] },
    { type: 'chart', title: 'Retained classes', chartType: 'bar', data: [{ label: 'A', value: 1 }] }
  ] });
  assert.equal(view.components.length, 1);
});

test('OQL guard follows the Calcite SQL dialect', () => {
  assert.equal(validateOql('SELECT * FROM java.lang.String s WHERE s.count > 10'), null);
  assert.equal(validateOql('SELECT title FROM ConferenceSession'), null);
  assert.equal(validateOql('SELECT * FROM java.lang.String LIMIT 10'), null);
  assert.equal(validateOql('SELECT DISTINCT title FROM ConferenceSession'), null);
  assert.equal(validateOql('SELECT s.this, e.key FROM java.util.Properties s, LATERAL TABLE(getMapEntries(s.this)) e'), null);
  assert.equal(validateOql('SELECT toString(s.this) AS unique_value, COUNT(*) AS count FROM "java.lang.String" s GROUP BY toString(s.this) ORDER BY COUNT(*) DESC'), null);
  assert.match(validateOql('SELECT * FROM java.lang.String;'), /one Calcite SELECT statement/);
  assert.match(validateOql('SELECT * FROM java.lang.String WHERE ('), /unbalanced parentheses/);
  assert.match(validateOql('UPDATE java.lang.String SET value = 1'), /read-only Calcite SELECT/);
  assert.match(validateOql('SELECT s.toString(s.this) FROM "java.lang.String" s'), /Calcite SQL function toString/);
});

test('OQL agent context uses Calcite function syntax for common String values', () => {
  assert.match(OQL_AGENT_RULES, /toString\(s\.this\).*never.*s\.toString\(s\.this\)/s);
  assert.match(OQL_AGENT_RULES, /toString\(s\.this\) AS val, count\(\*\) AS cnt FROM "java\.lang\.String" s GROUP BY toString\(s\.this\) ORDER BY COUNT\(\*\) DESC/);
  assert.match(OQL_AGENT_RULES, /HAVING COUNT\(\*\) > 1 ORDER BY SUM\(retainedSize\(this\)\) DESC/);
  assert.match(OQL_AGENT_RULES, /LATERAL TABLE\(getMapEntries\(p\.this\)\)/);
  assert.match(OQL_AGENT_RULES, /UNNEST\(asMultiSet\(fpc\.perms\)\)/);
});

test('agent context instructs query retries to simplify failed OQL', () => {
  assert.match(AGENT_SYSTEM_PROMPT, /Before constructing or calling heap_run_oql, always call heap_get_oql_grammar/);
  assert.match(AGENT_SYSTEM_PROMPT, /Do not call heap_find_path_to_gc_roots as a substitute for outboundReferences/);
  assert.match(AGENT_SYSTEM_PROMPT, /Graph nodes and edges must be top-level graph properties/);
  assert.match(AGENT_SYSTEM_PROMPT, /heap_run_oql fails, do not stop/);
  assert.match(AGENT_SYSTEM_PROMPT, /retry up to three times/);
  assert.match(AGENT_SYSTEM_PROMPT, /Do not repeat the same failed query/);
  assert.match(AGENT_SYSTEM_PROMPT, /all OQL attempts fail, stop trying to guess/);
  assert.match(AGENT_SYSTEM_PROMPT, /need more information/);
});
