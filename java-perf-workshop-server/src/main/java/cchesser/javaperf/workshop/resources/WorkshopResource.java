package cchesser.javaperf.workshop.resources;

import cchesser.javaperf.workshop.WorkshopConfiguration;
import cchesser.javaperf.workshop.cache.HistoricalCache;
import cchesser.javaperf.workshop.data.ConferenceSession;
import cchesser.javaperf.workshop.data.ConferenceSessionLoader;
import cchesser.javaperf.workshop.data.Searcher;
import cchesser.javaperf.workshop.data.Searcher.SearchResult;
import com.codahale.metrics.annotation.Timed;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/")
public class WorkshopResource {

    private Searcher searcher;

    private HistoricalCache<String, SearchResult> resultsByContext;

    public WorkshopResource(WorkshopConfiguration conf) {
        searcher = new Searcher(new ConferenceSessionLoader(conf));
        resultsByContext = new HistoricalCache<>(conf.getHistoricalCache().getCacheLimit(), conf.getHistoricalCache().getDiscardLimit());
    }

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Searcher.SearchResult searchConference(@QueryParam("q") String term, @QueryParam("c") String context) {
        return fetchResults(term, context);
    }

    @GET
    @Path("/ascii")
    @Produces(MediaType.TEXT_PLAIN)
    @Timed
    public String getAscii(@QueryParam("q") String term, @QueryParam("c") String context) {
        Searcher.SearchResult result = fetchResults(term, context);

        StringBuilder sb = new StringBuilder();
        for (Searcher.SearchResultElement element : result.getResults()) {
            sb.append(element.getAsciiArt());
            sb.append("\n\n");
        }

        return sb.toString();
    }

    @GET
    @Path("/sessions/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public ConferenceSession getSessionDetails(@PathParam("id") String sessionId) {
        return searcher.getSession(sessionId);
    }

    private Searcher.SearchResult fetchResults(String term, String context) {
        // fetch it!
        if (context != null && !context.isEmpty()) {
            if (resultsByContext.exists(context)) {
                return resultsByContext.fetch(context);
            }
        }

        // does not exist in cache, compute and store
        SearchResult results = searcher.search(term);
        resultsByContext.store(results.getResultsContext(), results);
        return results;
    }

}
