package cchesser.javaperf.workshop.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.Timed;
import cchesser.javaperf.workshop.WorkshopConfiguration;
import cchesser.javaperf.workshop.data.ConferenceSessionLoader;
import cchesser.javaperf.workshop.data.Searcher;

@Path("/")
public class WorkshopResource {

    private Searcher searcher;

    public WorkshopResource(WorkshopConfiguration conf) {
        searcher = new Searcher(new ConferenceSessionLoader(conf));
    }

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Searcher.SearchResult searchConference(@QueryParam("q") String term) {
       return searcher.search(term);
    }

    @GET
    @Path("/ascii")
    @Produces(MediaType.TEXT_PLAIN)
    @Timed
    public String getAscii(@QueryParam("q") String term) {
        StringBuilder sb = new StringBuilder();
        Searcher.SearchResult result = searcher.search(term);
        for(Searcher.SearchResultElement element : result.getResults()) {
            sb.append(element.getAsciiArt());
            sb.append("\n\n");
        }
        return sb.toString();
    }
}
