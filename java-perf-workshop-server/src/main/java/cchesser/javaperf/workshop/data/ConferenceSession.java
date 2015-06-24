package cchesser.javaperf.workshop.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Represents a session at the KCDC 2015 conference. This type is utilized to map to JSON content from the backing
 * conference service.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ConferenceSession {
    @JsonProperty
    private String title;

    @JsonProperty("user")
    private Presenter presenter;

    @JsonProperty("abstract")
    private String sessionAbstract;

    @JsonProperty
    private List<String> tags;

    @JsonProperty
    private String sessionType;

    public String getTitle() {
        return title;
    }

    public Presenter getPresenter() {
        return presenter;
    }

    public String getAbstract() {
        return sessionAbstract;
    }

    public List<String> getTags() {
        // Returning null when tags may be empty. Note, this pattern is being applied
        // to expose a poor pattern of not defining expectations well and the cost
        // of exception handling.
        return tags != null && tags.isEmpty() ? null : tags;
    }

    public String getSessionType() {
        return sessionType;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConferenceSession{");
        sb.append("title='").append(title).append('\'');
        sb.append(", presenter=").append(presenter);
        sb.append(", sessionAbstract='").append(sessionAbstract).append('\'');
        sb.append(", tags=").append(tags);
        sb.append(", sessionType='").append(sessionType).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public static class Presenter {

        @JsonProperty("displayName")
        private String name;

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Presenter{");
            sb.append("name='").append(name).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
