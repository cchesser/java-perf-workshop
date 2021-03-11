package cchesser.javaperf.workshop.data;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import org.junit.Rule;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import cchesser.javaperf.workshop.WorkshopConfiguration;

public class BaseTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().port(9090));

    public void setupMock() {
        stubFor(get(urlEqualTo("/sessions"))
                .willReturn(aResponse()
                        .withBodyFile("sessions.json")));

        stubFor(get(urlEqualTo("/precompilers"))
                .willReturn(aResponse()
                        .withBodyFile("precompilers.json")));
        WorkshopConfiguration conf = new WorkshopConfiguration();
        conf.setConferenceServiceHost("localhost:9090");
    }

    protected WorkshopConfiguration getConfiguration() {
        WorkshopConfiguration conf = new WorkshopConfiguration();
        conf.setConferenceServiceHost("localhost:9090");
        return conf;
    }
}
