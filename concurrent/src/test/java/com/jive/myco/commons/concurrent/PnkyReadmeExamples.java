package com.jive.myco.commons.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.fusesource.hawtdispatch.DispatchQueue;

/**
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public class PnkyReadmeExamples
{
  private Executor executor;
  private DispatchQueue dispatchQueue;

  /**
   * Shows how to use {@link CompletableFuture} to execute a step that throws an exception that
   * should be used to complete the future directly.
   */
  public PnkyPromise<Void> completableFutureNewExceptionalProcessExampleMethod()
  {
    final Pnky<Void> future = Pnky.create();

    executor.execute(() ->
    {
      try
      {
        doSomethingExceptional();
        future.set(null);
      }
      catch (final Exception e)
      {
        future.setException(e);
      }
    });

    return future;
  }

  /**
   * Shows an alternate example of how to use {@link CompletableFuture} to execute a step that
   * throws an exception that should be used to complete the future directly.
   */
  public PnkyPromise<Void> completableFutureNewExceptionalProcessExampleAlternateMethod()
  {
    return Pnky
        .runAsync(() -> {}, executor)
        .thenCompose((input) ->
        {
          Pnky<Void> future = Pnky.create();

          try
          {
            doSomethingExceptional();
            future.set(null);
          }
          catch (final Exception e)
          {
            future.setException(e);
          }

          return future;
        });
  }

  public void doSomethingExceptional() throws Exception
  {
  }

  /**
   * Shows how to use {@link CompletableFutures} to execute a step that throws an exception that
   * should be used to complete the future directly.
   */
  public PnkyPromise<Void> completableFuturesNewExceptionalProcessExampleMethod()
  {
    return Pnky.runAsync(this::doSomethingExceptional, executor);
  }

  /**
   * Shoes how to use {@link CompletableFuture} to execute a step, in-line mid process, that throws
   * an exception that should be used to complete the future directly.
   */
  public PnkyPromise<Void> completableFutureInlineExceptionalProcessExampleMethod()
  {
    return doSomethingUsingCf()
        .thenCompose((result) ->
        {
          final Pnky<Void> future = Pnky.create();

          if (result == 1)
          {
            try
            {
              doSomethingExceptional();
              future.set(null);
            }
            catch (final Exception e)
            {
              future.setException(e);
            }
          }

          return future;
        });
  }

  public PnkyPromise<Integer> doSomethingUsingCf()
  {
    return Pnky.supplyAsync(() -> 1, executor);
  }

  /**
   * Shoes how to use {@link CompletableFutures} to execute a step, in-line mid process, that throws
   * an exception that should be used to complete the future directly.
   */
  public PnkyPromise<Void> completableFuturesInlineExceptionalProcessExampleMethod()
  {
    return doSomethingUsingCf()
        .thenTransform((result) ->
        {
          if (result == 1)
          {
            doSomethingExceptional();
          }

          return null;
        });
  }

  /**
   * Shows how to use {@link CompletableFuture} to execute an asynchronous process while suspending
   * a {@link DispatchQueue}.
   */
  public PnkyPromise<String> completableFutureQueueSuspendExampleMethod()
  {
    final Pnky<String> future = Pnky.create();

    dispatchQueue.execute(() ->
    {
      dispatchQueue.suspend();

      doSomethingUsingCf()
          .thenCompose((value) -> doSomethingElseUsingCf(value, 2))
          .thenHandle((result, error) ->
          {
            dispatchQueue.resume();

            if (error != null)
            {
              future.setException(error);
            }
            else
            {
              future.set(result);
            }
          });
    });

    return future;
  }

  /**
   * Shows an alternate example of how to use {@link CompletableFuture} to execute an asynchronous
   * process while suspending a {@link DispatchQueue}.
   */
  public PnkyPromise<String> completableFutureQueueSuspendExampleAlternateMethod()
  {
    return Pnky
        .supplyAsync(() ->
        {
          dispatchQueue.suspend();

          return doSomethingUsingCf()
              .thenCompose((value) -> doSomethingElseUsingCf(value, 2))
              .thenHandle((result, cause) -> dispatchQueue.resume());
        }, dispatchQueue)
        .thenCompose((x) -> x);
  }

  public PnkyPromise<String> doSomethingElseUsingCf(final Integer value, final int radix)
  {
    return Pnky.supplyAsync(() -> Integer.toString(value, radix), executor);
  }

  /**
   * Shows how to use {@link CompletableFutures} to execute an asynchronous process while suspending
   * a {@link DispatchQueue}.
   */
  public PnkyPromise<String> completableFuturesQueueSuspendExampleMethod()
  {
    return Pnky
        .composeAsync(() ->
        {
          dispatchQueue.suspend();

          return doSomethingUsingCf()
              .thenCompose((value) -> doSomethingElseUsingCf(value, 2))
              .thenHandle((result, cause) -> dispatchQueue.resume());
        }, dispatchQueue);
  }

  /**
   * Shoes how to use {@link EnhancedCompletableFuture} to execute a step, in-line mid process, that
   * throws an exception that should be used to complete the future directly.
   */
  public PnkyPromise<Void> enhancedCompletableFutureInlineExceptionalProcessExampleMethod()
  {
    return doSomethingUsingCf()
        .thenTransform((result) ->
        {
          if (result == 1)
          {
            doSomethingExceptional();
          }

          return null;
        });
  }

}
