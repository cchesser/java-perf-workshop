package cchesser.javaperf.workshop.data;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.github.tomakehurst.wiremock.WireMockServer;
import cchesser.javaperf.workshop.WorkshopConfiguration;

public class BaseTest {

    private final WireMockServer wireMockServer = new WireMockServer(options().port(9090));

    @BeforeEach
    void startWireMock() {
        wireMockServer.start();
    }

    @AfterEach
    void stopWireMock() {
        wireMockServer.stop();
    }

    public void setupMock() {
        wireMockServer.stubFor(get(urlEqualTo("/sessions"))
                .willReturn(aResponse()
                        .withBodyFile("sessions.json")));

        wireMockServer.stubFor(get(urlEqualTo("/precompilers"))
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
