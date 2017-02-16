package cchesser.javaperf.workshop;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

public class WorkshopConfiguration extends Configuration {

    @NotEmpty
    private String conferenceServiceHost = "localhost:9090";

    @JsonProperty
    public String getConferenceServiceHost() {
        return conferenceServiceHost;
    }

    @JsonProperty
    public void setSonferenceServiceHost(String host) {
        this.conferenceServiceHost = host;
    }

}