---
title: "Process and Threads"
weight: 10
description: >
    Initial look at the JVM from its native perspective.
---

This first part of the workshop, we will begin to learn about looking at the JVM from its native perspective, and begin obtaining JVM based information to correlate to it's native representation.

## top

A very common utility as to monitor processes in Linux. For our case, we will want to monitor our JVM process. When just executing `top`, you will see all processes on the host:

```
top - 05:32:51 up 8 min,  1 user,  load average: 0.05, 0.12, 0.06
Tasks:  62 total,   1 running,  61 sleeping,   0 stopped,   0 zombie
Cpu(s): 11.2%us,  0.7%sy,  0.0%ni, 87.8%id,  0.0%wa,  0.0%hi,  0.0%si,  0.3%st
Mem:   1020184k total,   466284k used,   553900k free,    17740k buffers
Swap:        0k total,        0k used,        0k free,   289500k cached

  PID USER      PR  NI  VIRT  RES  SHR S %CPU %MEM    TIME+  COMMAND
 6730 ec2-user  20   0 2246m 108m  13m S 10.3 10.9   0:14.11 java
    1 root      20   0 19596 1616 1292 S  0.0  0.2   0:00.56 init
    2 root      20   0     0    0    0 S  0.0  0.0   0:00.00 kthreadd
    3 root      20   0     0    0    0 S  0.0  0.0   0:00.02 ksoftirqd/0
    4 root      20   0     0    0    0 S  0.0  0.0   0:00.00 kworker/0:0
    5 root       0 -20     0    0    0 S  0.0  0.0   0:00.00 kworker/0:0H
    6 root      20   0     0    0    0 S  0.0  0.0   0:00.04 kworker/u30:0
    7 root      20   0     0    0    0 S  0.0  0.0   0:00.27 rcu_sched
    8 root      20   0     0    0    0 S  0.0  0.0   0:00.00 rcu_bh
    9 root      RT   0     0    0    0 S  0.0  0.0   0:00.00 migration/0
```

You can switch in this view to also view the native threads, by hitting `Shift+H`.

```
top - 05:34:15 up 9 min,  1 user,  load average: 0.01, 0.09, 0.05
Tasks: 101 total,   1 running, 100 sleeping,   0 stopped,   0 zombie
Cpu(s):  4.0%us,  0.3%sy,  0.0%ni, 95.7%id,  0.0%wa,  0.0%hi,  0.0%si,  0.0%st
Mem:   1020184k total,   491176k used,   529008k free,    17804k buffers
Swap:        0k total,        0k used,        0k free,   312648k cached
 Show threads On
  PID USER      PR  NI  VIRT  RES  SHR S %CPU %MEM    TIME+  COMMAND
 6736 ec2-user  20   0 2246m 109m  13m S  2.0 11.0   0:05.72 java
 6737 ec2-user  20   0 2246m 109m  13m S  0.3 11.0   0:01.69 java
 6740 ec2-user  20   0 2246m 109m  13m S  0.3 11.0   0:01.67 java
 6746 ec2-user  20   0 2246m 109m  13m S  0.3 11.0   0:00.31 java
 6768 ec2-user  20   0 2246m 109m  13m S  0.3 11.0   0:00.03 java
 6778 ec2-user  20   0 2246m 109m  13m S  0.3 11.0   0:00.08 java
 6777 ec2-user  20   0 15224 1332 1032 R  0.3  0.1   0:00.04 top
    1 root      20   0 19596 1616 1292 S  0.0  0.2   0:00.56 init
    2 root      20   0     0    0    0 S  0.0  0.0   0:00.00 kthreadd
    3 root      20   0     0    0    0 S  0.0  0.0   0:00.02 ksoftirqd/0
    4 root      20   0     0    0    0 S  0.0  0.0   0:00.00 kworker/0:0
```

With these native threads, you can now get better context of what resources are being utilized. To simplify this, you can target top to just watch one parent process and then show threads via:

```bash
top -H -p <PID>
```

In our earlier example, our PID was 6730. Therefore, this would produce:

```
$ top -H -p 6730
top - 05:38:05 up 13 min,  1 user,  load average: 0.00, 0.05, 0.05
Tasks:  32 total,   0 running,  32 sleeping,   0 stopped,   0 zombie
Cpu(s):  0.0%us,  0.0%sy,  0.0%ni,100.0%id,  0.0%wa,  0.0%hi,  0.0%si,  0.0%st
Mem:   1020184k total,   515940k used,   504244k free,    17932k buffers
Swap:        0k total,        0k used,        0k free,   336520k cached

  PID USER      PR  NI  VIRT  RES  SHR S %CPU %MEM    TIME+  COMMAND
 6739 ec2-user  20   0 2246m 110m  13m S  0.3 11.1   0:00.24 java
 6730 ec2-user  20   0 2246m 110m  13m S  0.0 11.1   0:00.00 java
 6731 ec2-user  20   0 2246m 110m  13m S  0.0 11.1   0:01.50 java
 6732 ec2-user  20   0 2246m 110m  13m S  0.0 11.1   0:01.07 java
 6733 ec2-user  20   0 2246m 110m  13m S  0.0 11.1   0:00.00 java
 6734 ec2-user  20   0 2246m 110m  13m S  0.0 11.1   0:00.00 java
 6735 ec2-user  20   0 2246m 110m  13m S  0.0 11.1   0:00.00 java
 6736 ec2-user  20   0 2246m 110m  13m S  0.0 11.1   0:07.33 java
 6737 ec2-user  20   0 2246m 110m  13m S  0.0 11.1   0:01.69 java
 6738 ec2-user  20   0 2246m 110m  13m S  0.0 11.1   0:00.00 java
```

To make a simple standard capture of native threads for diagnostic purposes, you can do the following:

```bash
top -b -n3 -H -p <PID>
```

This will capture thread iterations of `top` by capturing threads of the parent process ID. You can then pipe it to a file.

Example:

```bash
top -b -n3 -H -p 6730 > jvm_native_threads.log
```

## Thread Dump

To capture a thread dump of the JVM, there are many tools that can achieve this. You can do this over JMX remotely, sending an OS signal (`kill -3`), or just form the command line (`jstack` or `jcmd`). To keep it simple, we will just use `jcmd` as we will use it later for other diagnostic commands.

```bash
jcmd <PID> Thread.print
```

:bulb: You need to be the same OS user when invoking the command as the target JVM.

So, using our earlier example, this would be:

```bash
jcmd 6730 Thread.print
```

However, correlating a thread dump to the native thread state requires a minor translation. In your thread dump, the native thread identifier is in hexadecimal, while the native thread identifiers from `top` are in decimal. Therefore, if you notice a thread spiking in the `top` view, you would then want to understand what cod it is correlating to. You can do this simply from the command line via:

```bash
printf "%x\n" <Decimal Thread ID>
```

Example:

```bash
$ printf "%x\n" 6736
1a50
$
```

I can then cross reference this thread to my thread dump via the `nid` (native thread ID) field:

```
"C2 CompilerThread0" #5 daemon prio=9 os_prio=0 tid=0x00007efda40d4000 nid=0x1a50 waiting on condition [0x0000000000000000]
   java.lang.Thread.State: RUNNABLE
```

An alternative tool [`fastthread.io`](https://fastthread.io/) also has a blog on other ways to capture a [thread dump](https://blog.fastthread.io/2016/06/06/how-to-take-thread-dumps-7-options/)

## jcmd PerfCounter

A very simple and quick collection of stats on a JVM can be collected via:

```bash
jcmd <PID> PerfCounter.print
```

Generally the information in this file may or may not indicate the actual problem, but can provide more context in a concise text file of what all has occurred in the life of that JVM. One simple set of counters related to threads are:

```
java.threads.daemon=20
java.threads.live=26
java.threads.livePeak=38
java.threads.started=697
```

## strace

In some rare cases, you may need to figure out what system calls your JVM is doing. One way to do this on several flavors of Linux, is to use `strace`.

```bash
strace -f -v -p <PID>
```

Example:

```bash
strace -f -v -p 6730
```

Example output:

```
[pid  6741] futex(0x7efda4587228, FUTEX_WAKE_PRIVATE, 1 <unfinished ...>
[pid  6740] write(1, "DEBUG [2015-06-24 05:50:34,727] "..., 168 <unfinished ...>
[pid  6830] gettimeofday( <unfinished ...>
```

From the `write` call, we can see it is tied to the thread ID 6740. If we translate it to `x1a54`, which is then tied to this logging thread in the JVM.

```
"AsyncAppender-Worker-Thread-1" #9 daemon prio=5 os_prio=0 tid=0x00007efda44d1800 nid=0x1a54 waiting on condition [0x00007efd941f0000]
   java.lang.Thread.State: WAITING (parking)
	at sun.misc.Unsafe.park(Native Method)
	- parking to wait for  <0x00000000f5d38d20> (a java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject)
	at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)
	at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2039)
```

You will notice that the system calls are quite noisy with the JVM, but this type of troubleshooting can greatly help when the JVM is getting "hung" or hitting limits by the OS and it isn't clear from the exception that you are seeing what it is getting restricted on.
