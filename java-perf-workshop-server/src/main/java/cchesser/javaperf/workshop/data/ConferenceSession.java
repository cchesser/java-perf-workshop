package cchesser.javaperf.workshop.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a session at the KCDC 2015 conference. This type is utilized to
 * map to JSON content from the backing conference service.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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

	private String asciiArt;

	@JsonProperty("_id")
	private String sessionId;

	public String getTitle() {
		return title;
	}

	public Presenter getPresenter() {
		return presenter;
	}

	public String getAbstract() {
		return sessionAbstract;
	}

	public String getAsciiArt() {
		return asciiArt;
	}

	public String getSessionId() {
		return sessionId;
	}

	/**
	 * This API is exposed to allow some silly use-case of adding a completely
	 * un-necessary attribute, to this, otherwise, type which could be immutable
	 * and doesn't need this content.
	 * 
	 * @param asciiArt
	 *            ASCII art string to take.
	 */
	public void setAsciiArt(String asciiArt) {
		this.asciiArt = asciiArt;
	};

	public List<String> getTags() {
		// Returning null when tags may be empty. Note, this pattern is being
		// applied
		// to expose a poor pattern of not defining expectations well and the
		// cost
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
		sb.append(", sessionId='").append(sessionId).append('\'');
		sb.append('}');
		return sb.toString();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
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
