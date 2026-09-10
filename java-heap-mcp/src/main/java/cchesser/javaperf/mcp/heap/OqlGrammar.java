package cchesser.javaperf.mcp.heap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent-facing description of the SQL dialect accepted by heap_run_oql.
 *
 * <p>This is deliberately a server contract rather than a copy of Calcite's
 * complete parser grammar. Calcite can parse mutating statements, while this
 * service only permits one read-only SELECT statement.</p>
 */
public final class OqlGrammar {
    private OqlGrammar() {
    }

    public static Map<String, Object> describe() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dialect", "Apache Calcite SQL backed by the MAT Calcite plugin");
        result.put("version", "1");
        result.put("readOnly", true);
        result.put("statementContract", "Exactly one SELECT statement, without a trailing semicolon");
        result.put("grammar", Map.of(
                "statement", "query",
                "query", "[ WITH [ RECURSIVE ] withItem [, ...] ] (select | selectWithoutFrom | query UNION [ALL | DISTINCT] query | query EXCEPT [ALL | DISTINCT] query | query INTERSECT [ALL | DISTINCT] query) [ ORDER BY orderItem [, ...] ] [ LIMIT [start,] count ] [ OFFSET start { ROW | ROWS } ] [ FETCH { FIRST | NEXT } [count] { ROW | ROWS } ONLY ]",
                "select", "SELECT [ ALL | DISTINCT ] projectItem [, projectItem ...] FROM tableExpression [ WHERE booleanExpression ] [ GROUP BY groupItem [, ...] ] [ HAVING booleanExpression ]",
                "projectItem", "expression [ AS columnAlias ] | tableAlias.*",
                "tableExpression", "tableReference [, tableReference ...] | tableExpression [NATURAL] [LEFT|RIGHT|FULL] [OUTER] JOIN tableExpression joinCondition | tableExpression CROSS JOIN tableExpression",
                "tableReference", "tablePrimary [ AS alias ]",
                "tablePrimary", "[catalog.]schema.table | UNNEST(expression) [WITH ORDINALITY] | [LATERAL] TABLE(functionName(expression [, ...]))",
                "joinCondition", "ON booleanExpression | USING (column [, ...])",
                "orderItem", "expression [ ASC | DESC ] [ NULLS FIRST | NULLS LAST ]",
                "expression", "identifiers, literals, function calls, CASE, arithmetic, comparisons, boolean operators, MAP/ARRAY indexing, and CAST"
        ));
        result.put("restrictions", List.of(
                "Only SELECT queries are supported; do not use EXPLAIN, DESCRIBE, INSERT, UPDATE, DELETE, MERGE, SET, or RESET.",
                "Send one statement without a semicolon.",
                "Double quotes delimit Java class/table identifiers; single quotes delimit string literals.",
                "The MCP tool applies its own result limit and offset after query execution."
        ));
        result.put("heapSchema", Map.of(
                "classes", "Each Java class is a table of direct instances without subclasses, for example java.util.HashMap.",
                "subclasses", "Use the instanceof schema, for example instanceof.java.util.HashMap, to include subclass instances.",
                "quotedClasses", "Quote fully qualified class names when needed, for example \"java.util.HashMap\" or \"long[]\".",
                "specialColumn", "this is a reference to the current heap object.",
                "dynamicFields", "Use this['fieldName'] and virtual properties this['@className'], this['@class'], this['@shallow'], and this['@retained'].",
                "nativeTable", "native.ThreadStackFrames exposes thread stack and local-variable information."
        ));
        result.put("referenceFunctions", List.of(
                function("getId(ref)", "internal object identifier"),
                function("getAddress(ref)", "memory address"),
                function("getType(ref)", "runtime class name"),
                function("toString(ref)", "textual representation"),
                function("shallowSize(ref)", "shallow heap size"),
                function("retainedSize(ref)", "retained heap size"),
                function("length(ref)", "array length"),
                function("getSize(ref)", "collection, map, or array size"),
                function("getByKey(ref, key)", "value from a referenced map"),
                function("getField(ref, name)", "field value by name"),
                function("getStaticField(ref, name)", "static field value"),
                function("getStringContent(ref)", "formatted object content")
        ));
        result.put("tableFunctions", List.of(
                function("getRetainedSet(ref)", "table of retained objects"),
                function("getOutboundReferences(ref)", "(name, this) reference pairs"),
                function("getInboundReferences(ref)", "inbound object references"),
                function("getValues(ref)", "values in a Java collection"),
                function("getMapEntries(ref)", "(key, value) map-entry pairs")
        ));
        result.put("collectionFunctions", List.of(
                "asMap(ref)", "asMultiSet(ref)", "asArray(ref)", "asByteArray(ref)", "asShortArray(ref)",
                "asIntArray(ref)", "asLongArray(ref)", "asBooleanArray(ref)", "asCharArray(ref)",
                "asFloatArray(ref)", "asDoubleArray(ref)"
        ));
        result.put("examples", List.of(
                "SELECT s.this FROM \"java.lang.String\" s",
                "SELECT toString(file) AS file_str, COUNT(*) AS cnt FROM java.net.URL GROUP BY toString(file) HAVING COUNT(*) > 1 ORDER BY SUM(retainedSize(this)) DESC",
                "SELECT u.this, refs.name, refs.this AS reference FROM java.net.URL u, LATERAL TABLE(getOutboundReferences(u.this)) refs",
                "SELECT p.this, vals.key, vals.\"value\" FROM java.util.Properties p, LATERAL TABLE(getMapEntries(p.this)) vals",
                "SELECT arrays.this, asLongArray(arrays.this)[1] AS first_element FROM \"long[]\" arrays WHERE length(arrays.this) > 0"
        ));
        result.put("references", Map.of(
                "calciteSqlReference", "https://calcite.apache.org/docs/reference.html",
                "matCalcitePlugin", "https://github.com/vlsi/mat-calcite-plugin"
        ));
        return result;
    }

    private static Map<String, String> function(String signature, String description) {
        return Map.of("signature", signature, "description", description);
    }
}
