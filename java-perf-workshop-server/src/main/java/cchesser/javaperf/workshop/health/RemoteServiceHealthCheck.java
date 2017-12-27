package cchesser.javaperf.workshop.health;

import com.codahale.metrics.health.HealthCheck;
import cchesser.javaperf.workshop.WorkshopConfiguration;
import cchesser.javaperf.workshop.data.ConferenceSessionLoader;

/**
 * Health check to inspect that remote service dependency is available.
 */
public class RemoteServiceHealthCheck extends HealthCheck {

    private ConferenceSessionLoader loader;

    public RemoteServiceHealthCheck(WorkshopConfiguration conf) {
       this.loader = new ConferenceSessionLoader(conf);
    }

    @Override
    protected Result check() throws Exception {
        return loader.isRemoteServiceAvailable() ? Result.healthy() : Result.unhealthy("KCDC service dependency is not available.");
    }
}
