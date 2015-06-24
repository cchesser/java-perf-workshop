package cchesser.javaperf.workshop.health;

import cchesser.javaperf.workshop.data.ConferenceSessionLoader;
import com.codahale.metrics.health.HealthCheck;

/**
 * Health check to inspect that remote service dependency is available.
 */
public class RemoveServiceHealthCheck extends HealthCheck {

    private ConferenceSessionLoader loader = new ConferenceSessionLoader();

    @Override
    protected Result check() throws Exception {
        return loader.isRemoteServiceAvailable() ? Result.healthy() : Result.unhealthy("KCDC service dependency is not available.");
    }
}
