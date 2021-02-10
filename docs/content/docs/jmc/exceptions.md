---
title: "Exceptions"
weight: 12
description: >
    Find the exceptional cases in our application.
---

In this section we'll dive into the Exceptions page and find out what information we can learn from it. Start by selecting the __Exceptions__ page from the Outline.

This page offers the following sections:

* Overview: You can get Exceptions by Class and Count
* Message: View the detail messages for all exceptions and errors.
* Timeline/Event Log: View when a particular exception was thrown.
* StackTrace: Identify where the exception was thrown from.

![](/jmc/exceptions_page.png).

You can select an exception from the table to filter the information to that particular exception. You could also filter by a particular exception message.

![](/jmc/exceptions_select_iae.png)

## <i class="fas fa-compass"></i> Explore

`NullPointerExceptions` are  a familiar exception you deal with in Java, and often times indicate a programming error. Other exception types often point to a common symptom.

### <i class="fas fa-question"></i> Follow Ups

* What are the exceptions that are getting generated the most? 
  * What stack traces are the contributor?
* Can you find any areas in our code where we may have made an incorrect assumption regarding nullability?
  * Are there any other areas in our code where we might have made an incorrect assumption based on an exception's class?
* Are there any exceptions that might indicate a problem with our dependencies?

In the next section, we'll look at the [Threads](/docs/jmc/threads/) used by our application.