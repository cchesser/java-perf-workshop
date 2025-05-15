---
title: "JOverflow"
weight: 12
description: Heap Analysis using JOverflow
---

Included in [JDK Mission Control](https://www.oracle.com/java/technologies/jdk-mission-control.html) is a plugin that used to be a separate instance, the JOverflow Heap Analyzer. This plugin now adds a context menu selection to __Dump Heap__ of JVMs from the JVM Browser.

![joverflow dump heap](/joverflow/dump_heap.png)

**JOverflow** is a great tool to start our analysis with since it is tailored to identifying common anti-patterns present in most heap dumps. This should give us an idea of the kinds of problems we can see in our JVMs.

![](/joverflow/main_page.png)

## Anti-Patterns

Here is a listing of some the patterns that JOverflow identifies:

* __Empty Unused Collections__: Empty collection, where modification `count == 0`.
* __Empty Used Collections__: Empty collection, where modification `count != 0`.
* __Small Sparse__: Only for array-based, where less than half the slots are occupied. 
Small is considered: size <= default (ex. 16 for HashMap).
* __Large Sparse__: Only for array-based, where less than half the slots are occupied. 
Large is considered: Size > default.
* __Boxed__: Contains boxed Numbers (ex. java.lang.Integer). Each of these boxed
number has overhead as compared to their primitive counterpart due to object references.
* __Small Collections__: Collections with 1 to 4 elements. There are fixed costs of collections, which may lend this
set of data better hosted in an array vs. a full Java collection type.
* __Vertical Bar Collections__: Collection which is a list of lists, where the outer collection
is large, and it's elements are all small collections (ex. List(1000) of List(100))
* __Zero Size Arrays__: Array where length == 0 (still consumers 12 - 16 bytes).
* __Vertical Bar Arrays__: Similar to _Vertical Bar Collections_, but for arrays.
* __Sparse Arrays__: Less than half of the elements are not null.
* __Long Zero Tail Arrays__: Ends with a consecutive series of zeros, where the tail length is >= size / 2.
* __Empty Arrays__: Only null elements.
* __Duplicate Arrays__: Where an array contents are the same in another instance, but they are separate array instances.
* __Duplicate Strings__: Same as _Duplicate Array_, where `string1.equals(string2)` and `string1 != string2`.

Reference: [JOverflow: Advanced Heap Analysis in JDK Mission Control](https://www.youtube.com/watch?v=b-mv9iWY8kw)

## Filtering

As you play with JOverflow, once you click on something, it will begin filtering down your content. If you wish to reduce down to code that you may own, you can start doing a package filter on the ancestor section. 

In this example, I want to see duplicate strings for code under `cchesser`. In this case, I can see that our workshop service has several duplicate
strings (as this content is replicated on each object):
* Regular Session
* 4-Hour Workshop
* 8-Hour Workshop

However, the number of instances / overhead is extremely low (since this is a small service), it gives you visibility of areas that you could optimize.

![joverflow filter](/joverflow/duplicate_strings.png)

### Navigating

Since JOverflow's mechanism of drilling down is by applying filters, it doesn't have an easy way to undo most operations, and instead relies on resetting the state to the unfiltered state. You can undo all operations by using the backwards arrow button:

![joverflow reset](/joverflow/reset.png)

## Instances

We can also use the plugin to drill down a bit into particular instances. 

We can enable the instances view by navigating to:

1. Window > Show View > Other 
![](/joverflow/window_showview_other.png)
2. Selecting JOverflow Instances
![](/joverflow/show_instances.png)


### Example - Arrays Size 1

In the following example, we're going to find:

1. Arrays with One Element
![](/joverflow/arrays_one_element.png)
2. Coming from our WorkshopConfiguration
![](/joverflow/referrer_workshop_configuration.png)

This should filter the Instances view down to that single object.

![](/joverflow/config_instance.png)

### Example - Conference Session Details

We can use this same mechanism to also explore values of some of the classes we own, in the next example we're going to:

1. Use All Objects of type Conference Session
![](/joverflow/conference_sessions.png)
2. Drill down into a couple of the instances
![](/joverflow/conference_session_instances.png)

&nbsp;
{{% nextSection %}}
In the next section, we'll continue our analysis in Eclipse MAT.
{{% /nextSection %}}