package com.jive.myco.commons.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Function;

import lombok.NonNull;

/**
 * An enhanced version of {@link CompletableFuture} that provides additional capabilities beyond
 * those defined in {@link CompletionStage} and the utility methods on {@link CompletableFuture}.
 * This implementation follows the completion and exceptional completion rules defined in
 * {@link CompletionStage}.
 *
 * @param <T>
 *          the result type of the future
 *
 * @author David Valeri
 * @author Brandon Pederson
 *
 * @see CompletionStage
 */
public class EnhancedCompletableFuture<T> implements EnhancedCompletionStage<T>
{
  private final CompletableFuture<T> delegate;

  private EnhancedCompletableFuture()
  {
    this(new CompletableFuture<>());
  }

  private EnhancedCompletableFuture(@NonNull final CompletableFuture<T> delegate)
  {
    this.delegate = delegate;
  }

  /**
   * Creates a new instance from the provided {@link CompletionStage}.
   *
   * @param completionStage
   *          the completion stage to convert
   *
   * @return a new {@code EnhancedCompletableFuture} based on the provided {@code CompletionStage}
   */
  public static <T> EnhancedCompletableFuture<T> from(final CompletionStage<T> completionStage)
  {
    return new EnhancedCompletableFuture<>(completionStage.toCompletableFuture());
  }

  /**
   * Creates a new instance from the provided {@link CompletionStage}.
   *
   * @param completableFuture
   *          the completable future to convert
   *
   * @return a new {@code EnhancedCompletableFuture} based on the provided {@code CompletableFuture}
   */
  public static <T> EnhancedCompletableFuture<T> from(final CompletableFuture<T> completableFuture)
  {
    return new EnhancedCompletableFuture<>(completableFuture);
  }

  /**
   * Returns a new {@code EnhancedCompletableFuture} that is complete with the provided result.
   *
   * @param result
   *          the result value to complete the returned future with
   *
   * @return a new completed {@code EnhancedCompletableFuture}
   */
  public static <T> EnhancedCompletableFuture<T> immediatelyComplete(final T result)
  {
    return from(CompletableFutures.immediatelyComplete(result));
  }

  /**
   * Returns a new {@code EnhancedCompletableFuture} that is completed exceptionally with the cause.
   *
   * @param cause
   *          the cause to exceptionally complete the returned future with
   *
   * @return a new exceptionally completed {@code EnhancedCompletableFuture}
   */
  public static <T> EnhancedCompletableFuture<T> immediatelyFailed(final Throwable cause)
  {
    return from(CompletableFutures.immediatelyFailed(cause));
  }

  /**
   * Returns a new {@code EnhancedCompletableFuture} that is asynchronously completed by a task
   * running in the {@link ForkJoinPool#commonPool()} after it runs the given
   * {@link ExceptionalRunnable action}. When the action completes by throwing an exception, the
   * returned {@code EnhancedCompletableFuture} completes exceptionally also using the
   * {@link ForkJoinPool#commonPool()}.
   *
   * @param action
   *          the action to run before completing the returned {@code EnhancedCompletionStage}
   *
   * @return the new {@code EnhancedCompletableFuture}
   */
  public static EnhancedCompletableFuture<Void> runExceptionallyAsync(
      final ExceptionalRunnable action)
  {
    return from(CompletableFutures.runExceptionallyAsync(action));
  }

  /**
   * Returns a new {@code EnhancedCompletableFuture} that is asynchronously completed by a task
   * running in the supplied {@code Executor} after it runs the given {@link ExceptionalRunnable
   * action}. When the action completes by throwing an exception, the returned
   * {@code EnhancedCompletableFuture} completes exceptionally also using the supplied
   * {@code Executor}.
   *
   * @param action
   *          the action to run before completing the returned {@code EnhancedCompletionStage}
   * @param executor
   *          the executor used to execute {@code runnable}
   *
   * @return the new {@code EnhancedCompletableFuture}
   */
  public static EnhancedCompletableFuture<Void> runExceptionallyAsync(
      final ExceptionalRunnable action, final Executor executor)
  {
    return from(CompletableFutures.runExceptionallyAsync(action, executor));
  }

  /**
   * Returns a new {@code EnhancedCompletableFuture} that is asynchronously completed by a task
   * running in the {@link ForkJoinPool#commonPool()} with the value obtained by calling the given
   * {@link ExceptionalSupplier supplier}. When the given {@link ExceptionalSupplier supplier}
   * completes by throwing an exception, the returned {@code EnhancedCompletableFuture} completes
   * exceptionally also using the {@link ForkJoinPool#commonPool()}.
   *
   * @param supplier
   *          a function returning the value to be used to complete the returned
   *          {@code EnhancedCompletableFuture}
   * @param <U>
   *          the suppliers's return type
   *
   * @return the new {@code EnhancedCompletableFuture}
   */
  public static <T> EnhancedCompletableFuture<T> supplyExceptionallyAsync(
      @NonNull final ExceptionalSupplier<T> supplier)
  {
    return from(CompletableFutures.supplyExceptionallyAsync(supplier));
  }

  /**
   * Returns a new {@code EnhancedCompletableFuture} that is asynchronously completed by a task
   * running in the supplied {@code Executor} with the value obtained by calling the given
   * {@link ExceptionalSupplier supplier}. When the given {@link ExceptionalSupplier supplier}
   * completes by throwing an exception, the returned {@code EnhancedCompletableFuture} completes
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
   * @return the new {@code EnhancedCompletableFuture}
   */
  public static <T> EnhancedCompletableFuture<T> supplyExceptionallyAsync(
      @NonNull final ExceptionalSupplier<T> supplier,
      @NonNull final Executor executor)
  {
    return from(CompletableFutures.supplyExceptionallyAsync(supplier, executor));
  }

  @Override
  public <U> EnhancedCompletionStage<U> thenApply(final Function<? super T, ? extends U> fn)
  {
    return from(delegate.thenApply(fn));
  }

  @Override
  public <U> EnhancedCompletionStage<U> thenApplyAsync(final Function<? super T, ? extends U> fn)
  {
    return from(delegate.thenApplyAsync(fn));
  }

  @Override
  public <U> EnhancedCompletionStage<U> thenApplyAsync(final Function<? super T, ? extends U> fn,
      final Executor executor)
  {
    return from(delegate.thenApplyAsync(fn, executor));
  }

  @Override
  public <U> EnhancedCompletionStage<U> thenApplyExceptionally(
      final ExceptionalFunction<? super T, ? extends U> efn)
  {
    return from(delegate.thenCompose(
        (input) -> CompletableFutures.applyExceptionally(input, efn)));
  }

  @Override
  public <U> EnhancedCompletionStage<U> thenApplyExceptionallyAsync(
      final ExceptionalFunction<? super T, ? extends U> efn)
  {
    return from(delegate.thenComposeAsync(
        (input) -> CompletableFutures.applyExceptionally(input, efn)));
  }

  @Override
  public <U> EnhancedCompletionStage<U> thenApplyExceptionallyAsync(
      final ExceptionalFunction<? super T, ? extends U> efn, final Executor executor)
  {
    return from(delegate.thenComposeAsync(
        (input) -> CompletableFutures.applyExceptionally(input, efn),
        executor));
  }

  @Override
  public EnhancedCompletionStage<Void> thenAccept(final Consumer<? super T> action)
  {
    return from(delegate.thenAccept(action));
  }

  @Override
  public EnhancedCompletionStage<Void> thenAcceptAsync(final Consumer<? super T> action)
  {
    return from(delegate.thenAcceptAsync(action));
  }

  @Override
  public EnhancedCompletionStage<Void> thenAcceptAsync(final Consumer<? super T> action,
      final Executor executor)
  {
    return from(delegate.thenAcceptAsync(action, executor));
  }

  @Override
  public EnhancedCompletionStage<Void> thenAcceptExceptionally(
      final ExceptionalConsumer<? super T> action)
  {
    return from(delegate.thenCompose(
        (input) -> CompletableFutures.acceptExceptionally(input, action)));
  }

  @Override
  public EnhancedCompletionStage<Void> thenAcceptExceptionallyAsync(
      final ExceptionalConsumer<? super T> action)
  {
    return from(delegate.thenComposeAsync(
        (input) -> CompletableFutures.acceptExceptionally(input, action)));
  }

  @Override
  public EnhancedCompletionStage<Void> thenAcceptExceptionallyAsync(
      final ExceptionalConsumer<? super T> action, final Executor executor)
  {
    return from(delegate.thenComposeAsync(
        (input) -> CompletableFutures.acceptExceptionally(input, action),
        executor));
  }

  @Override
  public EnhancedCompletionStage<Void> thenRun(final Runnable action)
  {
    return from(delegate.thenRun(action));
  }

  @Override
  public EnhancedCompletionStage<Void> thenRunAsync(final Runnable action)
  {
    return from(delegate.thenRunAsync(action));
  }

  @Override
  public EnhancedCompletionStage<Void> thenRunAsync(final Runnable action, final Executor executor)
  {
    return from(delegate.thenRunAsync(action, executor));
  }

  @Override
  public EnhancedCompletionStage<Void> thenRunExceptionally(final ExceptionalRunnable action)
  {
    return from(delegate.thenCompose((input) -> CompletableFutures.runExceptionally(action)));
  }

  @Override
  public EnhancedCompletionStage<Void> thenRunExceptionallyAsync(final ExceptionalRunnable action)
  {
    return from(delegate.thenComposeAsync((input) -> CompletableFutures.runExceptionally(action)));
  }

  @Override
  public EnhancedCompletionStage<Void> thenRunExceptionallyAsync(final ExceptionalRunnable action,
      final Executor executor)
  {
    return from(delegate.thenComposeAsync(
        (input) -> CompletableFutures.runExceptionally(action),
        executor));
  }
}
