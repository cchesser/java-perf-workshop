package cchesser.javaperf.workshop;

import cchesser.javaperf.workshop.health.RemoveServiceHealthCheck;
import cchesser.javaperf.workshop.resources.WorkshopResource;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class WorkshopApplication extends Application<WorkshopConfiguration> {

    public static void main(String[] args) throws Exception {
        new WorkshopApplication().run(args);
    }

    @Override
    public String getName() {
        return "workshop";
    }

    @Override
    public void initialize(Bootstrap<WorkshopConfiguration> bootstrap) {
    }

    @Override
    public void run(WorkshopConfiguration configuration, Environment environment) {
        final WorkshopResource resource = new WorkshopResource();
        environment.jersey().register(resource);
        environment.healthChecks().register("remoteService", new RemoveServiceHealthCheck());
        environment.getObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }
}