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
   * Shows how to use {@link PnkyPromise} to execute a step that throws an exception that can be
   * used to complete the future exceptionally.
   * <p>
   * This approach is the most verbose.
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
   * Shows an alternate, and more concise, example of how to use {@link PnkyPromise} to execute a
   * step that throws an exception that can be used to complete the future exceptionally.
   */
  public PnkyPromise<Void> newExceptionalProcessExampleAlternateMethod()
  {
    return Pnky.runAsync(() -> doSomethingExceptional(), executor);
  }

  /**
   * Shows an alternate, and even more concise, example of how to use {@link PnkyPromise} to execute
   * a step that throws an exception that can be used to complete the future exceptionally.
   */
  public PnkyPromise<Void> newExceptionalProcessExampleAlternateMethod2()
  {
    return Pnky.runAsync(this::doSomethingExceptional, executor);
  }

  /**
   * Shows how to use {@link PnkyPromise} to execute a step, in-line mid process, that throws an
   * exception that should be handled manually to complete the future.
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

  /**
   * Shoes how to use {@link PnkyPromise} to execute transformational and compositional steps,
   * in-line mid process, with a fallback step to handle exceptional completion.
   */
  public PnkyPromise<String> inlineProcessExampleTransformMethod()
  {
    return doSomethingUsingPromise()
        .thenTransform((result) ->
        {
          if (result == 1)
          {
            doSomethingExceptional();

            return 0;
          }
          else
          {
            return result;
          }
        })
        .thenCompose((value) -> doSomethingElseUsingPromise(value, 2))
        .composeFallback((cause) -> Pnky.immediatelyFailed(new IllegalStateException(cause)));
  }

  /**
   * Shows how to use {@link PnkyPromise} to execute an asynchronous process while suspending a
   * {@link DispatchQueue}.
   * <p>
   * A HawtDispatch DispatchQueue is often used to protect resource state without the need for locks
   * or synchronization. A common pattern is to protect state by starting a process on a
   * DispatchQueue, suspending the queue before launching additional asynchronous processes, and
   * then resuming the queue when the additional processes complete.
   * </p>
   * The following examples demonstrate how to suspend a DispatchQueue while performing additional
   * asynchronous operations using only PnkyPromise.
   */
  public PnkyPromise<String> queueSuspendExampleMethod()
  {
    return Pnky
        .composeAsync(() ->
        {
          dispatchQueue.suspend();

          try
          {
            return doSomethingUsingPromise()
                .thenCompose((value) -> doSomethingElseUsingPromise(value, 2))
                .alwaysAccept((result, cause) -> dispatchQueue.resume());
          }
          catch (final Exception e)
          {
            dispatchQueue.resume();
            throw e;
          }
        }, dispatchQueue);
  }

  /**
   * Represents some action that throws an exception.
   */
  public void doSomethingExceptional() throws Exception
  {
    // No-op
  }

  public PnkyPromise<Integer> doSomethingUsingPromise()
  {
    return Pnky.supplyAsync(() -> 1, executor);
  }

  public PnkyPromise<String> doSomethingElseUsingPromise(final Integer value, final int radix)
  {
    return Pnky.supplyAsync(() -> Integer.toString(value, radix), executor);
  }
}
