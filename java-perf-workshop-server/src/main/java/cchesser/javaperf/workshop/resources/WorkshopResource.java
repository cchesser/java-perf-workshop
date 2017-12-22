package cchesser.javaperf.workshop.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;
import cchesser.javaperf.workshop.WorkshopConfiguration;
import cchesser.javaperf.workshop.cache.RecoverableCache;
import cchesser.javaperf.workshop.data.ConferenceSession;
import cchesser.javaperf.workshop.data.ConferenceSessionLoader;
import cchesser.javaperf.workshop.data.Searcher;
import cchesser.javaperf.workshop.data.Searcher.SearchResult;

@Path("/")
public class WorkshopResource {

	private Searcher searcher;

	private RecoverableCache<String, SearchResult> resultsByContext;

	public WorkshopResource(WorkshopConfiguration conf) {
		searcher = new Searcher(new ConferenceSessionLoader(conf));
		resultsByContext = new RecoverableCache<>(conf.getCacheLimit(), conf.getDiscardLimit());
	}

	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	@Timed
	public Searcher.SearchResult searchConference(@QueryParam("q") String term, @QueryParam("c") String context) {

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

	@GET
	@Path("/ascii")
	@Produces(MediaType.TEXT_PLAIN)
	@Timed
	public String getAscii(@QueryParam("q") String term) {
		StringBuilder sb = new StringBuilder();
		Searcher.SearchResult result = searcher.search(term);
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
}
