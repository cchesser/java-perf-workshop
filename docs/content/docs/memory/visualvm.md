---
title: "Visual VM"
weight: 14
description: Heap Analysis using VisualVM
---

### VisualVM

[VisualVM](http://docs.oracle.com/javase/8/docs/technotes/guides/visualvm/) is a tool which is part of Oracle's JDK (has been since Java 6). This tool was spawned from the [Netbeans Profiler](https://profiler.netbeans.org/), and it has some helpful tools to inspect a heap dump.

To load the heap dump in VisualVM, you will just go to __File__ -> __Load...__ and specify the
__File Format__ to be __Heap Dumps...__. Once it is loaded, a view will then be presented with
the following sections:

* Summary
* Classes
* Instances
* OQL Console

For the purpose of the workshop we are only going to utilize the __OQL Console__ to construct
queries on the heap. There are other options in VisualVM which can give you context about your 
heap, but I feel there are other better tools we will touch on later. The attractive feature
I like with VisualVM is that its __OQL Console__ allows you to write Javascript code to query
objects, which you can then do other functions on that data.

#### OQL Console

From the __OQL Console__, we will issue queries and then allow you to further play around with the language.

Query for Thread objects which are daemons (you can get context of Threads, since they
are objects in your heap too):

```js
select t from java.lang.Thread t where t.daemon == true
```

These basic results which are returned, are essentially hyperlinks, that you can then click
and begin diving into the instance state.

Query for Thread objects (similar to last query), but return multiple attributes as JSON:

```js
select {name: t.name.toString(), thread_id: t.tid }
from java.lang.Thread t
where t.daemon == true
```

Query which includes a Javascript function to filter results by looking at all the ConferenceSession
objects, and only report the ones which have _tags_.

```js
function hasTags(conf) {
  return conf.tags.size > 0
}

filter(heap.objects('cchesser.javaperf.workshop.data.ConferenceSession'), hasTags)
```

Take that previous query and further expand it to list it in JSON with the number of tags:

```js
function hasTags(conf) {
  return conf.tags.size > 0
}

map(filter(heap.objects('cchesser.javaperf.workshop.data.ConferenceSession'), hasTags), 
 '{ conference: it, tags: it.tags.size }')
```

We can further expand this to sort the results, descending by number of tags, and list the tags in the results:

```js
function hasTags(conf) {
  return conf.tags.size > 0
}

sort(map(filter(heap.objects('cchesser.javaperf.workshop.data.ConferenceSession'), hasTags), 
 '{ conference: it, tag_count: it.tags.size, tags: toArray(it.tags.elementData) }'), 'rhs.tag_count - lhs.tag_count')
```

Reference: [VisualVM OQL Help](https://htmlpreview.github.io/?https://raw.githubusercontent.com/visualvm/visualvm.java.net.backup/master/www/oqlhelp.html)

💡 Note, the OQL which you use between this tool (and others), isn't necessarily _standard_, and therefore cannot be used between tooling without issues. VisualVM gives you
the ability to leverage Javascript, which is unique to other tools supporting heap dump analysis.
