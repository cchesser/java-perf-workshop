package cchesser.javaperf.workshop.data;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import cchesser.javaperf.workshop.data.Searcher.SearchResult;

/**
 * Tests the {@link cchesser.javaperf.workshop.data.Searcher}.
 */
public class SearcherTest extends BaseTest {

    @Test
    public void testSearch() {
        setupMock();
        Searcher searcher = new Searcher(new ConferenceSessionLoader(getConfiguration()));
        SearchResult result = searcher.search("clojure");
        assertNotNull(result);
        assertTrue(result.getResults().size() > 0);
    }
}
