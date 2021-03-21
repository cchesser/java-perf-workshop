---
title: "Setup"
weight: 1
description: >
    Instructions for setting up the workshop services through docker
---

In this section we're going to spin up the containers needed for the workshop.

{{% alert title="Note" color="info" %}}
* Make sure you've gone through the [Prerequisites](/docs/prereqs).
* Make sure your Docker daemon is running.
* If this is the first time you're using docker, we recommend going through [Orientation and Setup](https://docs.docker.com/get-started/) to quickly learn a few concepts.
{{% /alert %}}

## Building

The maven project uses the [fabric8.io/docker-maven-plugin](https://dmp.fabric8.io/) to create two images:

* `workshop-server` - the docker image for the workshop service
* `workshop-wiremock` - the docker image for the wiremock service

Run `mvn clean package -Pdocker` , the `docker` profile enables the docker-maven-plugin.

You can view the generated images with `docker image ls | grep workshop`:

```bash
$ docker image ls | grep workshop
workshop-wiremock                                                             1.1.0-SNAPSHOT                 2cc43b2348c8   2 minutes ago   657MB
workshop-wiremock                                                             latest                         2cc43b2348c8   2 minutes ago   657MB
workshop-server                                                               1.1.0-SNAPSHOT                 be7cfbd0735a   2 minutes ago   659MB
workshop-server                                                               latest                         be7cfbd0735a   2 minutes ago   659MB
```

## Running

Since our Workshop Service depends on the Wiremock Service, we're going to use [docker-compose](https://docs.docker.com/compose/) to create a docker environment with both our services ready to go:

![](/diagrams/workshop_docker_setup.png)

Within the `java-perf-workshop` directory, run `docker-compose up`:

```bash
$ docker-compose up
Creating network "java-perf-workshop_default" with the default driver
Creating java-perf-workshop_wiremock_1 ... done
Creating java-perf-workshop_server_1   ... done
Attaching to java-perf-workshop_wiremock_1, java-perf-workshop_server_1
...
wiremock_1  | port:                         8080
wiremock_1  | enable-browser-proxying:      false
wiremock_1  | disable-banner:               false
wiremock_1  | no-request-journal:           false
wiremock_1  | verbose:                      false
wiremock_1  | 
...
server_1    | INFO  [2021-03-14 18:59:06,883] org.eclipse.jetty.server.AbstractConnector: Started application@4c777e7b{HTTP/1.1,[http/1.1]}{0.0.0.0:8080}
server_1    | INFO  [2021-03-14 18:59:06,892] org.eclipse.jetty.server.AbstractConnector: Started admin@5f038248{HTTP/1.1,[http/1.1]}{0.0.0.0:8081}
server_1    | INFO  [2021-03-14 18:59:06,892] org.eclipse.jetty.server.Server: Started @4358ms
```

In another terminal, you can check the status of the containers by running `docker ps`:
```bash
$ docker ps
CONTAINER ID   IMAGE                      COMMAND                  CREATED          STATUS          PORTS                              NAMES
c9aeb5375f79   workshop-server:latest     "/bin/sh -c 'java -j…"   52 seconds ago   Up 50 seconds   0.0.0.0:8080-8081->8080-8081/tcp   java-perf-workshop_server_1
6b1522e7acb9   workshop-wiremock:latest   "/bin/sh -c 'java -j…"   52 seconds ago   Up 51 seconds                                      java-perf-workshop_wiremock_1
```

Our workshop service container is exposing port `8080` and mapping it into the container's `8080`. Verify that your setup is working by visiting: [http://localhost:8080/search?q=docker](http://localhost:8080/search?q=docker).


&nbsp;
{{% nextSection %}}
In the next section, we'll learn how to enable Java Monitoring Tooling to work with containers.
{{% /nextSection %}}