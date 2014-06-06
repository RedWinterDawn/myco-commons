package com.jive.myco.commons.concurrent;

import java.util.concurrent.Executor;

import org.fusesource.hawtdispatch.DispatchQueue;

/**
 * Example that shows different usage scenarios for a {@link PnkyPromise}.
 *
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public class PnkyExamples
{
  private Executor executor;
  private DispatchQueue dispatchQueue;

  /**
   * Shows how to use {@link PnkyPromise} to execute a step that throws an exception that
   * should be used to complete the future directly.
   */
  public PnkyPromise<Void> newExceptionalProcessExampleMethod()
  {
    final Pnky<Void> pnky = Pnky.create();

    executor.execute(() ->
    {
      try
      {
        doSomethingExceptional();
        pnky.resolve(null);
      }
      catch (final Exception e)
      {
        pnky.reject(e);
      }
    });

    return pnky;
  }

  /**
   * Shows an alternate example of how to use {@link PnkyPromise} to execute a step that
   * throws an exception that should be used to complete the future with an error directly.
   */
  public PnkyPromise<Void> newExceptionalProcessExampleAlternateMethod()
  {
    return Pnky
        .runAsync(() -> {}, executor)
        .thenCompose((input) ->
        {
          final Pnky<Void> future = Pnky.create();

          try
          {
            doSomethingExceptional();
            future.resolve(null);
          }
          catch (final Exception e)
          {
            future.reject(e);
          }

          return future;
        });
  }

  /**
   * Represents some action that throws an exception.
   */
  public void doSomethingExceptional() throws Exception
  {
  }

  /**
   * Shows how to use {@link PnkyPromise} to execute a step that throws an exception that
   * should be used to complete the future either with success or error depending on the outcome
   * of the exceptional method.
   */
  public PnkyPromise<Void> exceptionalPromiseThatJustCallsAMethodToRun()
  {
    return Pnky.runAsync(this::doSomethingExceptional, executor);
  }

  /**
   * Shoes how to use {@link PnkyPromise} to execute a step, in-line mid process, that throws
   * an exception that should be used to complete the future directly.
   */
  public PnkyPromise<Void> inlineExceptionalProcessCompleteDirectlyExampleMethod()
  {
    return doSomethingUsingPromise()
        .thenCompose((result) ->
        {
          final Pnky<Void> future = Pnky.create();

          if (result == 1)
          {
            try
            {
              doSomethingExceptional();
              future.resolve(null);
            }
            catch (final Exception e)
            {
              future.reject(e);
            }
          }

          return future;
        });
  }

  public PnkyPromise<Integer> doSomethingUsingPromise()
  {
    return Pnky.supplyAsync(() -> 1, executor);
  }

  /**
   * Shoes how to use {@link PnkyPromise} to execute a step, in-line mid process, that throws
   * an exception that should be used to complete the future via transform.
   */
  public PnkyPromise<Void> inlineExceptionalProcessExampleTransformMethod()
  {
    return doSomethingUsingPromise()
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
   * Shows how to use {@link PnkyPromise} to execute an asynchronous process while suspending a
   * {@link DispatchQueue}.
   *
   * A HawtDispatch DispatchQueue is often used to protect resource state without the need for locks
   * or synchronization. A common pattern is to protect state by starting a process on a
   * DispatchQueue, suspending the queue before launching additional asynchronous processes, and
   * then resuming the queue when the additional processes complete.
   *
   * The following examples demonstrate how to suspend a DispatchQueue while performing additional
   * asynchronous operations using only PnkyPromise.
   */
  public PnkyPromise<String> queueSuspendExampleMethod()
  {
    final Pnky<String> future = Pnky.create();

    dispatchQueue.execute(() ->
    {
      dispatchQueue.suspend();

      doSomethingUsingPromise()
          .thenCompose((value) -> doSomethingElseUsingPromise(value, 2))
          .alwaysAccept((result, error) ->
          {
            dispatchQueue.resume();

            if (error != null)
            {
              future.reject(error);
            }
            else
            {
              future.resolve(result);
            }
          });
    });

    return future;
  }

  /**
   * Shows an alternate example of how to use {@link PnkyPromise} to execute an asynchronous process
   * while suspending a {@link DispatchQueue}, while creating the initial promise on an executor
   * initially and then trans
   */
  public PnkyPromise<String> completableFuturesQueueSuspendExampleMethod()
  {
    return Pnky
        .composeAsync(() ->
        {
          dispatchQueue.suspend();

          return doSomethingUsingPromise()
              .thenCompose((value) -> doSomethingElseUsingPromise(value, 2))
              .alwaysAccept((result, cause) -> dispatchQueue.resume());
        }, dispatchQueue);
  }

  public PnkyPromise<String> doSomethingElseUsingPromise(final Integer value, final int radix)
  {
    return Pnky.supplyAsync(() -> Integer.toString(value, radix), executor);
  }
}
