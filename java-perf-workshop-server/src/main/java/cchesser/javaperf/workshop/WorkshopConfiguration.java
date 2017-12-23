package cchesser.javaperf.workshop;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

public class WorkshopConfiguration extends Configuration {

	@NotEmpty
	private String conferenceServiceHost = "localhost:9090";

	@Min(32)
	@Max(65536)
	@JsonProperty
	private int cacheLimit = 65536;

	@Min(1024)
	@Max(65536)
	@JsonProperty
	private int discardLimit = 65536;

	@JsonProperty
	public String getConferenceServiceHost() {
		return conferenceServiceHost;
	}

	@JsonProperty
	public void setSonferenceServiceHost(String host) {
		this.conferenceServiceHost = host;
	}

	@JsonProperty
	public int getCacheLimit() {
		return cacheLimit;
	}

	public int getDiscardLimit() {
		return discardLimit;
	}

}