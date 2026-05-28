---
title: "Hello World Example"
toc_hide: true 
description: >
    Simple Java Hello World example for a container application on Kubernetes.
---

Here is a tiny Java HTTP service used by the JVM memory in
Kubernetes slides. The demo is intentionally small so the commands focus on the
container, cgroup, heap, and native-memory signals rather than application code.

* [HelloWorld.java](HelloWorld.java): Simple single Java file that is a HTTP server
* [Dockerfile](Dockerfile): Dockerfile for the application
* [k8s-deployment.yaml](k8s-deployment.yaml): Kubernetes deployment manifest

The Kubernetes manifest enables a few JVM diagnostics through
`JAVA_TOOL_OPTIONS`:

```yaml
env:
- name: JAVA_TOOL_OPTIONS
  value: >
    -Xlog:os+container=debug
    -XX:NativeMemoryTracking=summary
    -XX:+UseG1GC
    -XX:+FlightRecorder
    -XX:StartFlightRecording=filename=/tmp/startup.jfr,dumponexit=true
```

It also exposes JMX on port `9091` for JDK Mission Control.

## Build the Image

Run these commands from this directory:

```bash
docker build -t jvmperf.net/java-hello-world:v1 .
```

If you are using a local Kubernetes runtime such as minikube, kind, or Docker
Desktop, make sure the image is available to that cluster before deploying.

## Deploy to Kubernetes

Apply the deployment and service:

```bash
kubectl apply -f k8s-deployment.yaml
```

Wait for the pod to become ready:

```bash
kubectl rollout status deployment/java-hello-world
kubectl get pods -l app=java-hello-world
```

For the commands below, capture the pod name once:

```bash
POD=$(kubectl get pod -l app=java-hello-world -o jsonpath='{.items[0].metadata.name}')
echo "$POD"
```

## Verify the App

The service is a `NodePort`, so Kubernetes assigns a local node port for the
HTTP endpoint:

```bash
kubectl get svc java-hello-world-service
```

Use the returned `NODE-PORT` value with localhost:

```bash
NODE_PORT=$(kubectl get svc java-hello-world-service -o jsonpath='{.spec.ports[0].nodePort}')
curl "http://localhost:${NODE_PORT}/"
```

Expected response:

```text
Hello, World from Kubernetes!
```

## Check the JVM's Container View

Start with `-XshowSettings:system`. This is the quick check that the JVM sees the
pod's cgroup limits rather than sizing itself from the whole node:

```bash
kubectl exec "$POD" -- java -XshowSettings:system -version
```

Look for output shaped like this:

```text
Operating System Metrics:
    Provider: cgroupv2
    Effective CPU Count: 1
    CPU Quota: 100000us
    CPU Period: 100000us
    Memory Limit: 1.00G
    Memory & Swap Limit: 1.00G
```

The important fields for the slides are:

| Field | Why it matters |
| --- | --- |
| `Provider` | Shows whether the JVM is reading cgroup data. |
| `Effective CPU Count` | Influences GC threads, JIT threads, and app parallelism. |
| `Memory Limit` | The process budget for heap plus native memory. |
| `CPU Quota` / `CPU Period` | Should line up with the deployment CPU limit. |

## Inspect Native Memory

Native Memory Tracking is enabled in the manifest with
`-XX:NativeMemoryTracking=summary`, so you can ask the running JVM for its native
memory map:

```bash
kubectl exec "$POD" -- jcmd 1 VM.native_memory summary
```

The top of the output gives total reserved and committed memory:

```text
Native Memory Tracking:

Total: reserved=1677076KB, committed=43616KB
       malloc: 3328KB #7179, peak=3201KB #7181
       mmap:   reserved=1673748KB, committed=40288KB
```

Then NMT breaks memory into JVM categories:

```text
- Java Heap (reserved=262144KB, committed=16384KB)
- Class     (reserved=1048664KB, committed=280KB)
- Thread    (reserved=30653KB, committed=1141KB)
- Code      (reserved=249735KB, committed=7735KB)
- GC        (reserved=860KB, committed=64KB)
```

Use this breakdown to explain where memory outside the Java heap can come from:
thread stacks, class metadata, code cache, GC structures, direct buffers, native
libraries, and JVM internals.

## Compare Heap and NMT

`GC.heap_info` explains the Java heap from the collector's point of view:

```bash
kubectl exec "$POD" -- jcmd 1 GC.heap_info
```

Example:

```text
garbage-first heap   total reserved 262144K, committed 18432K, used 2925K
```

For the Java heap, `reserved` and `committed` should line up with the
`Java Heap` row in NMT. `heap_info` adds `used`, which is the live heap occupancy
inside the committed heap space.

## Inspect VM and Cgroup Details

`VM.info` gives a broader diagnostic snapshot, including the JVM's cgroup view:

```bash
kubectl exec "$POD" -- jcmd 1 VM.info
```

Useful segment:

```text
container (cgroup) information:
container_type: cgroupv2
active_processor_count: 1
cpu_quota: 100000
cpu_period: 100000
memory_limit_in_bytes: 1048576 k
memory_and_swap_limit_in_bytes: 1048576 k
memory_usage_in_bytes: 118316 k
rss_usage_in_bytes: 116428 k
cache_usage_in_bytes: 64 k
current number of tasks: 29
```

Capture this beside the pod spec, heap settings, and NMT output when you want to
explain why a container is close to its limit.

## Record Heap Behavior with JFR

Java Flight Recorder is useful during load tests because it keeps the heap story
tied to time. Use it to see heap usage, allocation pressure, and GC behavior
across the same window where you are collecting NMT and cgroup data.

The manifest starts a recording automatically:

```yaml
-XX:+FlightRecorder
-XX:StartFlightRecording=filename=/tmp/startup.jfr,dumponexit=true
```

For a specific test window, start and stop a named recording with `jcmd`:

```bash
kubectl exec "$POD" -- jcmd 1 JFR.start name=load-test filename=/tmp/load-test.jfr settings=profile
# run representative load
kubectl exec "$POD" -- jcmd 1 JFR.dump name=load-test filename=/tmp/load-test.jfr
kubectl exec "$POD" -- jcmd 1 JFR.stop name=load-test
```

Copy the recording locally:

```bash
kubectl cp "${POD}:/tmp/load-test.jfr" ./load-test.jfr
```

Open `load-test.jfr` in JDK Mission Control. For heap-focused analysis, start
with heap usage, allocation, GC pause, and GC heap summary views.

## Use JDK Mission Control

The manifest opens JMX on container port `9091`. Port-forward it to your machine:

```bash
kubectl port-forward "$POD" 9091:9091
```

Then connect JDK Mission Control to:

```text
localhost:9091
```

This is useful when you want to show the memory story over time rather than only
single command snapshots.

## Cleanup

Remove the demo resources when you are done:

```bash
kubectl delete -f k8s-deployment.yaml
```

## References

- `java.lang.management.OperatingSystemMXBean`: https://docs.oracle.com/en/java/javase/11/docs/api/java.management/java/lang/management/OperatingSystemMXBean.html
- `com.sun.management.OperatingSystemMXBean`: https://docs.oracle.com/en/java/javase/25/docs/api/jdk.management/com/sun/management/OperatingSystemMXBean.html
- Native Memory Tracking: https://docs.oracle.com/en/java/javase/11/vm/native-memory-tracking.html
