package com.jive.myco.commons.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.NonNull;

/**
 * An enhanced version of {@link CompletableFuture} that provides additional capabilities beyond
 * those defined in {@link CompletionStage} and the utility methods on {@link CompletableFuture}.
 * This implementation follows the completion and exceptional completion rules defined in
 * {@link EnhancedCompletionStage}.
 *
 * @param <T>
 *          the result type of the future
 *
 * @author David Valeri
 * @author Brandon Pederson
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

  private static <T> Function<CompletionStage<T>, CompletionStage<T>> identity()
  {
    return (t) -> t;
  }

  // TODO
  public static <T> EnhancedCompletableFuture<T> compose(
      @NonNull final Supplier<CompletionStage<T>> supplier)
  {
    return supply(supplier)
        .thenCompose((x) -> x);
  }

  // TODO
  public static <T> EnhancedCompletableFuture<T> composeAsync(
      @NonNull final Supplier<CompletionStage<T>> supplier)
  {
    return supplyAsync(supplier)
        .thenCompose((x) -> x);
  }

  // TODO
  public static <T> EnhancedCompletableFuture<T> composeAsync(
      @NonNull final Supplier<CompletionStage<T>> supplier,
      @NonNull final Executor executor)
  {
    return supplyAsync(supplier, executor)
        .thenCompose((x) -> x);
  }

  // TODO JavaDoc
  public static EnhancedCompletableFuture<Void> runAsync(final Runnable action)
  {
    return from(CompletableFuture.runAsync(action));
  }

  // TODO JavaDoc
  public static EnhancedCompletableFuture<Void> runAsync(final Runnable action,
      final Executor executor)
  {
    return from(CompletableFuture.runAsync(action, executor));
  }

  /**
   * Returns a new {@code EnhancedCompletableFuture} that is asynchronously completed by a task
   * running in the {@link ForkJoinPool#commonPool()} after it runs the given
   * {@link ExceptionalRunnable action}. When the action completes by throwing an exception, the
   * returned {@code EnhancedCompletableFuture} completes exceptionally with the thrown exception.
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
   * {@code EnhancedCompletableFuture} completes exceptionally with the thrown exception.
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

  // TODO
  protected static <T> EnhancedCompletableFuture<T> supply(
      @NonNull final Supplier<T> supplier)
  {
    return from(CompletableFutures.supply(supplier));
  }

  // TODO
  public static <T> EnhancedCompletableFuture<T> supplyAsync(
      @NonNull final Supplier<T> supplier)
  {
    return from(CompletableFuture.supplyAsync(supplier));
  }

  // TODO
  public static <T> EnhancedCompletableFuture<T> supplyAsync(
      @NonNull final Supplier<T> supplier,
      @NonNull final Executor executor)
  {
    return from(CompletableFuture.supplyAsync(supplier, executor));
  }

  /**
   * Returns a new {@code EnhancedCompletableFuture} that is asynchronously completed by a task
   * running in the {@link ForkJoinPool#commonPool()} with the value obtained by calling the given
   * {@link ExceptionalSupplier supplier}. When the given {@link ExceptionalSupplier supplier}
   * completes by throwing an exception, the returned {@code EnhancedCompletableFuture} completes
   * exceptionally with the thrown exception.
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
   * exceptionally with the thrown exception.
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
  public <U> EnhancedCompletableFuture<U> thenApply(final Function<? super T, ? extends U> fn)
  {
    return from(delegate.thenApply(fn));
  }

  @Override
  public <U> EnhancedCompletableFuture<U> thenApplyAsync(final Function<? super T, ? extends U> fn)
  {
    return from(delegate.thenApplyAsync(fn));
  }

  @Override
  public <U> EnhancedCompletableFuture<U> thenApplyAsync(final Function<? super T, ? extends U> fn,
      final Executor executor)
  {
    return from(delegate.thenApplyAsync(fn, executor));
  }

  @Override
  public <U> EnhancedCompletableFuture<U> thenApplyExceptionally(
      final ExceptionalFunction<? super T, ? extends U> efn)
  {
    return from(delegate.thenCompose(CompletableFutures.thenApplyExceptionally(efn)));
  }

  @Override
  public <U> EnhancedCompletableFuture<U> thenApplyExceptionallyAsync(
      final ExceptionalFunction<? super T, ? extends U> efn)
  {
    return from(delegate.thenComposeAsync(CompletableFutures.thenApplyExceptionally(efn)));
  }

  @Override
  public <U> EnhancedCompletableFuture<U> thenApplyExceptionallyAsync(
      final ExceptionalFunction<? super T, ? extends U> efn, final Executor executor)
  {
    return from(delegate.thenComposeAsync(
        CompletableFutures.thenApplyExceptionally(efn),
        executor));
  }

  @Override
  public EnhancedCompletableFuture<Void> thenAccept(final Consumer<? super T> action)
  {
    return from(delegate.thenAccept(action));
  }

  @Override
  public EnhancedCompletableFuture<Void> thenAcceptAsync(final Consumer<? super T> action)
  {
    return from(delegate.thenAcceptAsync(action));
  }

  @Override
  public EnhancedCompletableFuture<Void> thenAcceptAsync(final Consumer<? super T> action,
      final Executor executor)
  {
    return from(delegate.thenAcceptAsync(action, executor));
  }

  @Override
  public EnhancedCompletableFuture<Void> thenAcceptExceptionally(
      final ExceptionalConsumer<? super T> action)
  {
    return from(delegate.thenCompose(CompletableFutures.thenAcceptExceptionally(action)));
  }

  @Override
  public EnhancedCompletableFuture<Void> thenAcceptExceptionallyAsync(
      final ExceptionalConsumer<? super T> action)
  {
    return from(delegate.thenComposeAsync(CompletableFutures.thenAcceptExceptionally(action)));
  }

  @Override
  public EnhancedCompletableFuture<Void> thenAcceptExceptionallyAsync(
      final ExceptionalConsumer<? super T> action, final Executor executor)
  {
    return from(delegate.thenComposeAsync(
        CompletableFutures.thenAcceptExceptionally(action),
        executor));
  }

  @Override
  public EnhancedCompletableFuture<Void> thenRun(final Runnable action)
  {
    return from(delegate.thenRun(action));
  }

  @Override
  public EnhancedCompletableFuture<Void> thenRunAsync(final Runnable action)
  {
    return from(delegate.thenRunAsync(action));
  }

  @Override
  public EnhancedCompletableFuture<Void> thenRunAsync(final Runnable action, final Executor executor)
  {
    return from(delegate.thenRunAsync(action, executor));
  }

  @Override
  public EnhancedCompletableFuture<Void> thenRunExceptionally(final ExceptionalRunnable action)
  {
    return from(delegate.thenCompose(CompletableFutures.thenRunExceptionally(action)));
  }

  @Override
  public EnhancedCompletableFuture<Void> thenRunExceptionallyAsync(final ExceptionalRunnable action)
  {
    return from(delegate.thenComposeAsync(CompletableFutures.thenRunExceptionally(action)));
  }

  @Override
  public EnhancedCompletableFuture<Void> thenRunExceptionallyAsync(
      final ExceptionalRunnable action,
      final Executor executor)
  {
    return from(delegate.thenComposeAsync(
        CompletableFutures.thenRunExceptionally(action),
        executor));
  }

  @Override
  public <U, V> EnhancedCompletableFuture<V> thenCombine(final CompletionStage<? extends U> other,
      final BiFunction<? super T, ? super U, ? extends V> fn)
  {
    return from(delegate.thenCombine(other, fn));
  }

  @Override
  public <U, V> EnhancedCompletableFuture<V> thenCombineAsync(
      final CompletionStage<? extends U> other,
      final BiFunction<? super T, ? super U, ? extends V> fn)
  {
    return from(delegate.thenCombineAsync(other, fn));
  }

  @Override
  public <U, V> EnhancedCompletionStage<V> thenCombineAsync(
      final CompletionStage<? extends U> other,
      final BiFunction<? super T, ? super U, ? extends V> fn, final Executor executor)
  {
    return from(delegate.thenCombineAsync(other, fn, executor));
  }

  @Override
  public <U> EnhancedCompletableFuture<Void> thenAcceptBoth(
      final CompletionStage<? extends U> other,
      final BiConsumer<? super T, ? super U> action)
  {
    return from(delegate.thenAcceptBoth(other, action));
  }

  @Override
  public <U> EnhancedCompletableFuture<Void> thenAcceptBothAsync(
      final CompletionStage<? extends U> other, final BiConsumer<? super T, ? super U> action)
  {
    return from(delegate.thenAcceptBothAsync(other, action));
  }

  @Override
  public <U> EnhancedCompletableFuture<Void> thenAcceptBothAsync(
      final CompletionStage<? extends U> other, final BiConsumer<? super T, ? super U> action,
      final Executor executor)
  {
    return from(delegate.thenAcceptBothAsync(other, action, executor));
  }

  @Override
  public EnhancedCompletableFuture<Void> runAfterBoth(final CompletionStage<?> other,
      final Runnable action)
  {
    return from(delegate.runAfterBoth(other, action));
  }

  @Override
  public EnhancedCompletableFuture<Void> runAfterBothAsync(final CompletionStage<?> other,
      final Runnable action)
  {
    return from(delegate.runAfterBothAsync(other, action));
  }

  @Override
  public EnhancedCompletableFuture<Void> runAfterBothAsync(final CompletionStage<?> other,
      final Runnable action, final Executor executor)
  {
    return from(delegate.runAfterBothAsync(other, action, executor));
  }

  @Override
  public <U> EnhancedCompletableFuture<U> applyToEither(final CompletionStage<? extends T> other,
      final Function<? super T, U> fn)
  {
    return from(delegate.applyToEither(other, fn));
  }

  @Override
  public <U> EnhancedCompletableFuture<U> applyToEitherAsync(
      final CompletionStage<? extends T> other, final Function<? super T, U> fn)
  {
    return from(delegate.applyToEitherAsync(other, fn));
  }

  @Override
  public <U> EnhancedCompletableFuture<U> applyToEitherAsync(
      final CompletionStage<? extends T> other, final Function<? super T, U> fn,
      final Executor executor)
  {
    return from(delegate.applyToEitherAsync(other, fn, executor));
  }

  @Override
  public EnhancedCompletableFuture<Void> acceptEither(final CompletionStage<? extends T> other,
      final Consumer<? super T> action)
  {
    return from(delegate.acceptEither(other, action));
  }

  @Override
  public EnhancedCompletableFuture<Void> acceptEitherAsync(
      final CompletionStage<? extends T> other,
      final Consumer<? super T> action)
  {
    return from(delegate.acceptEitherAsync(other, action));
  }

  @Override
  public EnhancedCompletableFuture<Void> acceptEitherAsync(
      final CompletionStage<? extends T> other,
      final Consumer<? super T> action, final Executor executor)
  {
    return from(delegate.acceptEitherAsync(other, action, executor));
  }

  @Override
  public EnhancedCompletableFuture<Void> runAfterEither(final CompletionStage<?> other,
      final Runnable action)
  {
    return from(delegate.runAfterEither(other, action));
  }

  @Override
  public EnhancedCompletableFuture<Void> runAfterEitherAsync(final CompletionStage<?> other,
      final Runnable action)
  {
    return from(delegate.runAfterEitherAsync(other, action));
  }

  @Override
  public EnhancedCompletableFuture<Void> runAfterEitherAsync(final CompletionStage<?> other,
      final Runnable action, final Executor executor)
  {
    return from(delegate.runAfterEitherAsync(other, action, executor));
  }

  @Override
  public <U> EnhancedCompletableFuture<U> thenCompose(
      final Function<? super T, ? extends CompletionStage<U>> fn)
  {
    return from(delegate.thenCompose(fn));
  }

  @Override
  public <U> EnhancedCompletableFuture<U> thenComposeAsync(
      final Function<? super T, ? extends CompletionStage<U>> fn)
  {
    return from(delegate.thenComposeAsync(fn));
  }

  @Override
  public <U> EnhancedCompletableFuture<U> thenComposeAsync(
      final Function<? super T, ? extends CompletionStage<U>> fn, final Executor executor)
  {
    return from(delegate.thenComposeAsync(fn, executor));
  }

  @Override
  public EnhancedCompletableFuture<T> exceptionally(final Function<Throwable, ? extends T> fn)
  {
    return from(delegate.exceptionally(fn));
  }

  @Override
  public EnhancedCompletableFuture<T> whenComplete(
      final BiConsumer<? super T, ? super Throwable> action)
  {
    return from(delegate.whenComplete(action));
  }

  @Override
  public EnhancedCompletableFuture<T> whenCompleteAsync(
      final BiConsumer<? super T, ? super Throwable> action)
  {
    return from(delegate.whenCompleteAsync(action));
  }

  @Override
  public EnhancedCompletableFuture<T> whenCompleteAsync(
      final BiConsumer<? super T, ? super Throwable> action, final Executor executor)
  {
    return from(delegate.whenCompleteAsync(action, executor));
  }

  @Override
  public <U> EnhancedCompletableFuture<U> handle(
      final BiFunction<? super T, Throwable, ? extends U> fn)
  {
    return from(delegate.handle(fn));
  }

  @Override
  public <U> EnhancedCompletableFuture<U> handleAsync(
      final BiFunction<? super T, Throwable, ? extends U> fn)
  {
    return from(delegate.handleAsync(fn));
  }

  @Override
  public <U> EnhancedCompletableFuture<U> handleAsync(
      final BiFunction<? super T, Throwable, ? extends U> fn, final Executor executor)
  {
    return from(delegate.handleAsync(fn, executor));
  }

  @Override
  public CompletableFuture<T> toCompletableFuture()
  {
    final CompletableFuture<T> completeMe = new CompletableFuture<>();

    whenComplete((result, cause) ->
    {
      if (cause == null)
      {
        completeMe.complete(result);
      }
      else
      {
        completeMe.completeExceptionally(cause);
      }
    });

    return completeMe;
  }
}
