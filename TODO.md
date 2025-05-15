# TODOs

## Update the use of Gatling

The current references to using gatling are pretty old. Need to update it so we can utilizes later versions.

### Using gatling

Alternatively, you can use [gatling](https://gatling.io/) (a performance library with a scala dsl ).

This should launch the `WorkshopSimulation`.

```bash
 mvn -f java-perf-workshop-tester/ gatling:test
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

