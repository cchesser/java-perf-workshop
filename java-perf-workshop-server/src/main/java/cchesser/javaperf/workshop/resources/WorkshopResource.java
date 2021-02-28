package cchesser.javaperf.workshop.resources;

import cchesser.javaperf.workshop.WorkshopConfiguration;
import cchesser.javaperf.workshop.cache.CleverCache;
import cchesser.javaperf.workshop.data.ConferenceSession;
import cchesser.javaperf.workshop.data.ConferenceSessionLoader;
import cchesser.javaperf.workshop.data.Schedule;
import cchesser.javaperf.workshop.data.Searcher;
import cchesser.javaperf.workshop.data.Searcher.SearchResult;
import com.codahale.metrics.annotation.Timed;

import javax.print.attribute.standard.Media;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/")
public class WorkshopResource {

    private final ConferenceSessionLoader loader;
    private Searcher searcher;

    private CleverCache<String, SearchResult> resultsByContext;

    public WorkshopResource(WorkshopConfiguration conf) {
        loader = new ConferenceSessionLoader(conf);
        searcher = new Searcher(loader);
        resultsByContext = new CleverCache<>(conf.getHistoricalCache().getCacheLimit());
    }

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Searcher.SearchResult searchConference(@QueryParam("q") String term, @QueryParam("c") String context) {
        return fetchResults(term, context);
    }

    @GET
    @Path("/sessions")
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public List<ConferenceSession> sessions() {
        return loader.load();
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
