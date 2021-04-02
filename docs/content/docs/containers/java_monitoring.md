---
title: "Java Monitoring Tooling"
weight: 2
description: >
    Instructions for enabling remote tooling on our JVM
---

In this section we're going to configure our container to enable remote tooling.

{{% alert title="Note" color="info" %}}
* Make sure you've gone through the [Prerequisites](/docs/prereqs).
* Make sure your Docker daemon is running.
* If this is the first time you're using docker, we recommend going through [Orientation and Setup](https://docs.docker.com/get-started/) to quickly learn a few concepts.
{{% /alert %}}

## Remote Monitoring

In previous sections of the workshop, we ran all our tooling without any configuration. In previous versions of Java you would have had to configure things even for local monitoring, this is no longer the case with Java 6+:

> Any application that is started on the Java SE 6 platform will support the Attach API, and so will automatically be made available for local monitoring and management when needed.

Since the docker-compose network and containers are ran separate from our host (consider them a different machine), we need to enable [remote monitoring and management](https://docs.oracle.com/javase/8/docs/technotes/guides/management/agent.html).

![](/diagrams/jmx_remote_docker.png)

### Properties

We'll set the following properties as a `JAVA_OPTS` environment variable when we start our workshop server container:

* The `JMX remote port` to `8082`: `-Dcom.sun.management.jmxremote.port=8082`
* The RMI registry port also set to `8082`: `-Dcom.sun.management.jmxremote.rmi.port=8082`
* Disabling for both the registry and jmx: `-Dcom.sun.management.jmxremote.registry.ssl=false` and `-Dcom.sun.management.jmxremote.ssl=false`
* Accept connections not from localhost: `-Dcom.sun.management.jmxremote.local.only=false`
  * Since the machine we are connecting from will not be in the container network, we need to allow non localhost connections.
* The host name for the [RMI server](https://docs.oracle.com/javase/8/docs/technotes/guides/rmi/javarmiproperties.html) will be set to `127.0.0.1`. The default value for this will be the container's IP address, which we are overridding.

Set these values as an `environment` property on your workshop container:

```yaml
...
  server:
    image: workshop-server:latest
    environment:
      JAVA_OPTS: "
        -Dcom.sun.management.jmxremote.port=8082
        -Dcom.sun.management.jmxremote.rmi.port=8082
        -Dcom.sun.management.jmxremote.registry.ssl=false
        -Dcom.sun.management.jmxremote.authenticate=false
        -Dcom.sun.management.jmxremote.ssl=false
        -Dcom.sun.management.jmxremote.local.only=false
        -Djava.rmi.server.hostname=127.0.0.1
      "
...
```

## Test Our Setup

Spin up your services again with `docker-compose up`. Once the services are started, use `docker ps` to check the open ports on the workshop server. Notice that `8082` is now mapped as well.
```bash
$ docker ps
CONTAINER ID   IMAGE                      COMMAND                  CREATED          STATUS          PORTS                              NAMES
44d4a1ebedef   workshop-server:latest     "/bin/sh -c 'java $J…"   39 seconds ago   Up 37 seconds   0.0.0.0:8080-8082->8080-8082/tcp   java-perf-workshop_server_1
```

### JDK Mission Control

We'll use JDK Mission Control to create a JMX Connection. 

Open JDK Mission Control. Notice that the JVM Browser no longer shows the two services (since they no longer are running on the local host):

![](/containers/jmx/empty_browser.png)

Create a new JMX Connection using `0.0.0.0` and `8082` as the host and port:

![](/containers/jmx/new_connection.png)

With our setup, we can connect with other addresses as well:

* `127.0.0.1`
* `localhost` (since we are exposing port 8082 on the container as port 8082 on the local host)
* Our wifi/ethernet IP address, which you can find under the `en0` interface using `ifconfig/ipconfig`:
  ```bash
  $ ifconfig
  ...
  en0: flags=8863<UP,BROADCAST,SMART,RUNNING,SIMPLEX,MULTICAST> mtu 1500
	ether 8c:85:90:ba:52:10 
	inet6 fe80::1862:95fe:55e7:284e%en0 prefixlen 64 secured scopeid 0x9 
	inet 192.168.1.189 netmask 0xffffff00 broadcast 192.168.1.255
  ...
  ```

![](/containers/jmx/all_jmx_servers.png)

&nbsp;
{{% nextSection %}}
In the next section, we'll learn about some Docker tooling.
{{% /nextSection %}}
