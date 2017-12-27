package cchesser.javaperf.workshop;

import cchesser.javaperf.workshop.cache.CacheConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

public class WorkshopConfiguration extends Configuration {

    @NotEmpty
    private String conferenceServiceHost = "localhost:9090";

    @JsonProperty
    private CacheConfiguration historicalCache = new CacheConfiguration();

    @JsonProperty
    public String getConferenceServiceHost() {
        return conferenceServiceHost;
    }

    @JsonProperty
    public void setSonferenceServiceHost(String host) {
        this.conferenceServiceHost = host;
    }

    @JsonProperty
    public CacheConfiguration getHistoricalCache() {
        return historicalCache;
    }

    @JsonProperty
    public void setHistoricalCache(CacheConfiguration historicalCache) {
        this.historicalCache = historicalCache;
    }
}