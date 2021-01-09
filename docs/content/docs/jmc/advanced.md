---
title: "Advanced Flight Recorder"
weight: 20
description: Take the next step in learning more with Flight Recorder
---

In this workshop we are going to expand what we profiled earlier to all the different areas of the JVM. The JFR has an extensive amount of data which it captures, we will not dive into each of these areas, but hit on a couple more common areas to inspect on your code base.

## Exceptions

Go into the __Code__ tab, and then navigate to the __Exceptions__ sub tab. Here you can get context of what exceptions are thrown, and when. Exceptions are not necessarily a cheap operation, as there are costs in producing the stack trace.

Within this view, there are three sub tabs:
* Overview: Get counts on exceptions/errors and related stack traces
* Exceptions: Get individual instances of exceptions thrown based on the timeline. Includes string messages which may provide additional context.
* Errors: Same content as the Exceptions view, but for Errors.

Overview:

![JMC Exceptions](https://github.com/cchesser/java-perf-workshop/wiki/images/jmc_exceptions_overview.png)

Exceptions sub view (filtered down to a specific time interval):

![JMC Exceptions](https://github.com/cchesser/java-perf-workshop/wiki/images/jmc_exceptions_exception_subtab.png)

### Follow-ups

* What are the exceptions that are getting generated the most? 
* What stack traces are the contributor?
* Select different intervals of time to filter the listing of what is being presented. Can you see where 

## Threads

In the __Threads__ tab, you can get context to compute and latency events tied to threads. For this we will look a couple of things:
* Hot Threads
* Latencies

The __Hot Thread__ sub tab view, you can view threads and the related methods that are consuming the most time. As with other views, you can zoom in on spikes of events to see if there is a common effect. Many times this gives you a better context of related resources (by threads of the same pool) that could be getting heavily utilized by a request. This also helps in correlating other facts you may have (like native threads / thread dumps).

![JMC Hot Threads](https://github.com/cchesser/java-perf-workshop/wiki/images/jmc_thread_hot_threads.png)

The __Latencies__ sub tab can help show common areas of your code (organized by thread states) which can cause slowness in your code base that may not be directly tied to a heavy computation task (ex. waiting on a network call). Generally it helps to look at the large contributor (based on total time consumed) and see when and why it is getting invoked (and if that can be minimized).

![JMC Latencies](https://github.com/cchesser/java-perf-workshop/wiki/images/jmc_thread_latencies.png)

### Follow-ups

* Where are we spending most of our time sleeping? What is causing this?

## I/O

The __I/O__ tab, can provide context of what external resources you are utilizing from you code base, and relate it to other latency events. For the purpose of our workshop service, we are doing network I/O, and therefore will look at the __Socket Read__ sub view. From this view you can gain context of what host you are calling, how many times you are calling it, and how much time it is taking.

![JMC IO Socket Read](https://github.com/cchesser/java-perf-workshop/wiki/images/jmc_io_socket_read.png)


### Follow-ups

* What remote services are we interacting with?
* How long are we interacting with them and how many invocations?
* How many times did we make calls and what threads were tied to those calls?