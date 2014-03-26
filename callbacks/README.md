# Callbacks

This module provides the mechanisms for projects to pass in and make use of callbacks. This is used
when an operation is asynchronous and a result is provided at a later time.

## ChainedFuture

The [ChainedFuture](src/main/java/com/jive/myco/commons/callbacks/ChainedFuture.java) class provides
a chainable implementation for Guava's `ListenableFuture`. This makes it possible to easily chain,
combine and transform asynchronous results together. See that class for more info.
