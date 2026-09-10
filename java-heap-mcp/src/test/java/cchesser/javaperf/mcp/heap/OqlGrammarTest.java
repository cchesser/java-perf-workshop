package cchesser.javaperf.mcp.heap;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OqlGrammarTest {
    @Test
    void describesTheReadOnlyCalciteHeapQueryContract() {
        Map<String, Object> grammar = OqlGrammar.describe();

        assertThat(grammar)
                .containsEntry("readOnly", true)
                .containsEntry("statementContract", "Exactly one SELECT statement, without a trailing semicolon");
        @SuppressWarnings("unchecked")
        Map<String, Object> syntax = (Map<String, Object>) grammar.get("grammar");
        assertThat(syntax).containsKeys("select", "tablePrimary");
        assertThat(syntax.get("select")).isInstanceOf(String.class);
        assertThat(syntax.get("tablePrimary")).isInstanceOf(String.class);
        assertThat((List<?>) grammar.get("examples"))
                .anyMatch(example -> example.toString().contains("LATERAL TABLE(getOutboundReferences"));
        @SuppressWarnings("unchecked")
        Map<String, Object> references = (Map<String, Object>) grammar.get("references");
        assertThat(references)
                .containsEntry("calciteSqlReference", "https://calcite.apache.org/docs/reference.html")
                .containsEntry("matCalcitePlugin", "https://github.com/vlsi/mat-calcite-plugin");
    }
}
