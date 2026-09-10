---
title: "Talks"
linkTitle: "Talks"
description: >
    Past talks shared on these topics.
toc_hide: true 
menu:
  main:
    weight: 30
    pre: "<i class=\"fas fa-microphone\"></i>"
---

*  [Heap Space Nine: Explore your Java Memory with AI](/slides/heap-space-nine/) (September 2026): This talk explores how AI and the Model Context Protocol (MCP) can make Java heap dump analysis faster and easier. It covers traditional memory-analysis tools, then shows how an MCP server can let an AI model examine object graphs, find memory hotspots, diagnose issues, and suggest optimizations.
  * Check out the code here: [java-perf-workshop](https://github.com/cchesser/java-perf-workshop)
*  [A Practical Guide to JVM Native Memory in Kubernetes](/slides/jvm-k8s-mem/) (May 2026): A lightning talk on how JVM native memory behaves in Kubernetes, why container limits can be surprising, and what to inspect when memory usage does not match heap settings.
   * [Hello World Example](/docs/containers/hello-world/): Tiny Hello World service example, with building it as a container, deploying on Kubernetes, and evaluated native memory utilization.
* [Open Up your JVM with Open Source Tooling](/slides/jvm-tooling/) (May 2025): A tour of open source JVM observability and troubleshooting tools, including JDK Mission Control, Eclipse Memory Analyzer, VisualVM, and OpenTelemetry.
