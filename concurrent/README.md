# Concurrent

This module provides additional capabilities on top of the concurrent capabilities provided by the classes in the *java.util.concurrent* package.

## CompletableFutures

The [CompletableFutures](src/main/java/com/jive/myco/commons/concurrent/CompletableFutures.java) class provides
a set of utility methods that provide additional capabilities not found on *CompletableFuture* itself.  These utility methods are interoperable with *CompletableFuture*.

### Functions that supply / throw Exceptions

*CompletableFuture* does not provide the ability to invoke functional interfaces that throw checked exceptions.  While APIs designed around *CompletableFuture* should not throw checked exceptions from the API methods, implementors of the API often need to interact with libraries that do throw checked exceptions and or complete futures with checked / unchecked exceptions not wrapped in a *CompletionException*.

#### Implementing an asynchronous operation that can complete with an exception

The following examples demonstrate how to implement an asynchronous operation where the implementation returns an Exception via the future using only *CompletableFuture*.

```
  /**
   * Shows how to use {@link CompletableFuture} to execute a step that throws an exception that
   * should be used to complete the future directly.
   */
  public CompletionStage<Void> completableFutureNewExceptionalProcessExampleMethod()
  {
    final CompletableFuture<Void> future = new CompletableFuture<>();

    executor.execute(() ->
    {
      try
      {
        doSomethingExceptional();
        future.complete(null);
      }
      catch (final Exception e)
      {
        future.completeExceptionally(e);
      }
    });

    return future;
  }

  /**
   * Shows an alternate example of how to use {@link CompletableFuture} to execute a step that
   * throws an exception that should be used to complete the future directly.
   */
  public CompletionStage<Void> completableFutureNewExceptionalProcessExampleAlternateMethod()
  {
    return CompletableFuture
        .runAsync(Runnables.doNothing(), executor)
        .thenCompose((nothing) ->
        {
          final CompletableFuture<Void> future = new CompletableFuture<>();

          try
          {
            doSomethingExceptional();
            future.complete(null);
          }
          catch (final Exception e)
          {
            future.completeExceptionally(e);
          }

          return future;
        });
  }
  
  public void doSomethingExceptional() throws Exception
  {
  }
```

The following example demonstrates how to implement an asynchronous operation where the implementation returns an Exception via the future using utility methods in *CompletableFutures*.  This approach is slightly less efficient due to additional internal object creation within *CompletableFutures*; however, it is significantly less verbose.

```
  /**
   * Shows how to use {@link CompletableFutures} to execute a step that throws an exception that
   * should be used to complete the future directly.
   */
  public CompletionStage<Void> completableFuturesNewExceptionalProcessExampleMethod()
  {
    return CompletableFutures.runExceptionallyAsync(() -> doSomethingExceptional(), executor);
  }
  
  public void doSomethingExceptional() throws Exception
  {
  }
```

#### Implementing a mult-step asynchronous process that can complete with an exception mid-process

Ocassionally the in-line invocation of a synchronous method that throws an exception is desired.


The following example demonstrates how to implement an asynchronous process, using only *CompletableFuture*, where a step mid process invokes code that throws an exception and completion of the future using the thrown exception is desired.

```
/**
   * Shoes how to use {@link CompletableFuture} to execute a step, in-line mid process, that throws
   * an exception that should be used to complete the future directly.
   */
  public CompletionStage<Void> completableFutureInlineExceptionalProcessExampleMethod()
  {
    return doSomethingUsingCf()
        .thenCompose((result) ->
        {
          final CompletableFuture<Void> future = new CompletableFuture<>();

          if (result == 1)
          {
            try
            {
              doSomethingExceptional();
              future.complete(null);
            }
            catch (final Exception e)
            {
              future.completeExceptionally(e);
            }
          }

          return future;
        });
  }
  
  public CompletionStage<Integer> doSomethingUsingCf()
  {
    return CompletableFuture.supplyAsync(() -> 1, executor);
  }
  
  public void doSomethingExceptional() throws Exception
  {
  }
```


The following example demonstrates how to implement an asynchronous process, using *CompletableFutures* utility methods, where a step mid process invokes code that throws an exception and completion of the future using the thrown exception is desired.  This approach is slightly less efficient due to additional internal object creation within *CompletableFutures*; however, it is significantly less verbose.

```
/**
   * Shoes how to use {@link CompletableFutures} to execute a step, in-line mid process, that throws
   * an exception that should be used to complete the future directly.  
   */
  public CompletionStage<Void> completableFuturesInlineExceptionalProcessExampleMethod()
  {
    return doSomethingUsingCf()
        .thenCompose(CompletableFutures.thenAcceptExceptionally((result) ->
        {
          if (result == 1)
          {
            doSomethingExceptional();
          }
        }));
  }
  
  public CompletionStage<Integer> doSomethingUsingCf()
  {
    return CompletableFuture.supplyAsync(() -> 1, executor);
  }
  
  public void doSomethingExceptional() throws Exception
  {
  }
```


### Suspending and Resuming a DispatchQueue while performing additional asynchronous operations

A HawtDispatch *DispatchQueue* is often used to protect resource state without the need for locks or synchronization.  A common pattern is to protect state by starting a process on a *DispatchQueue*, suspending the queue before launching additional asynchronous processes, and then resuming the queue when the additional processes complete.

The following examples demonstrate how to suspend a *DispatchQueue* while performing additional asynchronous operations using only *CompletableFuture*.

```
  /**
   * Shows how to use {@link CompletableFuture} to execute an asynchronous process while suspending
   * a {@link DispatchQueue}.
   */
  public CompletionStage<String> completableFutureQueueSuspendExampleMethod()
  {
    final CompletableFuture<String> future = new CompletableFuture<>();

    dispatchQueue.execute(() ->
    {
      dispatchQueue.suspend();

      doSomethingUsingCf()
          .thenCompose((value) -> doSomethingElseUsingCf(value, 2))
          // Could have a static helper method to help handle this common use case
          .whenComplete((result, cause) ->
          {
            dispatchQueue.resume();

            if (cause != null)
            {
              future.completeExceptionally(cause);
            }
            else
            {
              future.complete(result);
            }
          });
    });

    return future;
  }

  /**
   * Shows an alternate example of how to use {@link CompletableFuture} to execute an asynchronous
   * process while suspending a {@link DispatchQueue}.
   */
  public CompletionStage<String> completableFutureQueueSuspendExampleAlternateMethod()
  {
    return CompletableFuture
        .supplyAsync(() ->
        {
          dispatchQueue.suspend();

          return doSomethingUsingCf()
              .thenCompose((value) -> doSomethingElseUsingCf(value, 2))
              .whenComplete((result, cause) -> dispatchQueue.resume());
        }, dispatchQueue)
        .thenCompose((x) -> x); // Function::identity is problematic
  }
  
  public CompletionStage<Integer> doSomethingUsingCf()
  {
    return CompletableFuture.supplyAsync(() -> 1, executor);
  }

  public CompletionStage<String> doSomethingElseUsingCf(final Integer value, final int radix)
  {
    return CompletableFuture.supplyAsync(() -> Integer.toString(value, radix), executor);
  }
```

The following example demonstrates how to suspend a *DispatchQueue* while performing additional asynchronous operations using *CompletableFutures*.

```
  /**
   * Shows how to use {@link CompletableFutures} to execute an asynchronous process while suspending
   * a {@link DispatchQueue}.
   */
  public CompletionStage<String> completableFuturesQueueSuspendExampleMethod()
  {
    return CompletableFutures
        .composeAsync(() ->
        {
          dispatchQueue.suspend();

          return doSomethingUsingCf()
              .thenCompose((value) -> doSomethingElseUsingCf(value, 2))
              .whenComplete((result, cause) -> dispatchQueue.resume());
        }, dispatchQueue);
  }

  public CompletionStage<Integer> doSomethingUsingCf()
  {
    return CompletableFuture.supplyAsync(() -> 1, executor);
  }

  public CompletionStage<String> doSomethingElseUsingCf(final Integer value, final int radix)
  {
    return CompletableFuture.supplyAsync(() -> Integer.toString(value, radix), executor);
  }
```

## EnhancedCompletableFuture

The [EnhancedCompletableFuture](src/main/java/com/jive/myco/commons/concurrent/EnhancedCompletableFuture) class provides
a drop-in / interroperable replacement for *CompletableFuture* itself.  The *EnhancedCompletableFuture* provides a more fluent and unified interface for combining *CompletableFuture* with the utility methods in *CompletableFutures* with the disadvantage that its current implementation adds object creation overhead by wrapping a *CompletableFuture* instance using the delegate pattern.


```
  /**
   * Shows how to use {@link EnhancedCompletableFuture} to execute a step that throws an exception
   * that should be used to complete the future directly.
   */
  public EnhancedCompletionStage<Void> enhancedCompletableFuturesNewExceptionalProcessExampleMethod()
  {
    return EnhancedCompletableFuture
        .runExceptionallyAsync(() -> doSomethingExceptional(), executor);
  }

  public void doSomethingExceptional() throws Exception
  {
  }
```

```
  /**
   * Shows how to use {@link EnhancedCompletableFuture} to execute an asynchronous process while
   * suspending a {@link DispatchQueue}.
   */
  public EnhancedCompletionStage<String> enhancedCompletableFutureQueueSuspendExampleMethod()
  {
    return EnhancedCompletableFuture
        .composeAsync(() ->
        {
          dispatchQueue.suspend();

          return doSomethingUsingEcf()
              .thenCompose((value) -> doSomethingElseUsingEcf(value, 2))
              // Could have a utility method for this step
              .whenComplete((result, cause) -> dispatchQueue.resume());
        }, dispatchQueue);
  }

  public EnhancedCompletionStage<Integer> doSomethingUsingEcf()
  {
    return EnhancedCompletableFuture.supplyAsync(() -> 1, executor);
  }

  public EnhancedCompletionStage<String> doSomethingElseUsingEcf(final Integer value,
      final int radix)
  {
    return EnhancedCompletableFuture.supplyAsync(() -> Integer.toString(value, radix), executor);
  }
```

```
  /**
   * Shoes how to use {@link EnhancedCompletableFuture} to execute a step, in-line mid process, that
   * throws an exception that should be used to complete the future directly.
   */
  public EnhancedCompletionStage<Void> enhancedCompletableFutureInlineExceptionalProcessExampleMethod()
  {
    return doSomethingUsingEcf()
        .thenAcceptExceptionally((result) ->
        {
          if (result == 1)
          {
            doSomethingExceptional();
          }
        });
  }
  
  public EnhancedCompletionStage<Integer> doSomethingUsingEcf()
  {
    return EnhancedCompletableFuture.supplyAsync(() -> 1, executor);
  }
  
  public void doSomethingExceptional() throws Exception
  {
  }
```