---
title: "Setup"
weight: 7
description: >
    Instructions for setting up the workshop
---

In this section we're going to spin up the services needed for the workshop.

{{% alert title="Note" color="info" %}}
Make sure you've gone through the Prerequisites
{{% /alert %}}


### Service

The web service is a [simple dropwizard server](http://www.dropwizard.io/), which has a single API to search for talks
from [kcdc.info](http://www.kcdc.info/), and returns results about these talks.

![](/diagrams/workshop_service_interaction.png)

#### Running

**Prerequisites:**

* Maven
* Java JDK

Assemble the service:

```bash
mvn clean package
```

💡 If you have issues with building it locally due to your setup, you can download the
server assembly [here](https://github.com/cchesser/java-perf-workshop/wiki/java-perf-workshop-server-1.0-SNAPSHOT.jar).

Start the workshop service:

```bash
java -jar java-perf-workshop-server/target/java-perf-workshop-server-1.1.0-SNAPSHOT.jar server server.yml
```

#### Mocking Service Dependency

To simulate responses of kcdc.info (as the service may change over time), we will first run a mock
instance of this service using [WireMock](http://wiremock.org/). 

![](/diagrams/interaction_wiremock.png)

Go ahead and start another terminal session where we will run another service to mock a remote dependency of the workshop service. Navigate
to the same directory where you cloned this repository, then execute the following commands:

```bash
mvn dependency:copy -Dartifact=com.github.tomakehurst:wiremock-standalone:2.24.1 -Dmdep.stripVersion=true -DoutputDirectory=.
```

Run the mock service, which will provide the essential end-points to support the service we will be
testing:

```bash
java -jar wiremock-standalone.jar --port 9090 --root-dir java-perf-workshop-server/src/test/resources
```

Alternatively, you can run the `mockservice.sh` script which will do both commands, ie: `sh mockservice.sh`

#### Configuration

Example configuration of service:

```
server:
  applicationConnectors:
    - type: http
      port: 80
  adminConnectors:
    - type: http
      port: 8071
  requestLog:
    timeZone: UTC
    appenders:
      - type: file
        currentLogFilename: /var/log/java-perf-workshop-server-access.log
        threshold: ALL
        archive: true
        archivedLogFilenamePattern: /var/log/java-perf-workshop-server-access.%d.log.gz
        archivedFileCount: 5
logging:
  level: INFO
  appenders:
    - type: file
      currentLogFilename: /var/log/java-perf-workshop-server.log
      threshold: ALL
      archive: true
      archivedLogFilenamePattern: /var/log/java-perf-workshop-server-%d.log
      archivedFileCount: 5
      timeZone: UTC
```

#### Testing

The service will return back results from the KCDC website on sessions that are available which contain
a substring in their title, abstract, or tags. Example:

[http://localhost:8080/search?q=clojure](http://localhost:8080/search?q=clojure)

Example results:

```json
{
  "results" : [ {
    "title" : "Concurrency Options on the JVM",
    "presenter" : "Jessica Kerr"
  }, {
    "title" : "Fast, Parallel, or Reliable: Pick 3, a tour of Elixir",
    "presenter" : "Jordan Day"
  } ]
}
```

#### Troubleshooting

If you get a `500` error message when trying to test the service, verify that the wiremock server is running.
```
{
  "code" : 500,
  "message" : "There was an error processing your request. It has been logged (ID d8998189f8d4ee8c)."
}
```

### Reference

* [KCDC 2015 workshop slides](https://github.com/cchesser/java-perf-workshop/wiki/slides/kcdc2015_whats_in_you_jvm.zip)


