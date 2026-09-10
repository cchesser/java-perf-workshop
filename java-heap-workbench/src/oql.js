export const OQL_AGENT_RULES = `OQL query language (Apache Calcite SQL; full grammar: https://calcite.apache.org/docs/reference.html):
- OQL is backed by Apache Calcite SQL by default. Always formulate OQL as a read-only Calcite SELECT query; do not generate Eclipse MAT OQL syntax or switch dialects.
- Only read-only SELECT statements are supported. Use the Calcite query shape SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ... ORDER BY ... LIMIT/OFFSET or FETCH. Standard SQL is supported for filtering and projection (WHERE, CASE, DISTINCT), joins (JOIN ... ON), aggregation (COUNT, SUM, AVG, MIN, MAX with GROUP BY/HAVING), sorting (ORDER BY), set operations (UNION/EXCEPT/INTERSECT), and paging (LIMIT/OFFSET/FETCH). Submit one SELECT statement without a semicolon. Do not send EXPLAIN, DESCRIBE, INSERT, UPDATE, DELETE, or other statements to heap_run_oql.
- Heap schema: each Java class is a table containing its direct instances, without subclasses. Use the 'instanceof' schema/table when subclasses should be included. Quote a fully qualified class table when needed, for example FROM "java.util.HashMap" h. The special 'this' column is the current heap object; Java fields are exposed as columns.
- Qualify fields with table aliases. Use this['fieldName'] for dynamic field lookup and this['@className'], this['@class'], this['@shallow'], and this['@retained'] for virtual properties. Use double-quoted identifiers for class names or field names that need quoting; quoted identifiers are case-sensitive. Use single quotes for string literals. Do not use Java method syntax on aliases.
- Useful heap functions include getId, getAddress, getType, toString, shallowSize, retainedSize, length, getSize, getField, getStaticField, getStringContent, getValues, getMapEntries, getInboundReferences, getOutboundReferences, and getRetainedSet. These are SQL functions: call them as toString(s.this), retainedSize(s.this), or getField(s.this, 'fieldName'); never call them as row-alias methods such as s.toString(s.this).
- Expand references and maps with LATERAL TABLE(getOutboundReferences(o.this)), LATERAL TABLE(getInboundReferences(o.this)), or LATERAL TABLE(getMapEntries(o.this)). Use UNNEST(asMultiSet(collection)) for collections and UNNEST(asArray(referenceArray)) for arrays; SQL array indexes start at 1, and WITH ORDINALITY adds an index column. Collection conversion helpers include asMap, asMultiSet, asArray, asByteArray, asShortArray, asIntArray, asLongArray, asBooleanArray, asCharArray, asFloatArray, and asDoubleArray.
- Canonical frequency query for String objects: SELECT toString(s.this) AS val, count(*) AS cnt FROM "java.lang.String" s GROUP BY toString(s.this) ORDER BY COUNT(*) DESC. Keep the function call exactly in function form; do not rewrite it as s.toString(s.this). Add a WHERE clause before GROUP BY when filtering values.
- Examples: SELECT toString(file) AS file_str, COUNT(*) AS cnt, SUM(retainedSize(this)) AS sum_retained, SUM(shallowSize(this)) AS sum_shallow FROM java.net.URL GROUP BY toString(file) HAVING COUNT(*) > 1 ORDER BY SUM(retainedSize(this)) DESC; SELECT s.this, u.this FROM "java.lang.String" s JOIN "java.net.URL" u ON s.this = u.path; SELECT p.this, e.key, e."value" FROM java.util.Properties p, LATERAL TABLE(getMapEntries(p.this)) e; SELECT fpc.this, fp.fp_ref FROM java.io.FilePermissionCollection fpc, UNNEST(asMultiSet(fpc.perms)) fp(fp_ref); SELECT c.this, cs.index, cs.val FROM java.util.GregorianCalendar c, UNNEST(asIntArray(c.stamp)) WITH ORDINALITY cs(val, index).
- Aggregate rule: in an aggregate query, every SELECT, HAVING, and ORDER BY expression must be grouped, constant, or an aggregate. Prefer repeating the aggregate expression in ORDER BY (for example ORDER BY COUNT(*) DESC or ORDER BY SUM(retainedSize(this)) DESC) instead of relying on an alias.
- The MCP tool's limit and offset arguments cap rows returned to the UI. Use SQL LIMIT/OFFSET when it is part of the query's intended semantics; otherwise rely on the tool limit.
- Use the inspection and GC-root tools after an OQL query returns an object reference. Never claim fields or values that the tool did not return.`;

// Lightweight guard for the Calcite SELECT dialect. Calcite owns the final
// parse; this prevents mutations, multiple statements, and malformed input.
export function validateOql(query) {
  const source = String(query || '').trim();
  if (!source) return 'OQL query is empty.';
  if (source.includes(';')) return 'OQL must contain one Calcite SELECT statement without a semicolon.';
  if (!/^SELECT\b/i.test(source)) return 'OQL must be a read-only Calcite SELECT statement.';
  if (!/\bFROM\b/i.test(source)) return 'OQL SELECT must contain a FROM clause naming a heap table.';
  if (/\b(INSERT|UPDATE|DELETE|MERGE|ALTER|DROP|TRUNCATE)\b/i.test(source)) return 'Only read-only Calcite SELECT statements are allowed for heap queries.';
  if (/\b[A-Za-z_][A-Za-z0-9_]*\s*\.\s*toString\s*\(/i.test(source)) return 'Use the Calcite SQL function toString(expression), not alias.toString(expression).';
  let quote = null;
  let depth = 0;
  for (const character of source) {
    if (quote) { if (character === quote) quote = null; continue; }
    if (character === '"' || character === "'") { quote = character; continue; }
    if (character === '(') depth += 1;
    if (character === ')') depth -= 1;
    if (depth < 0) return 'OQL has an unmatched closing parenthesis.';
  }
  if (quote) return 'OQL has an unterminated quoted identifier or string literal.';
  if (depth !== 0) return 'OQL has unbalanced parentheses.';
  return null;
}
