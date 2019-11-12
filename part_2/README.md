# Part 2: Java Mission Control & Flight Recorder

In this workshop, we will be getting more familiar with using Java Mission Control and the Flight Recorder. As we have been running our service, we want to gather more insight of the code in the service by running series of simple tests and measuring the service.


## Java Flight Recorder

The Java Flight Recorder (JFR) is a very low overhead profiling and diagnostics tool. It was inherited from the JRockit JVM, and it is now offered as part of the HotSpot JVM. It is designed to be "black box" data recorder of the the run-time, which can be used in production environments, making it an attractive tool for profiling code since it has low overhead on the JVM.

To enable the Flight Recorder on the JVM, the following options need to be included on the JVM:

```bash
-XX:+UnlockCommercialFeatures -XX:+FlightRecorder
```

:heavy_exclamation_mark: Notice that you have to include `-XX:+UnlockCommercialFeatures` first in the options listing. This is because using Flight Recorder is a feature that requires additional licensing when used in production environments. One of the following licenses is required in order to utilize this in a production environment.

* Oracle Java SE Advanced
* Oracle Java SE Suite

However, you may use this in non-production environments without additional licensing.


### Higher Fidelity on Method Profiling

To get better fidelity on method profiling, include the following options which will enable the compiler to include additional metadata on non-safe code points. This is helpful, as sometimes the metadata will not fully resolve to the correct line in the code.

```bash
-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints
```

## Java Mission Control
We will be using Java Mission Control _(included in Oracle JDK 7u40)_ to monitor and evaluate the Java flight recordings. To start up  Java Mission Control, simply executing the following in your console:

```bash
jmc
```

:bulb: In order to be able to invoke `jmc` (Java Mission Control) from your console, it assumes `$JAVA_HOME/bin` is on your `$PATH`. If it is not included, go ahead and update your profile to include this so you can easily invoke `jmc` from your terminal.

## Start Service with JFR

Let's start profiling our service. Start the service up by enabling JFR:

### Start from the console

```bash
# Note, if you are running this server from a different folder, consider changing the SERVER_HOME
SERVER_HOME=java-perf-workshop-server/target
java -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints -jar $SERVER_HOME/java-perf-workshop-server-1.1.0-SNAPSHOT.jar server server.yml
```

:bulb: Starting with Java Mission Control 5.5 _(included in Oracle Java 8u40)_, you no longer have to enable JFR prior to capturing the recording (it will dynamically enable it, after prompting about it).


## Start Flight Recording from JMC

From Java Mission Control (JMC), you can start a flight recording by right-clicking the JVM from the __JVM Browser__ view and selecting __Start Flight Recording...__

![jmc_start](https://github.com/cchesser/java-perf-workshop/wiki/images/jmc_start_flight_recorder.png)

This will open a window where you apply some settings for the recording. First select that you want this to be a __Continuous recording__ and for Event settings, we will import a template to get some consistent settings for profiling. Within the __Template Manager__, select __Import Files...__ and import the `profile.jfc` included in this folder. It should appear as _Java Performance Workshop Profile_. Select this as the __Event Settings__ and then click on __Finish__.

![jmc_started](https://github.com/cchesser/java-perf-workshop/wiki/images/jmc_start_flight_recorder_wizard.png)

Once your flight recording is being captured in a _Continuous_ recording, it will show a ∞.

![jmc_started](https://github.com/cchesser/java-perf-workshop/wiki/images/jmc_flight_recorder_started.png)

:bulb: You can see the JFR templates (continuous / profile) which are shipped as part of the JRE in: `$JAVA_HOME/jre/lib/jfr`. These become helpful if you are wanting to compare your settings to some of the standard ones.

## Generate HTTP traffic on service

We will want to generate some traffic on the service to measure some general characteristics of the service:
* Throughput (requests per second)
* Response time
* Trend of response time over time

By generating traffic on service, this gives us baseline activity in the JVM to start evaluating the flight recording. For the purpose of this workshop, we will utilize [Apache Benchmark](http://httpd.apache.org/docs/2.2/programs/ab.html) to generate traffic on the service.

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

From the console, we will execute the following to generate traffic against our service. Note, we will use a very basic search of just "a", since this will generate more results.

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

### Using gatling

Alternatively, you can use [gatling](https://gatling.io/) (a performance library with a scala dsl ).

This should launch the `WorkshopSimulation`.

```bash
 mvn -f java-perf-workshop-tester/ gatling:test
```

```bash
mvn gatling:test
```

Sample output while running:
```bash
[~/java-perf-workshop/java-perf-workshop-tester]$ mvn gatling:test
[INFO] Scanning for projects...
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building java-perf-workshop-tester 1.1.0-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] --- gatling-maven-plugin:2.2.4:test (default-cli) @ java-perf-workshop-tester ---
19:12:16,662 |-INFO in ch.qos.logback.classic.LoggerContext[default] - Could NOT find resource [logback-test.xml]
19:12:16,663 |-INFO in ch.qos.logback.classic.LoggerContext[default] - Could NOT find resource [logback.groovy]
19:12:16,663 |-INFO in ch.qos.logback.classic.LoggerContext[default] - Found resource [logback.xml] at [file:/J:/Workspaces/java-perf-workshop/java-perf-workshop-tester/target/test-classes/logback.xml]
19:12:16,663 |-WARN in ch.qos.logback.classic.LoggerContext[default] - Resource [logback.xml] occurs multiple times on the classpath.
19:12:16,663 |-WARN in ch.qos.logback.classic.LoggerContext[default] - Resource [logback.xml] occurs at [file:/J:/Workspaces/java-perf-workshop/java-perf-workshop-tester/target/test-classes/logback.xml]
19:12:16,663 |-WARN in ch.qos.logback.classic.LoggerContext[default] - Resource [logback.xml] occurs at [jar:file:/C:/Users/JMonterrubio/.m2/repository/io/gatling/gatling-maven-plugin/2.2.4/gatling-maven-plugin-2.2.4.jar!/logback.xml]
19:12:16,727 |-INFO in ch.qos.logback.classic.joran.action.ConfigurationAction - debug attribute not set
19:12:16,731 |-INFO in ch.qos.logback.core.joran.action.AppenderAction - About to instantiate appender of type [ch.qos.logback.core.ConsoleAppender]
19:12:16,737 |-INFO in ch.qos.logback.core.joran.action.AppenderAction - Naming appender as [CONSOLE]
19:12:16,742 |-INFO in ch.qos.logback.core.joran.action.NestedComplexPropertyIA - Assuming default type [ch.qos.logback.classic.encoder.PatternLayoutEncoder] for [encoder] property
19:12:16,781 |-INFO in ch.qos.logback.classic.joran.action.RootLoggerAction - Setting level of ROOT logger to WARN
19:12:16,782 |-INFO in ch.qos.logback.core.joran.action.AppenderRefAction - Attaching appender named [CONSOLE] to Logger[ROOT]
19:12:16,782 |-INFO in ch.qos.logback.classic.joran.action.ConfigurationAction - End of configuration.
19:12:16,783 |-INFO in ch.qos.logback.classic.joran.JoranConfigurator@7a0ac6e3 - Registering current configuration as safe fallback point

Simulation cchesser.javaperf.workshop.WorkshopSimulation started...
```

## Stop Flight Recorder

After you have played traffic through the service, you can then stop your flight recording from JMC.

![jmc_stop](https://github.com/cchesser/java-perf-workshop/wiki/images/jmc_flight_recorder_stop.png)

Then dump the whole recording.

![jmc_dump](https://github.com/cchesser/java-perf-workshop/wiki/images/jmc_flight_recorder_dump.png)

## The Flight Recording

From a Java Flight Recording, there are several categories of information you can view from JMC:

* __Memory__: View memory utilization and garbage collection costs.
* __Code__: View profiling of your code to identify _hot spots_ in your code base. Also, you can get additional insight to exceptions thrown, compilation costs, and class loading.
* __Threads__: View series of thread dumps, _hot_ threads, latency events, and locking situations causing contention.
* __I/O__: View general I/O (file / network) costs that were occurring within your code.
* __System__: Get a general context of the run-time environment of the JVM.
* __Events__: View a full detailed log of the events within the JVM.

### Code

Our first walkthrough of the Flight Recording will begin with the __Code__ view to give context of where we might be spending time with our code.

When you first open the __Code__ tab, you will be brought to the __Overview__ sub tab. This will give you a breakdown of where you are spending your time by code package and classes.

![JMC code overview](https://github.com/cchesser/java-perf-workshop/wiki/images/jmc_code_overview.png)

To go deeper into these details, go into the __Hot Methods__ sub tab. From here you can expand down to the method which is consuming most of your time.

![JMC code hot methods](https://github.com/cchesser/java-perf-workshop/wiki/images/jmc_code_hot_methods.png)

You can also zoom into the timeline to scope it to a spike of events. This can be done on most view in Java Mission Control. In this case, we will zoom into a 847 ms interval:

![JMC code hot methods](https://github.com/cchesser/java-perf-workshop/wiki/images/jmc_code_hot_methods_scoped.png)

Another helpful means of identify within a method is including the line number. This can be accomplished by right-clicking in the view and going to __Distinguish Frames By__, and then selecting __Line Number__.

:bulb: Generally, you don't need this, as it can be quite apparent by the base method being invoked where the cost is at. Though, it may be helpful to include in some contexts.

![JMC code hot methods](https://github.com/cchesser/java-perf-workshop/wiki/images/jmc_code_hot_methods_line_number.png)

![JMC code hot methods](https://github.com/cchesser/java-perf-workshop/wiki/images/jmc_code_hot_methods_with_line.png)

Walk around to look at other areas where you are spending time in your code. In many cases you find that there are very expensive areas of code that you cannot change (as you may not own it), but you can dictate whether or not it should be executed (or executed as frequently.

We will go into more of the different areas of the flight recording in the next lab.
