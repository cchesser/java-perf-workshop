---
title: "I/O Operations"
weight: 15
description: >
    Find the interactions with external resources.
---

The I/O pages can provide context of what external resources you are utilizing from you code base, and relate it to other latency events.

For the purpose of our workshop service, we are doing network I/O, and therefore will look at the __Socket I/O__ page.

From this page, we can gain context of what host(s) we are calling, with what frequency, and the cost associated with those calls:

![](/jmc/io_page.png)

We can then filter on a particular host and port and drill down into a particular piece of code responsible for that call:

![](/jmc/io_page_9090.png)

## <i class="fas fa-compass"></i> Explore

Filter through the total I/O time and explore a couple of events with high I/O. 


### <i class="fas fa-question"></i> Follow Ups

* What remote services are we interacting with?
* How long are we interacting with them and how many invocations?
* How many times did we make calls and what threads were tied to those calls?
