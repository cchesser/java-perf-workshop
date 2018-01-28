# Part 5: Garbage Collections

In this workshop, we are going to do some analysis on the JVM in regards to it's garbage collection (GC)
cycles.

## Prerequisites

* [R environment](https://www.r-project.org/) _(If you want to try parsing some of the logs)_

## Garbage Collection Logs

A key piece to understanding what is happening in terms of GC cycles within your service, 
is enabling GC logs. In this section we will go over the JVM options you can enable on your
service and how to interpret those logs.

### JVM Options

* `-XX:+PrintGCDetails`: Includes more details within your GC log
* `-XX:+PrintGCDateStamps`: Have a readable date/time string to correlate events in your log. Without
this option, your GC log will have elapsed time since the JVM was started. This format (which is reported)
in seconds (with millisecond precision), is not easy for someone to quickly correlate when this event was 
logged (as you have to infer the time based on when the JVM was started).
* `-Xloggc`: Specifies the log file for the garbage collection logs (otherwise will go to stdout). 
:bulb: Note: `-verbose:gc` is NOT necessary when you set `Xloggc` (it is implied).
* `-XX:+UseGCLogFileRotation`: Support rotating your GC log file (you don't want to let this get too
big).
* `-XX:GCLogFileSize`: Size of the file for rotation (ex. `10M`).
* `-XX:NumberOfGCLogFiles`: Number of GC log files to maintain (ex. `3`). :bulb: Note: if you are monitoring
your log file with another solution (like [splunk](http://www.splunk.com/) or 
[logstash](https://www.elastic.co/products/logstash)), you typically don't need to be keeping an inventory of 
rolled files around, unless you are concerned about log forwarding failing and want to ensure a given amount 
is still capable of being captured from a host.

### GC Log formats

With different garbage collectors in the JVM, you will get slightly different GC log formats.

#### Parralel GC

```
2015-09-30T10:57:20.215+0600: 0.847: [GC (Allocation Failure) [PSYoungGen: 65536K->10748K(76288K)] 65536K->12607K(251392K), 0.0118637 secs] [Times: user=0.03 sys=0.01, real=0.01 secs]
2015-09-30T10:57:20.556+0600: 1.188: [GC (Metadata GC Threshold) [PSYoungGen: 44312K->10748K(141824K)] 46171K->12786K(316928K), 0.0077755 secs] [Times: user=0.03 sys=0.00, real=0.01 secs]
2015-09-30T10:57:20.564+0600: 1.196: [Full GC (Metadata GC Threshold) [PSYoungGen: 10748K->0K(141824K)] [ParOldGen: 2038K->10444K(116736K)] 12786K->10444K(258560K), [Metaspace: 20976K->20976K(1067008K)], 0.0286381 secs] [Times: user=0.14 sys=0.01, real=0.03 secs]
```

#### CMS

```
2015-09-30T11:11:35.994+0600: 0.838: [GC (Allocation Failure) 0.838: [ParNew: 69952K->8703K(78656K), 0.0128204 secs] 69952K->12781K(253440K), 0.0128848 secs] [Times: user=0.04 sys=0.01, real=0.01 secs]
2015-09-30T11:11:38.009+0600: 2.853: [GC (CMS Initial Mark) [1 CMS-initial-mark: 4077K(174784K)] 67493K(253440K), 0.0088311 secs] [Times: user=0.04 sys=0.00, real=0.01 secs]
2015-09-30T11:11:38.018+0600: 2.862: [CMS-concurrent-mark-start]
2015-09-30T11:11:38.018+0600: 2.862: [CMS-concurrent-mark: 0.000/0.000 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
2015-09-30T11:11:38.018+0600: 2.862: [CMS-concurrent-preclean-start]
2015-09-30T11:11:38.019+0600: 2.863: [CMS-concurrent-preclean: 0.001/0.001 secs] [Times: user=0.01 sys=0.00, real=0.00 secs]
2015-09-30T11:11:38.019+0600: 2.863: [CMS-concurrent-abortable-preclean-start]
 CMS: abort preclean due to time 2015-09-30T11:11:43.074+0600: 7.918: [CMS-concurrent-abortable-preclean: 1.233/5.055 secs] [Times: user=1.23 sys=0.01, real=5.06 secs]
2015-09-30T11:11:43.074+0600: 7.918: [GC (CMS Final Remark) [YG occupancy: 63415 K (78656 K)]7.918: [Rescan (parallel) , 0.0052614 secs]7.924: [weak refs processing, 0.0000337 secs]7.924: [class unloading, 0.0029068 secs]7.927: [scrub symbol table, 0.0025781 secs]7.929: [scrub string table, 0.0002699 secs][1 CMS-remark: 4077K(174784K)] 67493K(253440K), 0.0117740 secs] [Times: user=0.04 sys=0.01, real=0.01 secs]
2015-09-30T11:11:43.086+0600: 7.930: [CMS-concurrent-sweep-start]
2015-09-30T11:11:43.086+0600: 7.930: [CMS-concurrent-sweep: 0.000/0.000 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]
2015-09-30T11:11:43.087+0600: 7.930: [CMS-concurrent-reset-start]
2015-09-30T11:11:43.110+0600: 7.954: [CMS-concurrent-reset: 0.023/0.023 secs] [Times: user=0.01 sys=0.01, real=0.03 secs]
```

#### G1

```
015-09-30T11:13:03.870+0600: 0.395: [GC pause (G1 Evacuation Pause) (young), 0.0052206 secs]
   [Parallel Time: 1.9 ms, GC Workers: 8]
      [GC Worker Start (ms): Min: 395.4, Avg: 395.5, Max: 395.8, Diff: 0.3]
      [Ext Root Scanning (ms): Min: 0.0, Avg: 0.3, Max: 1.1, Diff: 1.1, Sum: 2.0]
      [Update RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]
         [Processed Buffers: Min: 0, Avg: 0.0, Max: 0, Diff: 0, Sum: 0]
      [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]
      [Code Root Scanning (ms): Min: 0.0, Avg: 0.1, Max: 0.4, Diff: 0.4, Sum: 0.5]
      [Object Copy (ms): Min: 0.7, Avg: 1.4, Max: 1.6, Diff: 0.9, Sum: 11.4]
      [Termination (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]
      [GC Worker Other (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]
      [GC Worker Total (ms): Min: 1.5, Avg: 1.8, Max: 1.8, Diff: 0.3, Sum: 14.2]
      [GC Worker End (ms): Min: 397.3, Avg: 397.3, Max: 397.3, Diff: 0.0]
   [Code Root Fixup: 0.1 ms]
   [Code Root Purge: 0.1 ms]
   [Clear CT: 0.1 ms]
   [Other: 3.0 ms]
      [Choose CSet: 0.0 ms]
      [Ref Proc: 2.7 ms]
      [Ref Enq: 0.0 ms]
      [Redirty Cards: 0.2 ms]
      [Humongous Reclaim: 0.0 ms]
      [Free CSet: 0.0 ms]
   [Eden: 24.0M(24.0M)->0.0B(39.0M) Survivors: 0.0B->3072.0K Heap: 24.0M(256.0M)->5754.5K(256.0M)]
 [Times: user=0.02 sys=0.01, real=0.00 secs]
2015-09-30T11:13:04.343+0600: 0.868: [GC pause (G1 Evacuation Pause) (young), 0.0082908 secs]
   [Parallel Time: 5.1 ms, GC Workers: 8]
      [GC Worker Start (ms): Min: 868.3, Avg: 868.4, Max: 868.8, Diff: 0.5]
      [Ext Root Scanning (ms): Min: 0.0, Avg: 0.3, Max: 1.7, Diff: 1.7, Sum: 2.7]
      [Update RS (ms): Min: 0.0, Avg: 0.1, Max: 0.2, Diff: 0.2, Sum: 0.5]
         [Processed Buffers: Min: 0, Avg: 0.4, Max: 1, Diff: 1, Sum: 3]
      [Scan RS (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]
      [Code Root Scanning (ms): Min: 0.0, Avg: 0.4, Max: 1.4, Diff: 1.4, Sum: 3.5]
      [Object Copy (ms): Min: 3.2, Avg: 4.0, Max: 4.6, Diff: 1.4, Sum: 32.1]
      [Termination (ms): Min: 0.0, Avg: 0.1, Max: 0.1, Diff: 0.1, Sum: 0.6]
      [GC Worker Other (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.1]
      [GC Worker Total (ms): Min: 4.6, Avg: 4.9, Max: 5.0, Diff: 0.5, Sum: 39.5]
      [GC Worker End (ms): Min: 873.3, Avg: 873.3, Max: 873.4, Diff: 0.0]
   [Code Root Fixup: 0.3 ms]
   [Code Root Purge: 0.1 ms]
   [Clear CT: 0.1 ms]
   [Other: 2.7 ms]
      [Choose CSet: 0.0 ms]
      [Ref Proc: 2.4 ms]
      [Ref Enq: 0.0 ms]
      [Redirty Cards: 0.1 ms]
      [Humongous Reclaim: 0.0 ms]
      [Free CSet: 0.1 ms]
   [Eden: 39.0M(39.0M)->0.0B(147.0M) Survivors: 3072.0K->6144.0K Heap: 44.6M(256.0M)->13.9M(256.0M)]
 [Times: user=0.04 sys=0.00, real=0.01 secs]
2015-09-30T11:13:04.650+0600: 1.176: [GC pause (Metadata GC Threshold) (young) (initial-mark), 0.0090083 secs]
   [Parallel Time: 5.5 ms, GC Workers: 8]
      [GC Worker Start (ms): Min: 1175.9, Avg: 1176.0, Max: 1176.0, Diff: 0.1]
      [Ext Root Scanning (ms): Min: 1.1, Avg: 1.2, Max: 1.4, Diff: 0.3, Sum: 9.4]
      [Update RS (ms): Min: 0.0, Avg: 0.1, Max: 0.2, Diff: 0.2, Sum: 1.2]
         [Processed Buffers: Min: 0, Avg: 1.1, Max: 2, Diff: 2, Sum: 9]
      [Scan RS (ms): Min: 0.1, Avg: 0.1, Max: 0.3, Diff: 0.2, Sum: 1.1]
      [Code Root Scanning (ms): Min: 0.0, Avg: 0.2, Max: 0.5, Diff: 0.5, Sum: 2.0]
      [Object Copy (ms): Min: 3.4, Avg: 3.7, Max: 3.8, Diff: 0.4, Sum: 29.3]
      [Termination (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.0]
      [GC Worker Other (ms): Min: 0.0, Avg: 0.0, Max: 0.0, Diff: 0.0, Sum: 0.2]
      [GC Worker Total (ms): Min: 5.4, Avg: 5.4, Max: 5.5, Diff: 0.1, Sum: 43.3]
      [GC Worker End (ms): Min: 1181.4, Avg: 1181.4, Max: 1181.4, Diff: 0.0]
   [Code Root Fixup: 0.3 ms]
   [Code Root Purge: 0.0 ms]
   [Clear CT: 0.1 ms]
   [Other: 3.0 ms]
      [Choose CSet: 0.0 ms]
      [Ref Proc: 2.7 ms]
      [Ref Enq: 0.0 ms]
      [Redirty Cards: 0.1 ms]
      [Humongous Reclaim: 0.0 ms]
      [Free CSet: 0.1 ms]
   [Eden: 33.0M(147.0M)->0.0B(140.0M) Survivors: 6144.0K->13.0M Heap: 46.9M(256.0M)->20.9M(256.0M)]
 [Times: user=0.04 sys=0.00, real=0.01 secs]
2015-09-30T11:13:04.660+0600: 1.185: [GC concurrent-root-region-scan-start]
2015-09-30T11:13:04.664+0600: 1.190: [GC concurrent-root-region-scan-end, 0.0046509 secs]
2015-09-30T11:13:04.664+0600: 1.190: [GC concurrent-mark-start]
2015-09-30T11:13:04.665+0600: 1.190: [GC concurrent-mark-end, 0.0007287 secs]
2015-09-30T11:13:04.665+0600: 1.190: [GC remark 1.190: [Finalize Marking, 0.0001736 secs] 1.191: [GC ref-proc, 0.0000411 secs] 1.191: [Unloading, 0.0016740 secs], 0.0020377 secs]
 [Times: user=0.01 sys=0.00, real=0.00 secs]
2015-09-30T11:13:04.667+0600: 1.193: [GC cleanup 21M->14M(256M), 0.0004254 secs]
 [Times: user=0.01 sys=0.00, real=0.00 secs]
```

#### Type of Collections

A simple rule to watch for on your logs is the prefix of either:

```
[GC ...      <- Minor GC cycle (young gen)
[Full GC ... <- Full GC cycle
```

(:exclamation:) Explicit GCs can also be identified, which is when something is invoking the `System.gc()` API. Note, this is not good thing, as
something is forcing a GC cycle to occur, rather than letting the JVM trigger this on its own (what should naturally occur).

```
2015-09-30T12:23:44.425+0600: 195.699: [GC (System.gc()) [PSYoungGen: 39223K->3562K(76288K)] 49685K->14032K(190464K), 0.0047880 secs] [Times: user=0.02 sys=0.00, real=0.01 secs]
2015-09-30T12:23:44.430+0600: 195.704: [Full GC (System.gc()) [PSYoungGen: 3562K->0K(76288K)] [ParOldGen: 10469K->9174K(114176K)] 14032K->9174K(190464K), [Metaspace: 25137K->25137K(1071104K)], 0.0724521 secs] [Times: user=0.38 sys=0.01, real=0.07 secs]
```

It is generally recommended to use the `-XX:+DisableExplicitGC` JVM option to disable forceful GC events. This will allow
the JVM to still have garbage collections, but it disables them from being triggered explicitly. The description of this option:

> By default calls to System.gc() are enabled (-XX:-DisableExplicitGC). Use -XX:+DisableExplicitGC to disable calls to System.gc(). Note that the JVM still performs garbage collection when necessary.

### Enabling GC logging

With our service, let's go ahead and start it up with GC logging enabled:

```bash
# Navigate to the root directory of this project
java -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:gc.log -XX:GCLogFileSize=5M -XX:NumberOfGCLogFiles=2 \
 -jar java-perf-workshop-server/target/java-perf-workshop-server-1.0-SNAPSHOT.jar server server.yml 
```

### Parsing the log

For parsing the logs, we are just going to show a simple approach using [R](https://www.r-project.org/) to parse the
`gc.log` that we created. To do this, we will use the `stringr` package.

```r
# Including stringr package for regex
library(stringr)

# Navigating to where the project is at on my filesystem
setwd("~/java-perf-workshop")

# Read the GC log file in
gc <- readLines("gc.log")

# Regex the matches
matches <- str_match(gc, "(\\d+)K->(\\d+)K\\((\\d+)K\\),\\s(\\d+.\\d+)\\ssecs.*\\[Times\\: user=(\\d+.\\d+) sys=(\\d+.\\d+), real=(\\d+.\\d+) secs")

# Look at what matches, now we want to filter out the NA
matches

# Filter content out and convert to a data frame
gc.df <- data.frame(na.omit(matches[,-1]), stringsAsFactors=FALSE)

# Add a column header to describe fields
colnames(gc.df) <- c("HeapUsedBeforeGC_KB","HeapUsedAfterGC_KB", "HeapCapacity_KB", "GCPauseTime_Sec", "GCUserTime_Sec", "GCSysTime_Sec", "GCRealTime_Sec")

# List out the contents of the data frame
gc.df
```

Example output:

```
  HeapUsedBeforeGC_KB HeapUsedAfterGC_KB HeapCapacity_KB GCPauseTime_Sec GCUserTime_Sec GCSysTime_Sec GCRealTime_Sec
1               65536              12554          251392       0.0091645           0.03          0.01           0.01
2               45883              12707          316928       0.0092271           0.03          0.01           0.01
```

Reminder on the types of time being collected:

* `GCUserTime_Sec`: User space time
* `GCSysTime_Sec`: Kernel space time (operating system)
* `GCRealTime_Sec`: Complete time taken

Notice that the `GCRealTime_Sec` should closely align with the `GCPauseTime_Sec` value (just rounded up). If you notice that the
`GCUserTime_Sec` is much larger than the `GCRealTime_Sec`, you can conclude that multiple threads are executing garbage collection,
as `GCUserTime_Sec` is just the sum time of all the threads.

# GC Easy

There's an existing tool [GC Easy](http://gceasy.io/) which will do GC log parsing and analysis as well. 
