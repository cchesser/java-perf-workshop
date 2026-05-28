## Title

A Practical Guide to JVM Native Memory in Kubernetes

## Description

Most JVM tuning stops at the heap, but in Kubernetes, that's often why you're overprovisioning memory and still getting OOMKilled. Native memory (metaspace, threads, direct buffers, and more) quietly consumes a significant portion of your container limits, leading teams to oversize requests "just to be safe" and pay for it.

In this lightning talk, we'll break down where JVM memory really goes and show how modern Java runtimes, especially Java 17 and newer, are better at reading Kubernetes container limits. We'll connect Kubernetes memory requests and limits to cgroup enforcement, then use built-in tools like `-XshowSettings:system`, `jcmd VM.info`, Native Memory Tracking (NMT), and Java Flight Recorder (JFR) to connect JVM memory to container memory in minutes. You'll leave with a practical loop for right-sizing heap and native headroom, validating container awareness, and reducing wasted memory spend without waiting for an OOMKill to teach the lesson.
