---
title: "Visual VM"
weight: 14
description: Heap Analysis using VisualVM
---

{{% alert title="Note" color="info" %}}
Make sure you've gone through the [Prerequisites](/docs/prereqs)
{{% /alert %}}

[VisualVM](http://docs.oracle.com/javase/8/docs/technotes/guides/visualvm/) is a tool which used to be part of Oracle's JDK and is now a [standalone tool](https://visualvm.github.io/). This tool was spawned from the [Netbeans Profiler](https://profiler.netbeans.org/), and it has some helpful tools to inspect a heap dump.

## Opening Dumps

To load the heap dump in VisualVM, you will just go to __File__ -> __Load...__ and specify the
__File Format__ to be __Heap Dumps...__. 

![](/visualvm/load_dump.png)

## Analyzing the Heap Dump

VisualVM offers the following sections:

* Summary
* Objects
* Threads
* OQL Console
* R Console

For the purpose of this section we are only going to utilize the __OQL Console__ to construct
queries on the heap. There are other options in VisualVM (briefly discussed here), but we feel that the other tools presented before this offer much nicer views/data. 

### Summary

The __Summary__ view gives you a quick overview of the state of the heap:

![](/visualvm/summary.png)

This view also lets you quickly gather the dominators for the heap, similar to the Dominator Tree in Eclipse MAT.

![](/visualvm/dominators.png)

### Objects

The __Objects__ view groups instances by class. 

![](/visualvm/objects.png)

Within this view, you can inspect individual instances of an object and see both the fields and the incoming references to that instance:

![](/visualvm/objects_expanded.png)

### Threads

The __Threads__ view visualizes the state of threads.

![](/visualvm/threads.png)

Within this view, you can drill down into certain threads and view what was specifically referenced in a stack frame:

![](/visualvm/threads_expanded.png)


### OQL Console

One of the attractive features for VisualVM is that its __OQL Console__ allows you to write Javascript code to query objects, which you can then do other functions on that data.

From the __OQL Console__, we will issue queries and then allow you to further play around with the language.

## OQL Queries

{{% alert title="Note" color="info" %}}
The OQL which you use between this tool (and others), isn't necessarily _standard_, and therefore cannot be used between tooling without issues. VisualVM gives you
the ability to leverage Javascript, which is unique to other tools supporting heap dump analysis.
{{% /alert %}}

Reference: [VisualVM OQL Help](https://htmlpreview.github.io/?https://raw.githubusercontent.com/visualvm/visualvm.java.net.backup/master/www/oqlhelp.html)

### Deamon Threads

Query for Thread objects which are daemons (you can get context of Threads, since they
are objects in your heap too):

```js
select t from java.lang.Thread t where t.daemon == true
```

Previous versions of VisualVM used to return the results as hyperlinks, which you could then click
and begin diving into the instance state:

![](/visualvm/oql_sample_results_links.png)

The newer versions of VisualVM will populate the results in a view similar to the __Objects__ view:

![](/visualvm/oql_sample_results.png)

### Mapping to JSON

Query for Thread objects (similar to last query), but return multiple attributes as JSON:

```js
select {name: t.name.toString(), thread_id: t.tid }
from java.lang.Thread t
where t.daemon == true
```

![](/visualvm/sample_json_results.png)

### Applying Javascript

VisualVM offers a helper object that represents the heap, the `heap` object, which offers a few useful functions.

![](/visualvm/oql_heap_object.png)


During our OQL searching, we'll be using `heap.objects` to gather the set of objects we want.

We'll start by limiting our objects to instances of `cchesser.javaperf.workshop.data.ConferenceSession` and assign it to a variable called `sessions` (which we'll reference later).

```js
var sessions = heap.objects('cchesser.javaperf.workshop.data.ConferenceSession');
```

We're going to find ConferenceSessions that which have _tags_, we'll do that by writing a Javascript function to filter results which have _tags_. We'll apply this filter to our `sessions`.

```js
var sessions = heap.objects('cchesser.javaperf.workshop.data.ConferenceSession');

function hasTags(conf) {
  return conf.tags.size > 0
}

filter(sessions, hasTags);
```

![](/visualvm/oql_hasTags.png)


Take that previous query and further expand it to list it in JSON with the number of tags. It is often times convenient to have a reference to the object we're interacting with, to more easily see the available fields.

```js
var sessions = heap.objects('cchesser.javaperf.workshop.data.ConferenceSession');

function hasTags(conf) {
  return conf.tags.size > 0
}

var withTags = filter(sessions, hasTags);

function toJSON(conf) {
return { conference: conf , tagCount : conf.tags.size };
}

map(withTags, toJSON);
```

![](/visualvm/oql_hasTags_json.png)


We can further expand this to sort the results, descending by number of tags, and list the tags in the results:

```js
var sessions = heap.objects('cchesser.javaperf.workshop.data.ConferenceSession');

function hasTags(conf) {
  return conf.tags.size > 0
}

var withTags = filter(sessions, hasTags);

function toJSON(conf) {
  return { conference: conf , tagCount : conf.tags.size };
}

var confs = map(withTags, toJSON);

function tagSorter(lhs, rhs) {
  return rhs.tagCount - lhs.tagCount;
}

sort(confs, tagSorter);
```

![](/visualvm/oql_tags_sorted.png)

We could also inline most of this using the inline syntax:

```js
function hasTags(conf) {
  return conf.tags.size > 0
}

sort(map(filter(heap.objects('cchesser.javaperf.workshop.data.ConferenceSession'), hasTags), 
 '{ conference: it, tag_count: it.tags.size, tags: toArray(it.tags.elementData) }'), 'rhs.tag_count - lhs.tag_count')
```

![](/visualvm/oql_tags_inline.png)

## Useful Plugins

### OQL Syntax Support

This plugin adds some auto complete and syntax highlighting to the OQL Console

![](/visualvm/oql_syntax_plugin.png)

## <i class="fas fa-atlas"></i> Challenge

Apply what you've learned about OQL in this section and write some javascript that will show the distribution of tag counts in our conference sessions (similar to what we did with calcite mat.)

### Sample Solutions

#### JSON Output

This solution will return the result as a JSON object.

{{% spoiler title="Show JSON" %}}

```js
var sessions = heap.objects('cchesser.javaperf.workshop.data.ConferenceSession');

function toJSON(conf) {
  return { conference: conf , tagCount : conf.tags ? conf.tags.size : 0, title: conf.title };
}

var confs = map(sessions, toJSON);

var results = {}
var arr = toArray(confs)
arr.length;

for(var i = 0; i < arr.length; i++) {
  var conf = arr[i];
  if(!results[conf.tagCount]) { 
    results[conf.tagCount] = 0;
  }
  results[conf.tagCount] = results[conf.tagCount] + 1;
}
results
```

![](/visualvm/tag_distributions_json.png)

{{% /spoiler %}}

#### HTML Output

This solution will return the result as an HTML `<ul>`.


{{% spoiler title="Show HTML" %}}

```js
var sessions = heap.objects('cchesser.javaperf.workshop.data.ConferenceSession');

function toJSON(conf) {
  return { conference: conf , tagCount : conf.tags ? conf.tags.size : 0, title: conf.title };
}

var confs = map(sessions, toJSON);
var arr = toArray(confs)

var maxSize = max(map(arr, 'it.tagCount'))
var results = []
for(var i = 0; i < maxSize+1; i++ ) {
  results[i] = 0;
}

for(var i = 0; i < arr.length; i++) {
  var conf = arr[i];
  results[conf.tagCount]++
}

var report = "<html><ul>";

for(var i = 0; i < maxSize+1; i++) {
  var count = results[i];
  report += "<li><b>Size(" +i+")</b>="+count+"</li>" 
}
report+="</ul></html>"
toHtml(report)
```

![](/visualvm/tag_distributions_html.png)

{{% /spoiler %}}
