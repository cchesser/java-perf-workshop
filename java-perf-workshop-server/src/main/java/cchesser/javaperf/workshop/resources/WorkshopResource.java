package cchesser.javaperf.workshop.resources;

import cchesser.javaperf.workshop.data.ConferenceSessionLoader;
import cchesser.javaperf.workshop.data.Searcher;
import com.codahale.metrics.annotation.Timed;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/")
public class WorkshopResource {

    private Searcher searcher;

    public WorkshopResource() {
        searcher = new Searcher(new ConferenceSessionLoader());
    }

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public Searcher.SearchResult searchConference(@QueryParam("q") String term) {
       return searcher.search(term);
    }
}
