---
title: "Introduction"
weight: 10
description: >
    Introduction into JDK Mission Control with Flight Recorder
resources:
- src: "**/*.png"
- src: "**/*.jfc"
---

## JDK Flight Recorder

The JDK Flight Recorder (JFR) is a very low overhead profiling and diagnostics tool. It was inherited from the JRockit JVM, and originally was offered as part of the HotSpot JVM. It is designed to be "black box" data recorder of the the run-time, which can be used in production environments, making it an attractive tool for profiling code since it has low overhead on the JVM. In 2018, it was open-sourced and released as part of OpenJDK.

JFR is enabled by default in Java 11 and later. In previous versions, it required enabling with JVM options (ex. `-XX:+FlightRecorder`)

### Higher Fidelity on Method Profiling

To get better fidelity on method profiling, include the following options which will enable the compiler to include additional metadata on non-safe code points. This is helpful, as sometimes the metadata will not fully resolve to the correct line in the code.

```bash
-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints
```

## JDK Mission Control

We will be using JDK Mission Control to monitor and evaluate the Java flight recordings. To utilizes JDK Mission Control, you will need to [download the build](https://jdk.java.net/jmc/9/). To start up JDK Mission Control, simply executing the following in your console:

```bash
jmc
```

💡 In order to be able to invoke `jmc` (JDK Mission Control) from your console, it assumes `$JAVA_HOME/bin` is on your `$PATH`. If it is not included, go ahead and update your profile to include this so you can easily invoke `jmc` from your terminal. Also, JMC 9.1 [requires JDK 21](https://www.oracle.com/java/technologies/javase/jmc9-install.html).

## Start Service with JFR

Let's start profiling our service. Start the service up by enabling JFR:

### Start from the console

```bash
# Note, if you are running this server from a different folder, consider changing the SERVER_HOME
SERVER_HOME=java-perf-workshop-server/target
java -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -jar $SERVER_HOME/java-perf-workshop-server-1.1.0-SNAPSHOT.jar server server.yml
```

## Start Flight Recording from JMC

{{< figure src="img/jfr-start.png" alt="JFR Start" >}}

This will open a window where you apply some settings for the recording. First select that you want this to be a __Continuous recording__ and for Event settings, we will import a template to get some consistent settings for profiling. Within the __Template Manager__, select __Import Files...__ and import the `open_jdk_9_.jfc` by [downloading it here](jfc/open_jdk_9+.jfc) folder. It should appear as _Java Performance Workshop JDK9+ Profile_. Select this as the __Event Settings__ and then click on __Finish__.

For reference, these are the options for the template.

{{< figure src="img/jfr-settings.png" alt="JFR Settings" >}}

First select that you want this to be a __Continuous recording__ and for Event settings we will use __Profiling on Server__.

{{< figure src="img/jfr-start-wizard.png" alt="JFR Configuration" >}}


Once your flight recording is being captured in a _Continuous_ recording, it will show a ∞.

{{< figure src="img/jfr-started.png" alt="JFR Started" >}}


💡 You can see the JFR templates (continuous / profile) which are shipped as part of the JRE in: `$JAVA_HOME/jre/lib/jfr`. These become helpful if you are wanting to compare your settings to some of the standard ones.

## Generate HTTP traffic on service

We will want to generate some traffic on the service to measure some general characteristics of the service:
* Throughput (requests per second)
* Response time
* Trend of response time over time

![](/diagrams/web_traffic.png)

By generating traffic on service, this gives us baseline activity in the JVM to start evaluating the flight recording.

### Basic test

This service under test, is a simple web service which provides results based on a search API. When interacting with the service, you can simply supply a HTTP GET on the _search_ resource with a query parameter ('q') with the term that you are searching for. It will then return KCDC's 2015 sessions that contain that term. Example:

```bash
curl "http://localhost:8080/search?q=jvm"
{
  "results" : [ {
    "title" : "Concurrency Options on the JVM",
    "presenter" : "Jessica Kerr",
    "sessionType" : "Regular Session"
  }, {
    "title" : "Exploring the Actor Model with Akka.NET",
    "presenter" : "Robert Macdonald Smith",
    "sessionType" : "Regular Session"
  }, {
    "title" : "What's in your JVM?",
    "presenter" : "Carl Chesser",
    "sessionType" : "4-Hour Workshop"
  } ]
}
```

### Using Apache Benchmark

We can utilize [Apache Benchmark](http://httpd.apache.org/docs/2.2/programs/ab.html) to generate traffic on the service From the console, we will execute the following to generate traffic against our service. Note, we will use a very basic search of just "a", since this will generate more results.

```bash
ab -n 1000 -c 15 "http://localhost:8080/search?q=a"
```

### Using loadtest

An alternative to Apache Benchmark, is [loadtest](https://github.com/alexfernandez/loadtest) (a node.js equivalent). To install:

```bash
sudo npm install -g loadtest
```

Then you can execute similarly:

```bash
loadtest -n 1000 -c 15 "http://localhost:8080/search?q=a"
```

## Stop Flight Recorder

After you have played traffic through the service, you can then stop your flight recording from JMC.

{{< figure src="img/jfr-stop.png" alt="JFR Stop" >}}

Then dump the whole recording.

{{< figure src="img/jfr-dump.png" alt="JFR Dump" >}}

## The Flight Recording

From a Java Flight Recording, there are several categories of information (pages) you can view from JMC:

* Code Information:
  * __Method Profiling__: Provides information about specific method runs and how long each run took, identifying _hot spots_ in your code base.
  * __Exceptions__: Displays Exceptions and Errors thrown and which methods threw them. Viewing exceptions requires editing the settings to also capture Exceptions.
* Thread information:
  * __Threads__: Provides a snapshot of all the threads that belong to the Java Application and the thread activity.
  * __Lock Instances__: Provides further details on threads by specifying lock information, showing which threads request and which threads take a particular lock.
  * __Thread Dump__: Provides period thread dump information.
* Memory Information:
  * __Memory__: Represents heap memory usage of the JVM. 
* IO Information:
  * __File I/O__: Displays File costs and behaviors
  * __Socket I/O__: Displays Network costs and behaviors
* JVM Internals:
  * __Garbage Collections__: Displays heap usage compared to pause times as well as GC events.
  * __Compilations__: Provides details on code compilation.
  * __TLAB Allocations__: Displays Thread Local Allocation Buffers.
* System Information:
  * __Processes__: See other processes on the system and what is competing for resources.
  * __Environment__: Provides information about the environment in which the recording was made.
  * __System Properties__: Show properties passed on to the JVM.
* Events:
  __Event Browser__: View detailed log of the events within the JVM. You can use this page to further filter down on events across all the pages in the recording.

{{% alert title="Note" color="info" %}}
Feel free to reference the additional [resources](/docs/jmc/_references/) as you navigate through the sections.
{{% /alert %}}

{{% nextSection %}}
In the next section, we'll find out more about what our methods are doing.
{{% /nextSection %}}
