package com.jive.myco.commons.concurrent;

import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.fusesource.hawtdispatch.DispatchQueue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.Runnables;

/**
 *
 * @author David Valeri
 */
public class EnhancedCompletableFutureTest
{

  private ExecutorService executor;
  private DispatchQueue dispatchQueue;

  @Before
  public void setup()
  {
    // TODO should use a test executor here that can validate that the executor was used.

    executor = Executors.newSingleThreadExecutor();

  }

  @After
  public void teadown()
  {
    executor.shutdownNow();
  }

  // ///////////////////////////

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

  /**
   * Shows how to use {@link CompletableFutures} to execute a step that throws an exception that
   * should be used to complete the future directly.
   */
  public CompletionStage<Void> completableFuturesNewExceptionalProcessExampleMethod()
  {
    return CompletableFutures.runExceptionallyAsync(() -> doSomethingExceptional(), executor);
  }

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

  // ///////////////////////////

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

  // ////////////////////////////

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

  // //////////////////////////

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

  // //////////////////////////

  // TODO test the rest

  @Test
  public void testAccept() throws Exception
  {
    final EnhancedCompletableFuture<Integer> future =
        EnhancedCompletableFuture.immediatelyComplete(-1);

    final AtomicInteger value = new AtomicInteger();

    future
        .thenAccept((result) -> value.set(result))
        .toCompletableFuture()
        .get();

    assertEquals(-1, value.get());

    value.set(0);

    future
        .thenAcceptAsync((result) -> value.set(result))
        .toCompletableFuture()
        .get();

    assertEquals(-1, value.get());

    value.set(0);

    future
        .thenAcceptAsync((result) -> value.set(result), executor)
        .toCompletableFuture()
        .get();

    assertEquals(-1, value.get());
  }

  @Test
  public void testAcceptExceptionally() throws Exception
  {
    final EnhancedCompletableFuture<Integer> future =
        EnhancedCompletableFuture.immediatelyComplete(-1);

    final AtomicInteger value = new AtomicInteger();

    future
        .thenAccept((result) -> value.set(result))
        .toCompletableFuture()
        .get();

    assertEquals(-1, value.get());

    value.set(0);

    future
        .thenAcceptAsync((result) -> value.set(result))
        .toCompletableFuture()
        .get();

    assertEquals(-1, value.get());

    value.set(0);

    future
        .thenAcceptAsync((result) -> value.set(result), executor)
        .toCompletableFuture()
        .get();

    assertEquals(-1, value.get());

    value.set(0);

    try
    {
      future
          .thenAcceptExceptionally((result) ->
          {
            throw new TestException();
          })
          .toCompletableFuture()
          .get();

      fail();
    }
    catch (final ExecutionException e)
    {
      assertEquals(TestException.class, e.getCause().getClass());
    }

    try
    {
      future
          .thenAcceptExceptionallyAsync((result) ->
          {
            throw new TestException();
          })
          .toCompletableFuture()
          .get();

      fail();
    }
    catch (final ExecutionException e)
    {
      assertEquals(TestException.class, e.getCause().getClass());
    }

    try
    {
      future
          .thenAcceptExceptionallyAsync((result) ->
          {
            throw new TestException();
          }, executor)
          .toCompletableFuture()
          .get();

      fail();
    }
    catch (final ExecutionException e)
    {
      assertEquals(TestException.class, e.getCause().getClass());
    }
  }

  @Test
  public void testThenCombine() throws Exception
  {
    final EnhancedCompletableFuture<Integer> other =
        EnhancedCompletableFuture.immediatelyComplete(1);

    final EnhancedCompletableFuture<Integer> future =
        EnhancedCompletableFuture.immediatelyComplete(0);

    assertTrue(future
        .thenCombine(other, (f, o) -> f == 0 && o.equals(1))
        .toCompletableFuture()
        .get());

    assertTrue(future
        .thenCombineAsync(other, (f, o) -> f == 0 && o.equals(1))
        .toCompletableFuture()
        .get());

    assertTrue(future
        .thenCombineAsync(
            other,
            (f, o) -> f == 0 && o.equals(1),
            executor)
        .toCompletableFuture()
        .get());
  }
}
