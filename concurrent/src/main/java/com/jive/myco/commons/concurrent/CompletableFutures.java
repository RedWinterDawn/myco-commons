package com.jive.myco.commons.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.NonNull;

/**
 * A utility class providing helper methods for working with {@link CompletableFuture}s.
 * <p>
 * <b>A note on exceptional completion via "Exceptionally" methods</b> <br>
 * When the resultant {@link CompletableFuture} completes exceptionally, the thrown exception is not
 * wrapped in a {@link CompletionException} as is the case for the non-"Exceptionally" variants of
 * the methods found on {@code CompletableFuture}.
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
   * @param <U>
   *          the result type
   *
   * @return a new completed {@code CompletableFuture}
   */
  public static <U> CompletableFuture<U> immediatelyComplete(final U result)
  {
    return CompletableFuture.completedFuture(result);
  }

  /**
   * Returns a new {@code CompletableFuture} that is completed exceptionally with the cause.
   *
   * @param cause
   *          the cause to exceptionally complete the returned future with
   * @param <U>
   *          the result type
   *
   * @return a new exceptionally completed {@code CompletableFuture}
   */
  public static <U> CompletableFuture<U> immediatelyFailed(final Throwable cause)
  {
    final CompletableFuture<U> completeMe = new CompletableFuture<>();
    completeMe.completeExceptionally(cause);

    return completeMe;
  }

  // TODO
  public static <T> CompletableFuture<T> compose(
      @NonNull final Supplier<CompletionStage<T>> supplier)
  {
    return supply(supplier)
        .thenCompose((x) -> x);
  }

  // TODO
  public static <T> CompletableFuture<T> composeAsync(
      @NonNull final Supplier<CompletionStage<T>> supplier)
  {
    return CompletableFuture.supplyAsync(supplier)
        .thenCompose((x) -> x);
  }

  // TODO
  public static <T> CompletableFuture<T> composeAsync(
      @NonNull final Supplier<CompletionStage<T>> supplier,
      @NonNull final Executor executor)
  {
    return CompletableFuture.supplyAsync(supplier, executor)
        .thenCompose((x) -> x);
  }

  /**
   * Returns a new {@code CompletableFuture} that is asynchronously completed by a task running in
   * the {@link ForkJoinPool#commonPool()} after it runs the given {@link ExceptionalRunnable
   * action}.
   * <p>
   * See the {@link CompletableFutures} documentation for special notes on exceptional completion.
   *
   * @param action
   *          the action to run before completing the returned {@code CompletableFuture}
   * @param <U>
   *          the result type
   *
   * @return the new {@code CompletableFuture}
   */
  public static CompletableFuture<Void> runExceptionallyAsync(final ExceptionalRunnable action)
  {
    return doRunExceptionally(action, ForkJoinPool.commonPool());
  }

  /**
   * Returns a new {@code CompletableFuture} that is asynchronously completed by a task running in
   * the supplied {@code Executor} after it runs the given {@code Runnable}.
   * <p>
   * See the {@link CompletableFutures} documentation for special notes on exceptional completion.
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

  // TODO
  protected static <U> CompletableFuture<U> supply(@NonNull final Supplier<U> supplier)
  {
    final CompletableFuture<U> completeMe = new CompletableFuture<>();

    try
    {
      completeMe.complete(supplier.get());
    }
    catch (final Throwable t)
    {
      completeMe.completeExceptionally(new CompletionException(t));
    }

    return completeMe;
  }

  /**
   * Returns a new {@code CompletableFuture} that is asynchronously completed by a task running in
   * the {@link ForkJoinPool#commonPool()} with the value obtained by calling the given
   * {@link ExceptionalSupplier supplier}.
   * <p>
   * See the {@link CompletableFutures} documentation for special notes on exceptional completion.
   *
   * @param supplier
   *          a function returning the value to be used to complete the returned
   *          {@code EnhancedCompletableFuture}
   * @param <U>
   *          the suppliers's return type
   *
   * @return the new {@code CompletableFuture}
   */
  public static <U> CompletableFuture<U> supplyExceptionallyAsync(
      @NonNull final ExceptionalSupplier<U> supplier)
  {
    return doSupplyExceptionally(supplier, ForkJoinPool.commonPool());
  }

  /**
   * Returns a new {@code CompletableFuture} that is asynchronously completed by a task running in
   * the supplied {@code Executor} with the value obtained by calling the given
   * {@link ExceptionalSupplier supplier}.
   * <p>
   * See the {@link CompletableFutures} documentation for special notes on exceptional completion.
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
  public static <U> CompletableFuture<U> supplyExceptionallyAsync(
      @NonNull final ExceptionalSupplier<U> supplier,
      @NonNull final Executor executor)
  {
    return doSupplyExceptionally(supplier, executor);
  }

  /**
   * Returns a {@code Function} producing a new {@code CompletableFuture} that is completed,
   * synchronously on the returned function's invoking thread, by the {@link ExceptionalFunction},
   * {@code efn}.
   * <p>
   * See the {@link CompletableFutures} documentation for special notes on exceptional completion.
   * <p>
   * This method simplifies the handling of chained asynchronous calls leveraging
   * {@code CompletableFuture} in the case where the function executed by the stage may throw a
   * checked exception. For example:
   *
   * <pre>
   * CompletableFuture
   *     // Start off doing something asynchronously to get some thing.
   *     .supplyAsync(SomeClass::createInstanceOfSomeThing)
   *     // Then apply a function on the completion thread of the prior stage to turn some thing into
   *     // stuff.
   *     .thenApply(SomeThingClass::transformToStuff)
   *     // Then apply some function on stuff, on a different thread from that of the
   *     // completion thread of the prior stage, that may throw a checked exception.
   *     .thenComposeAsync(
   *         CompletableFutures.thenApplyExceptionally(
   *             StuffClass::FunctionThatMightThrowACheckedException));
   * </pre>
   *
   * @param fn
   *          the function to use to compute the value of the returned CompletionStage
   * @param <T>
   *          the function's input type
   * @param <U>
   *          the function's return type
   *
   * @return the new CompletionStage
   */
  public static <T, U> Function<T, CompletionStage<U>> thenApplyExceptionally(
      final ExceptionalFunction<? super T, ? extends U> efn)
  {
    return (input) ->
    {
      final CompletableFuture<U> completeMe = new CompletableFuture<>();

      try
      {
        completeMe.complete(efn.apply(input));
      }
      catch (final Exception e)
      {
        completeMe.completeExceptionally(e);
      }

      return completeMe;
    };
  }

  /**
   * Returns a {@code Function} producing a new {@code CompletableFuture} that is completed,
   * synchronously on the returned function's invoking thread, by the {@link ExceptionalConsumer},
   * {@code action}.
   * <p>
   * See the {@link CompletableFutures} documentation for special notes on exceptional completion.
   * <p>
   * This method simplifies the handling of chained asynchronous calls leveraging
   * {@code CompletableFuture} in the case where the action executed by the stage may throw a
   * checked exception. For example:
   *
   * <pre>
   * CompletableFuture
   *     // Start off doing something asynchronously to get some thing.
   *     .supplyAsync(SomeClass::createInstanceOfSomeThing)
   *     // Then apply a function on the completion thread of the prior stage to turn some thing into
   *     // stuff.
   *     .thenApply(SomeThingClass::transformToStuff)
   *     // Then apply some consumer on stuff, on a different thread from that of the
   *     // completion thread of the prior stage, that may throw a checked exception.
   *     .thenComposeAsync(
   *         CompletableFutures.thenAcceptExceptionally(
   *             (stuff) -> throw new CheckedException(stuff.toString()));
   * </pre>
   *
   * @param action
   *          the action to perform before completing the {@code CompletionStage} created by the
   *          returned function
   * @param <T>
   *          the consumers's input type
   *
   * @return the new CompletionStage
   */
  public static <T> Function<T, CompletionStage<Void>> thenAcceptExceptionally(
      final ExceptionalConsumer<? super T> consumer)
  {
    return (input) ->
    {
      final CompletableFuture<Void> completeMe = new CompletableFuture<>();

      try
      {
        consumer.accept(input);
        completeMe.complete(null);
      }
      catch (final Exception e)
      {
        completeMe.completeExceptionally(e);
      }

      return completeMe;
    };
  }

  /**
   * Returns a new {@code CompletableFuture} that is synchronously completed in the calling thread
   * after it runs the given {@link ExceptionalRunnable action}.
   * <p>
   * See the {@link CompletableFutures} documentation for special notes on exceptional completion.
   *
   * @param action
   *          the action to run before completing the returned {@code CompletableFuture}
   *
   * @return the new {@code CompletableFuture}
   */
  static <T> Function<T, CompletionStage<Void>> thenRunExceptionally(
      @NonNull final ExceptionalRunnable action)
  {
    return (input) ->
    {
      final CompletableFuture<Void> completeMe = new CompletableFuture<>();

      try
      {
        action.run();
        completeMe.complete(null);
      }
      catch (final Exception e)
      {
        completeMe.completeExceptionally(e);
      }

      return completeMe;
    };
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
