package cchesser.javaperf.workshop.cache;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class CacheConfiguration {

    @Min(32)
    @Max(65536)
    @JsonProperty
    private int cacheLimit = 250;

    @JsonProperty
    public int getCacheLimit() {
        return cacheLimit;
    }

    @JsonProperty
    public void setCacheLimit(int cacheLimit) {
        this.cacheLimit = cacheLimit;
    }
}
