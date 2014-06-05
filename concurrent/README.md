# Concurrent

This module provides additional capabilities on top of the concurrent capabilities provided by the classes in the *java.util.concurrent* package.

## PnkyPromise

The [PnkyPromise](src/main/java/com/jive/myco/commons/concurrent/PnkyPromise.java) class is a custom implementation of a promise or futures framework. It provides the ability to work with short asynchronous tasks, chaining actions one after the other with custom handling for actions that succeed or fail.

In addition to the methods provided on the interface for listening to the results of a task, the [Pnky](src/main/java/com/jive/myco/commons/concurrent/Pnky.java) implementation provides utilities to initiate a process that will run on an executor and return a promise that you can use to start listening for the result when it is completed.

The *PnkyPromise* framework grew out of a need for capabilities that we found were lacking in Guava's *ListenableFuture* and Java 8's *CompletableFuture* classes that also provide capabilities to work with future asynchrounous tasks.

Have a look at the javadoc on [PnkyPromise](src/main/java/com/jive/myco/commons/concurrent/PnkyPromise.java) and the utility methods on [Pnky](src/main/java/com/jive/myco/commons/concurrent/Pnky.java) for more information on capabilities. Also take a look at an example of different usages in [PnkyExamples](src/test/java/com/jive/myco/commons/concurrent/PnkyExamples.java).