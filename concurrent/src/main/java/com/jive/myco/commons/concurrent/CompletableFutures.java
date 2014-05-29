package com.jive.myco.commons.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import lombok.NonNull;

/**
 * A utility class providing helper methods for working with {@link CompletableFuture}s.
 *
 * @author David Valeri
 */
public final class CompletableFutures
{
  // Hidden on utility class.
  private CompletableFutures()
  {
  }

  /**
   * Returns a new {@code CompletableFuture} that is completed with the provided result.
   *
   * @param result
   *          the result value to complete the returned future with
   *
   * @return a new completed {@code CompletableFuture}
   */
  public static <T> CompletableFuture<T> immediatelyComplete(final T result)
  {
    return CompletableFuture.completedFuture(result);
  }

  /**
   * Returns a new {@code CompletableFuture} that is completed exceptionally with the cause.
   *
   * @param cause
   *          the cause to exceptionally complete the returned future with
   *
   * @return a new exceptionally completed {@code CompletableFuture}
   */
  public static <T> CompletableFuture<T> immediatelyFailed(final Throwable cause)
  {
    final CompletableFuture<T> completeMe = new CompletableFuture<>();
    completeMe.completeExceptionally(cause);

    return completeMe;
  }

  /**
   * Returns a new {@code CompletableFuture} that is synchronously completed in the calling thread
   * after it runs the given {@link ExceptionalRunnable action}. When the action completes by
   * throwing an exception, the returned {@code CompletableFuture} completes exceptionally also in
   * the calling thread.
   *
   * @param action
   *          the action to run before completing the returned {@code CompletableFuture}
   *
   * @return the new {@code CompletableFuture}
   */
  public static CompletableFuture<Void> runExceptionally(@NonNull final ExceptionalRunnable action)
  {
    return doRunExceptionally(action, null);
  }

  /**
   * Returns a new {@code CompletableFuture} that is asynchronously completed by a task running in
   * the {@link ForkJoinPool#commonPool()} after it runs the given {@link ExceptionalRunnable
   * action}. When the action completes by throwing an exception, the returned
   * {@code CompletableFuture} completes exceptionally also using the
   * {@link ForkJoinPool#commonPool()}.
   *
   * @param action
   *          the action to run before completing the returned {@code CompletableFuture}
   *
   * @return the new {@code CompletableFuture}
   */
  public static CompletableFuture<Void> runExceptionallyAsync(final ExceptionalRunnable action)
  {
    return doRunExceptionally(action, ForkJoinPool.commonPool());
  }

  /**
   * Returns a new {@code CompletableFuture} that is asynchronously completed by a task running in
   * the supplied {@code Executor} after it runs the given {@code Runnable}. When this action
   * completes by throwing an exception, the returned {@code CompletableFuture} completes
   * exceptionally also using the supplied {@code Executor}.
   *
   * @param action
   *          the action to run before completing the returned {@code CompletableFuture}
   * @param executor
   *          the executor used to execute {@code runnable}
   *
   * @return the new {@code CompletableFuture}
   */
  public static CompletableFuture<Void> runExceptionallyAsync(
      @NonNull final ExceptionalRunnable action, @NonNull final Executor executor)
  {
    return doRunExceptionally(action, executor);
  }

  /**
   * Returns a new {@code CompletableFuture} that is asynchronously completed by a task running in
   * the {@link ForkJoinPool#commonPool()} with the value obtained by calling the given
   * {@link ExceptionalSupplier supplier}. When the given {@link ExceptionalSupplier supplier}
   * completes by throwing an exception, the returned {@code CompletableFuture} completes
   * exceptionally also using the {@link ForkJoinPool#commonPool()}.
   *
   * @param supplier
   *          a function returning the value to be used to complete the returned
   *          {@code EnhancedCompletableFuture}
   * @param <U>
   *          the suppliers's return type
   *
   * @return the new {@code CompletableFuture}
   */
  public static <T> CompletableFuture<T> supplyExceptionallyAsync(
      @NonNull final ExceptionalSupplier<T> supplier)
  {
    return doSupplyExceptionally(supplier, ForkJoinPool.commonPool());
  }

  /**
   * Returns a new {@code CompletableFuture} that is asynchronously completed by a task running in
   * the supplied {@code Executor} with the value obtained by calling the given
   * {@link ExceptionalSupplier supplier}. When the given {@link ExceptionalSupplier supplier}
   * completes by throwing an exception, the returned {@code CompletableFuture} completes
   * exceptionally also using the supplied {@code Executor}.
   *
   * @param supplier
   *          a function returning the value to be used to complete the returned
   *          {@code EnhancedCompletableFuture}
   * @param executor
   *          the executor used to execute {@code supplier}
   * @param <U>
   *          the suppliers's return type
   *
   * @return the new {@code CompletableFuture}
   */
  public static <T> CompletableFuture<T> supplyExceptionallyAsync(
      @NonNull final ExceptionalSupplier<T> supplier,
      @NonNull final Executor executor)
  {
    return doSupplyExceptionally(supplier, executor);
  }

  public static <T, U> CompletableFuture<U> applyExceptionally(final T input,
      final ExceptionalFunction<? super T, ? extends U> efn)
  {
    return doFunctionExceptionally(input, efn, null);
  }

  public static <T> CompletableFuture<Void> acceptExceptionally(final T input,
      final ExceptionalConsumer<? super T> consumer)
  {
    return doConsumeExceptionally(input, consumer, null);
  }

  private static <T> CompletableFuture<T> doSupplyExceptionally(
      final ExceptionalSupplier<T> supplier,
      final Executor executor)
  {
    final CompletableFuture<T> completeMe = new CompletableFuture<>();

    execute(executor, () ->
    {
      try
      {
        completeMe.complete(supplier.get());
      }
      catch (final Exception e)
      {
        completeMe.completeExceptionally(e);
      }
    });

    return completeMe;
  }

  private static CompletableFuture<Void> doRunExceptionally(
      final ExceptionalRunnable runnable, final Executor executor)
  {
    final CompletableFuture<Void> completeMe = new CompletableFuture<>();

    execute(executor, () ->
    {
      try
      {
        runnable.run();
        completeMe.complete(null);
      }
      catch (final Exception e)
      {
        completeMe.completeExceptionally(e);
      }
    });

    return completeMe;
  }

  private static <T, U> CompletableFuture<U> doFunctionExceptionally(
      final T input, final ExceptionalFunction<? super T, ? extends U> efn, final Executor executor)
  {
    final CompletableFuture<U> completeMe = new CompletableFuture<>();

    execute(executor, () ->
    {
      try
      {
        completeMe.complete(efn.apply(input));
      }
      catch (final Exception e)
      {
        completeMe.completeExceptionally(e);
      }
    });

    return completeMe;
  }

  private static <T> CompletableFuture<Void> doConsumeExceptionally(
      final T input,
      final ExceptionalConsumer<T> consumer,
      final Executor executor)
  {
    final CompletableFuture<Void> completeMe = new CompletableFuture<>();

    execute(executor, () ->
    {
      try
      {
        consumer.accept(input);
        completeMe.complete(null);
      }
      catch (final Exception e)
      {
        completeMe.completeExceptionally(e);
      }
    });

    return completeMe;
  }

  /**
   * Executes {@code runnable} on {@code executor} unless {@code executor} is {@code null} or the
   * {@link ForkJoinPool#commonPool() fork join common pool} and the pool is disabled, in which case
   * {@code runnable} is executed on the calling thread or on a new thread, respectively.
   *
   * @param executor
   *          the executor to use
   * @param runnable
   *          the runnable to execute
   */
  private static void execute(final Executor executor,
      @NonNull final Runnable runnable)
  {
    if (executor == null)
    {
      runnable.run();
    }
    else if (executor == ForkJoinPool.commonPool() &&
        ForkJoinPool.getCommonPoolParallelism() <= 1)
    {
      new Thread(runnable).start();
    }
    else
    {
      executor.execute(runnable);
    }
  }
}
