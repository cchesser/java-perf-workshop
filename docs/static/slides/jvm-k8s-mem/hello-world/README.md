# Try it out

Build with:

```
docker build -t cchesser/java-hello-world:v1 .
```

Deploy with:

```
kubectl apply -f k8s-deployment.yaml
```

Find the internal port mapping:

```
kubectl get svc java-hello-world-service
```

Take the port that is returned, and then apply it to localhost: http://localhost:32435/ 

Should now return:

```
Hello, World from Kubernetes!
```

Run `kubectl get pods` to get the pod name, to then make this call:

```
kubectl exec java-hello-world-c974f7d4f-dw4rq -- java -XshowSettings:system -version
```

```
kubectl exec java-hello-world-8675d6cff9-b8v6x -- jcmd 1 VM.native_memory summary
```



Example output:

```
Native Memory Tracking:

(Omitting categories weighting less than 1KB)

Total: reserved=1677076KB, committed=43616KB
       malloc: 3328KB #7179, peak=3201KB #7181
       mmap:   reserved=1673748KB, committed=40288KB

-                 Java Heap (reserved=262144KB, committed=16384KB)
                            (mmap: reserved=262144KB, committed=16384KB, at peak)

-                     Class (reserved=1048664KB, committed=280KB)
                            (classes #1164)
                            (  instance classes #1004, array classes #160)
                            (malloc=88KB tag=Class #1486) (at peak)
                            (mmap: reserved=1048576KB, committed=192KB, at peak)
                            (  Metadata:   )
                            (    reserved=65536KB, committed=896KB)
                            (    used=852KB)
                            (    waste=44KB =4.90%)
                            (  Class space:)
                            (    reserved=1048576KB, committed=192KB)
                            (    used=92KB)
                            (    waste=100KB =52.17%)

-                    Thread (reserved=30653KB, committed=1141KB)
                            (threads #15)
                            (stack: reserved=30600KB, committed=1088KB, peak=1088KB)
                            (malloc=37KB tag=Thread #91) (peak=46KB #95)
                            (arena=15KB #26) (peak=78KB #24)

-                      Code (reserved=249735KB, committed=7735KB)
                            (malloc=126KB tag=Code #2157) (at peak)
                            (mmap: reserved=249608KB, committed=7608KB, at peak)
                            (arena=1KB #1) (peak=34KB #2)

-                        GC (reserved=860KB, committed=64KB)
                            (malloc=4KB tag=GC #54) (at peak)
                            (mmap: reserved=856KB, committed=60KB, at peak)

-                  Compiler (reserved=200KB, committed=200KB)
                            (malloc=4KB tag=Compiler #36) (at peak)
                            (arena=196KB #6) (peak=1124KB #7)

-                  Internal (reserved=1228KB, committed=1228KB)
                            (malloc=1192KB tag=Internal #967) (at peak)
                            (mmap: reserved=36KB, committed=36KB, at peak)

-                     Other (reserved=16KB, committed=16KB)
                            (malloc=16KB tag=Other #1) (peak=26KB #3)

-                    Symbol (reserved=1157KB, committed=1157KB)
                            (malloc=797KB tag=Symbol #703) (at peak)
                            (arena=360KB #1) (at peak)

-    Native Memory Tracking (reserved=131KB, committed=131KB)
                            (malloc=5KB tag=Native Memory Tracking #74) (at peak)
                            (tracking overhead=126KB)

-        Shared class space (reserved=16384KB, committed=14016KB, readonly=0KB)
                            (mmap: reserved=16384KB, committed=14016KB, peak=14272KB)

-               Arena Chunk (reserved=257KB, committed=257KB)
                            (malloc=257KB tag=Arena Chunk #47) (peak=1545KB #73)

-                   Tracing (reserved=4KB, committed=4KB)
                            (malloc=4KB tag=Tracing #20) (at peak)

-                    Module (reserved=40KB, committed=40KB)
                            (malloc=40KB tag=Module #1261) (at peak)

-                 Safepoint (reserved=8KB, committed=8KB)
                            (mmap: reserved=8KB, committed=8KB, at peak)

-           Synchronization (reserved=27KB, committed=27KB)
                            (malloc=27KB tag=Synchronization #238) (at peak)

-            Serviceability (reserved=17KB, committed=17KB)
                            (malloc=17KB tag=Serviceability #14) (peak=20KB #18)

-                 Metaspace (reserved=65549KB, committed=909KB)
                            (malloc=13KB tag=Metaspace #10) (at peak)
                            (mmap: reserved=65536KB, committed=896KB, at peak)

-      String Deduplication (reserved=1KB, committed=1KB)
                            (malloc=1KB tag=String Deduplication #8) (at peak)

-           Object Monitors (reserved=1KB, committed=1KB)
                            (malloc=1KB tag=Object Monitors #4) (at peak)
```


```
kubectl exec java-hello-world-8675d6cff9-b8v6x -- jcmd 1 VM.info
```

Example segment:
```
container (cgroup) information:
container_type: cgroupv2
cpu_cpuset_cpus:
cpu_memory_nodes:
active_processor_count: 1
cpu_quota: 100000
cpu_period: 100000
cpu_shares: 264
cpu_usage_in_micros: 436057
memory_limit_in_bytes: 1048576 k
memory_and_swap_limit_in_bytes: 1048576 k
memory_soft_limit_in_bytes: 0
memory_throttle_limit_in_bytes: unlimited
memory_usage_in_bytes: 118316 k
memory_max_usage_in_bytes: 118576 k
rss_usage_in_bytes: 116428 k
cache_usage_in_bytes: 64 k
memory_swap_current_in_bytes: 0
memory_swap_max_limit_in_bytes: 0
maximum number of tasks: unlimited
current number of tasks: 29
```

kubectl exec java-hello-world-56765fcf5d-mx7tq -- jcmd 1 GC.heap_info