---
title: "Threads"
weight: 13
description: >
    Find the computation and latency events in your application.
---

The __Threads__ page provides a snaphost of all threads in our application, and we can use it to acquire information about computation and latency events.

![](/jmc/threads_page.png)

### Thread Chart

We can use the Thread Chart view to select a region of time and view the state of threads and the most common stack frames for that particular time slice:

![](/jmc/thread_chart.png)

We can also edit the types of events shown by the chart. Right click on the chart, and select __Edit Thread Activity Lanes__

![](/jmc/edit_thread_lanes.png)

We can then narrow down on specific events.

For the purpose of this section filter on only the __Sleep__ events. Then, select a period of time and look at what has caused our threads to go to sleep for that particular period:

![](/jmc/filter_sleeping_threads.png)

![](/jmc/sleeping_threads.png)

Once you're done with the filtered view, right click on the __Thread__ page and Reset the page (to return to the default state).

![](/jmc/reset_thread_page.png)

### By Thread

We can select an invididual thread from the __Thread Table__ to further filter the data just for that thread. From this filtered view, we can inspect what the commonly executed methods are for a particular thread. 

![](/jmc/thread_selected.png)

We can then use the stacktrace view to correlate what piece of code executed the most.

![](/jmc/thread_selected_stack_trace.png)

## <i class="fas fa-compass"></i> Explore

There's a couple of threads specifically created by our application, these are the `wkshp-conf-loader` named threads. The `dw-*` named threads are threads created by DropWizard to handle specific requests (you might have noticed this based on the name containing a REST Resource). Explore the state of some of these threads while thinking about the follow up questions.

### <i class="fas fa-question"></i> Follow Ups

* Where are we spending most of our time sleeping? What is causing this?
* Are there any unnecessary method invocations? Are any threads doing work that isn't visible?

In the next section, we'll look at the [Memory](/docs/jmc/memory) state of the application.