# Commons HawtDispatch

Contains some basic utilities for working with [HawtDispatch](http://hawtdispatch.fusesource.org/).

## Dispatch Queue Builder

Handles creating hierarchical dispatch queue names. A
[DispatchQueueBuilder](src/main/java/com/jive/myco/commons/hawtdispatch/DispatchQueueBuilder) can be
provided to a class who will create it's own queues internally for whatever it needs. The person
who creates the instance can prefix the dispatch queue with something and therefore the consumer of
the queue builder doesn't need to know anything about the top-level queue names. It is only
concerned with what it appends to the queue name.

See the javadoc for usage info.
