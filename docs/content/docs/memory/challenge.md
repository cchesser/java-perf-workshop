---
title: "Memory Challenge"
weight: 15
description: Put all the pieces together with this challenge to test your skills
---

The provided WorkshopService has an intentional memory problem, use the previous sections as reference to try to pin point it.

## Tips

* With the Dominator Tree (Eclipse MAT) determine what object would free up the most memory (if we could get rid of it)
* With the Histogram (Eclipse MAT) determine what type of object has the most instances
  * Using the Incoming Objects view, find out what's pointing to them.

Once you've found the large object, look through it's properties and determine why this object holds on to so many other objects.

{{% alert title="Solution" color="warning" %}}
This section contains a solution, make sure you've given things a shot before spoiling the fun.
{{% /alert %}}

{{% spoiler title="Show Solution" %}}
## Dominator Tree

A first glance at the dominator tree will point us to a `io.dropwizard.jetty.MutableServletContextHandler`, which is something controlled by the DropWizard framework. Let's dig through its references until we find some classes in the `cchesser` package.

![](/mem_challenge/dominator_tree_start.png)

The first instance of a class we "own" is `cchesser.javaperf.workshop.resources.WorkshopResource`

![](/mem_challenge/dominator_tree_resource.png)

Let's look at it's properties a bit, and we'll find a `cchesser.javaperf.workshop.cache.CleverCache` reference with about 70% of the memory consumption.

![](/mem_challenge/dominator_tree_clever_cache.png)

We'll look at its properties in a bit more detail in the following sections.

## Histogram View

A first glance at the Histogram View (sorted by retained heap), shows us about 2,000,000 instances of `cchesser.javaperf.workshop.data.Searcher$SearchResultElement`

![](/mem_challenge/histogram.png)

Looking through the references to an instance of this type, we end up being referenced by the `data` field in `cchesser.javaperf.workshop.cache.CleverCache$CacheEntry`

![](/mem_challenge/sre_incoming_refs.png)

We can further inspect the Incoming References for a particular entry, and see what is pointing to it, tracing it back to something we own:

![](/mem_challenge/cc_entry_incoming_refs.png)

Once again, we've ended up at `cchesser.javaperf.workshop.resources.WorkshopResource`, specifically a field called `resultsByContext`. 

We'll take a closer look at this `cchesser.javaperf.workshop.cache.CleverCache` object.

## Inspecting Clever Cache

First, lets determine how many instances of `cchesser.javaperf.workshop.cache.CleverCache` we have. We've found a single instance that is about 70Mb (in our sample dump), so let's double check if there's more of these.

We can do that in a couple of ways:

1. Use the histogram view to filter on `CleverCache`

![](/mem_challenge/mat_clevercache_instance.png)

2. Use OQL to find instances of that object:

```sql
select * from cchesser.javaperf.workshop.cache.CleverCache
```

![](/mem_challenge/mat_oql_clevercache.png)

### Fields/References

We can find the fields and references for an instance using either Eclipse MAT or VisualVM.

In Eclipse Mat, the fields for an instance display in an __Attributes__ tab

![](/mem_challenge/mat_cc_instance.png)


In VisualVM, we can load the __Objects__ view and apply a filter on class name to then inspect the fields and references

![](/mem_challenge/visualvm_cc_instance.png)


Looking at the properties, we find that there's a `cacheLimit` of `250`. Let's find out exactly how many entries are in the cache (the `innerCache` field).

## Finding Sizes

Let's write a query that will show us what the count of entries in the cache is. 

* For our query, we need to pull the `cacheLimit` and `size` of the `innerCache` map,

We can do this a few ways:

### Eclipse MAT / OQL

 we can do that with the following query:
```sql
SELECT c.cacheLimit AS "max entries", c.innerCache.size AS entries FROM cchesser.javaperf.workshop.cache.CleverCache c 
```

![](/mem_challenge/oql_size_report.png)


### Eclipse MAT / Calcite
```sql
select c.cacheLimit as "max entries", getSize(c.innerCache) as "entries" 
from cchesser.javaperf.workshop.cache.CleverCache c
```

![](/mem_challenge/calcite_size_report.png)


### VisualVM / OQL

```js
var caches = heap.objects("cchesser.javaperf.workshop.cache.CleverCache")

function report(cache) {
  return { maxEntries: cache.cacheLimit, entries: cache.innerCache.size }
}
 
map(caches, report)
```

![](/mem_challenge/visualvm_size_report.png)

{{% pageinfo color="warning" %}}
That doesn't seem right at all, we want at most 250 items but we have about 10,000. Let's find out why that is the case.
{{% /pageinfo %}}


## Memory Problem Explained

The interaction between the `WorkshopResource` and `CleverCache` happens through the `/search` resource.

_cchesser.javaperf.workshop.WorkshopResource_
```java
@GET
@Path("/search")
@Produces(MediaType.APPLICATION_JSON)
@Timed
public Searcher.SearchResult searchConference(@QueryParam("q") String term, @QueryParam("c") String context) {
    return fetchResults(term, context);
}

private Searcher.SearchResult fetchResults(String term, String context) {
  
  if (context != null && !context.isEmpty()) {
      if (resultsByContext.exists(context)) {
          return resultsByContext.fetch(context);
      }
  }

// does not exist in cache, compute and store
  
  SearchResult results = searcher.search(term);
  resultsByContext.store(results.getResultsContext(), results);
  return results;
}
```

If a context is sent, the previously cached values for the search are returned, otherwise we cache those result values in case they need to be referenced again.

_javaperf.workshop.cache.CleverCache_
```java
 /**
  * Stores the given key value pair in the cache
  *
  * @param key   the unique identifier associated with the given value
  * @param value the value mapped to the given key
  */
  public void store(K key, V value) {
    if (isFull()) {
        K keyToRemove = getLFUKey();
        CleverCache<K, V>.CacheEntry<V> removedEntry = innerCache.remove(keyToRemove);
    }

    CacheEntry<V> newEntry = new CacheEntry<V>();
    newEntry.data = value;
    newEntry.frequency = 0;

    innerCache.put(key, newEntry);
  }
  // other methods
  private boolean isFull() {
    return innerCache.size() == cacheLimit;
}
```

This interaction is **NOT** thread safe. Consider this scenario:

* Thread 1 invokes `store` when the `innerCache.size()` is `249`
  * `isFull()` evaluates to `false`
  * Execution halts prior to `innerCache.put`
* Thread 2 invokes `store` when the `innerCache.size()` is `249`
  * `isFull()` evaluates to false
  * Execution halts prior to `innerCache.put`
* Thread 1 resumes and executes `innerCache.put`, `innerCache.size()` is now `250`
* Thread 2 resumes and executes `innerCache.put`, `innerCache.size()` is now `251`
* For the lifecycle of the jvm `isFull()` will no longer return `true`

Fortunately, the DropWizard documentation calls out the need to consider thread safety:

![](/mem_challenge/dw_thread_safety.png)

Since the `WorkshopResource` has a `resultsByContext` field that will get shared across threads, that type should be thread safe. 

### Potential Solutions

* The naive solution would be to just change `isFull()` to consider `>=`, though that might lead to ConcurrentModificationExceptions
* The proper solution would be to make the class Thread Safe

{{% /spoiler %}}

&nbsp;
{{% nextSection %}}
In the next section, we'll continue learn about Garbage Collection logs.
{{% /nextSection %}}
