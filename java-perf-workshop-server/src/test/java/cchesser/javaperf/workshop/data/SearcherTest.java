package cchesser.javaperf.workshop.data;

import cchesser.javaperf.workshop.data.Searcher.SearchResult;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link cchesser.javaperf.workshop.data.Searcher}.
 */
public class SearcherTest {

    @Test
    public void testSearch() {
        Searcher searcher = new Searcher(new ConferenceSessionLoader());
        SearchResult result = searcher.search("jvm");
        assertNotNull(result);
        assertTrue(result.getResults().size() > 0);
    }
}
