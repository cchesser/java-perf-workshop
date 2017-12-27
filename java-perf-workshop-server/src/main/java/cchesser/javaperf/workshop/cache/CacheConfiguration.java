package cchesser.javaperf.workshop.cache;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

public class CacheConfiguration {

    @Min(32)
    @Max(65536)
    @JsonProperty
    private int cacheLimit = 10000;

    @Min(1024)
    @Max(65536)
    @JsonProperty
    private int discardLimit = 65536;

    @JsonProperty
    public int getCacheLimit() {
        return cacheLimit;
    }

    @JsonProperty
    public void setCacheLimit(int cacheLimit) {
        this.cacheLimit = cacheLimit;
    }

    @JsonProperty
    public int getDiscardLimit() {
        return discardLimit;
    }

    @JsonProperty
    public void setDiscardLimit(int discardLimit) {
        this.discardLimit = discardLimit;
    }
}
