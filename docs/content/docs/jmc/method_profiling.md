---
title: "Method Profiling"
weight: 11
description: >
    Gather context of where we spend time in our code
---

Our first look at the recording will start with finding out where we might be spending time with our code. 

### Method Profiling Page

We'll start by taking a look at the detailed __Method Profiling Page__. This page displays how often a specific method is ran based on sampling. 

{{% alert title="Note" color="info" %}}
This page is a little bit difficult to use in JMC7, planned improvements for the data visualization will come in JMC8, see [JMC#165](https://github.com/openjdk/jmc/pull/165)
{{% /alert %}}

![](/jmc/method_profile_page.png)

By default, this view groups threads by method, but you can also include the line number for each method. This can be accomplished by selecting the __View Menu__ option and  going to __Distinguish Frames By__ and selecting __Line Number__.

![](/jmc/method_profile_distinguish_line_numbers.png)

💡 Generally, you don't need this, as it can be quite apparent by the base method being invoked where the cost is at. Though, it may be helpful to include in some contexts.

![](/jmc/method_profile_with_line_numbers.png)

{{% alert title="Note" color="info" %}}
Remember that this is just a sampling view, and does not mean that only a small amount of methods were hit. We'll be able to visualize more of this info in the following part.
{{% /alert %}}

## Java Application

Navigate to the __Java Application__ page. This page provides an overview of the state of the JVM and collates information from a couple of the dedicated pages.

![](/jmc/java_application_page.png)

Using the checkboxes on the right of the Graph, select only __Method Profiling__ to narrow down the set of events on the graph:

![](/jmc/java_application_method_profiling.png)

The __Stack Trace__ view from this page can give you a better count of sampled methods (not just the ones from a specific Method Profiling event)

![](/jmc/java_application_stack_trace.png)

This information is the equivalent of the __Hot Methods__ tab in JMC 5, see [forum discussion](https://community.oracle.com/tech/developers/discussion/4094447/difference-between-method-profiling-and-java-application-stack-traces).

We can then use this information to correlate what is happening in our code:

![](/jmc/hot_method_line_number.png)

## <i class="fas fa-compass"></i> Explore

* Walk around to look at other areas where you are spending time in your code. 
  * In many cases you find that there are very expensive areas of code that you cannot change (as you may not own it), but you can dictate whether or not it should be executed (or executed as frequently).

### <i class="fas fa-question"></i> Follow Ups

*  Is there any method that stands out to you that might be unnecessarily called?

{{% nextSection %}}
In the next section, we'll look at the Exceptions thrown by our application.
{{% /nextSection %}}
