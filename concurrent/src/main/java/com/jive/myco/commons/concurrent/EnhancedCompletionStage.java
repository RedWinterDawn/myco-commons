package com.jive.myco.commons.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.jive.myco.commons.function.ExceptionalConsumer;
import com.jive.myco.commons.function.ExceptionalFunction;
import com.jive.myco.commons.function.ExceptionalRunnable;

/**
 * An enhanced version of {@link CompletionStage} providing value-added operations to reduce
 * boilerplate code and simplify asynchronous processing.
 * <p>
 * Implementations must follow the completion and exceptional completion rules as defined in
 * {@link CompletionStage} with the exception of the "Exceptionally" method variants. When the
 * resultant {@link CompletableFuture} completes exceptionally, the thrown exception is not wrapped
 * in a {@link CompletionException} as is the case for the non-"Exceptionally" variants of the
 * methods found on {@code CompletableFuture}.
 *
 * @author David Valeri
 * @author Brandon Pederson
 *
 * @param <T>
 *          the type of the completion result
 * @deprecated Use {@link PnkyPromise} instead
 */
@Deprecated
public interface EnhancedCompletionStage<T> extends CompletionStage<T>
{
  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  <U> EnhancedCompletionStage<U> thenApply(final Function<? super T, ? extends U> fn);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  <U> EnhancedCompletionStage<U> thenApplyAsync(final Function<? super T, ? extends U> fn);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  <U> EnhancedCompletionStage<U> thenApplyAsync(final Function<? super T, ? extends U> fn,
      final Executor executor);

  /**
   * Returns a new {@code EnhancedCompletionStage} that, when this stage completes normally, is
   * executed with this stage's result as the argument to the supplied {@link ExceptionalFunction
   * function} as in {@link CompletionStage#thenApply(Function)}.
   * <p>
   * See the {@link EnhancedCompletionStage} documentation for special notes on exceptional
   * completion.
   *
   * @param efn
   *          the function to use to compute the value of the returned
   *          {@code EnhancedCompletionStage}
   * @param <U>
   *          the function's return type
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  <U> EnhancedCompletionStage<U> thenApplyExceptionally(
      final ExceptionalFunction<? super T, ? extends U> efn);

  /**
   * Returns a new {@code EnhancedCompletionStage} that, when this stage completes normally, is
   * executed using this stage's default asynchronous execution facility, with this stage's result
   * as the argument to the supplied {@link ExceptionalFunction function} as in
   * {@link CompletionStage#thenApplyAsync(Function)}.
   * <p>
   * See the {@link EnhancedCompletionStage} documentation for special notes on exceptional
   * completion.
   *
   * @param efn
   *          the function to use to compute the value of the returned
   *          {@code EnhancedCompletionStage}
   * @param <U>
   *          the function's return type
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  <U> EnhancedCompletionStage<U> thenApplyExceptionallyAsync(
      final ExceptionalFunction<? super T, ? extends U> efn);

  /**
   * Returns a new {@code EnhancedCompletionStage} that, when this stage completes normally, is
   * executed asynchronously using the supplied {@code Executor} with this stage's result as the
   * argument to the supplied {@link ExceptionalFunction function} as in
   * {@link CompletionStage#thenApplyAsync(Function, Executor)}.
   * <p>
   * See the {@link EnhancedCompletionStage} documentation for special notes on exceptional
   * completion.
   *
   * @param efn
   *          the function to use to compute the value of the returned
   *          {@code EnhancedCompletionStage}
   * @param executor
   *          the executor used to execute {@code efn}
   * @param <U>
   *          the function's return type
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  <U> EnhancedCompletionStage<U> thenApplyExceptionallyAsync(
      final ExceptionalFunction<? super T, ? extends U> efn, final Executor executor);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  EnhancedCompletionStage<Void> thenAccept(final Consumer<? super T> action);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  EnhancedCompletionStage<Void> thenAcceptAsync(final Consumer<? super T> action);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  EnhancedCompletionStage<Void> thenAcceptAsync(final Consumer<? super T> action,
      final Executor executor);

  /**
   * Returns a new {@code EnhancedCompletionStage} that, when this stage completes normally, is
   * executed with this stage's result as the argument to the supplied {@link ExceptionalConsumer
   * action} as in {@link CompletionStage#thenAccept(Consumer)}.
   * <p>
   * See the {@link EnhancedCompletionStage} documentation for special notes on exceptional
   * completion.
   *
   * @param action
   *          the action to perform before completing the returned CompletionStage
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  EnhancedCompletionStage<Void> thenAcceptExceptionally(final ExceptionalConsumer<? super T> action);

  /**
   * Returns a new {@code EnhancedCompletionStage} that, when this stage completes normally, is
   * executed using this stage's default asynchronous execution facility with this stage's result as
   * the argument to the supplied {@link ExceptionalConsumer action} as in
   * {@link CompletionStage#thenAcceptAsync(Consumer)}.
   * <p>
   * See the {@link EnhancedCompletionStage} documentation for special notes on exceptional
   * completion.
   *
   * @param action
   *          the action to perform before completing the returned {@code EnhancedCompletionStage}
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  EnhancedCompletionStage<Void> thenAcceptExceptionallyAsync(
      final ExceptionalConsumer<? super T> action);

  /**
   * Returns a new {@code EnhancedCompletionStage} that, when this stage completes normally, is
   * executed using the supplied {@code Executor} with this stage's result as the argument to the
   * supplied {@link ExceptionalConsumer action} as in
   * {@link CompletionStage#thenAcceptAsync(Consumer, Executor)}.
   * <p>
   * See the {@link EnhancedCompletionStage} documentation for special notes on exceptional
   * completion.
   *
   * @param action
   *          the action to perform before completing the returned {@code EnhancedCompletionStage}
   * @param executor
   *          the executor used to execute {@code action}
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  EnhancedCompletionStage<Void> thenAcceptExceptionallyAsync(
      final ExceptionalConsumer<? super T> action,
      final Executor executor);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  EnhancedCompletionStage<Void> thenRun(final Runnable action);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  EnhancedCompletionStage<Void> thenRunAsync(final Runnable action);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  EnhancedCompletionStage<Void> thenRunAsync(final Runnable action,
      final Executor executor);

  /**
   * Returns a new {@code EnhancedCompletionStage} that, when this stage completes normally,
   * executes the given {@link ExceptionalRunnable action} as in
   * {@link CompletionStage#thenRun(Runnable)}.
   * <p>
   * See the {@link EnhancedCompletionStage} documentation for special notes on exceptional
   * completion.
   *
   * @param action
   *          the action to perform before completing the returned {@code EnhancedCompletionStage}
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  EnhancedCompletionStage<Void> thenRunExceptionally(final ExceptionalRunnable action);

  /**
   * Returns a new {@code EnhancedCompletionStage} that, when this stage completes normally,
   * executes the given {@link ExceptionalRunnable action} using this stage's default asynchronous
   * execution facility as in {@link CompletionStage#thenRunAsync(Runnable)}.
   * <p>
   * See the {@link EnhancedCompletionStage} documentation for special notes on exceptional
   * completion.
   *
   * @param action
   *          the action to perform before completing the returned {@code EnhancedCompletionStage}
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  EnhancedCompletionStage<Void> thenRunExceptionallyAsync(final ExceptionalRunnable action);

  /**
   * Returns a new {@code EnhancedCompletionStage} that, when this stage completes normally,
   * executes the given {@link ExceptionalRunnable action} using the supplied {@code Executor} as in
   * {@link CompletionStage#thenRunAsync(Runnable, Executor)}.
   * <p>
   * See the {@link EnhancedCompletionStage} documentation for special notes on exceptional
   * completion.
   *
   * @param action
   *          the action to perform before completing the returned {@code EnhancedCompletionStage}
   * @param executor
   *          the executor used to execute {@code action}
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  EnhancedCompletionStage<Void> thenRunExceptionallyAsync(final ExceptionalRunnable action,
      final Executor executor);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  <U, V> EnhancedCompletionStage<V> thenCombine(final CompletionStage<? extends U> other,
      final BiFunction<? super T, ? super U, ? extends V> fn);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  <U, V> EnhancedCompletionStage<V> thenCombineAsync(
      final CompletionStage<? extends U> other,
      final BiFunction<? super T, ? super U, ? extends V> fn);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  <U, V> EnhancedCompletionStage<V> thenCombineAsync(
      final CompletionStage<? extends U> other,
      final BiFunction<? super T, ? super U, ? extends V> fn, final Executor executor);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  <U> EnhancedCompletionStage<Void> thenAcceptBoth(final CompletionStage<? extends U> other,
      final BiConsumer<? super T, ? super U> action);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  <U> EnhancedCompletionStage<Void> thenAcceptBothAsync(
      final CompletionStage<? extends U> other,
      final BiConsumer<? super T, ? super U> action);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  <U> EnhancedCompletionStage<Void> thenAcceptBothAsync(
      final CompletionStage<? extends U> other,
      final BiConsumer<? super T, ? super U> action, final Executor executor);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  EnhancedCompletionStage<Void> runAfterBoth(final CompletionStage<?> other,
      final Runnable action);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  EnhancedCompletionStage<Void> runAfterBothAsync(final CompletionStage<?> other,
      final Runnable action);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  EnhancedCompletionStage<Void> runAfterBothAsync(final CompletionStage<?> other,
      final Runnable action, final Executor executor);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  <U> EnhancedCompletionStage<U> applyToEither(final CompletionStage<? extends T> other,
      final Function<? super T, U> fn);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  <U> EnhancedCompletionStage<U> applyToEitherAsync(
      final CompletionStage<? extends T> other, final Function<? super T, U> fn);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  <U> EnhancedCompletionStage<U> applyToEitherAsync(
      final CompletionStage<? extends T> other, final Function<? super T, U> fn,
      final Executor executor);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  EnhancedCompletionStage<Void> acceptEither(final CompletionStage<? extends T> other,
      final Consumer<? super T> action);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  EnhancedCompletionStage<Void> acceptEitherAsync(final CompletionStage<? extends T> other,
      final Consumer<? super T> action);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  EnhancedCompletionStage<Void> acceptEitherAsync(final CompletionStage<? extends T> other,
      final Consumer<? super T> action, final Executor executor);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  EnhancedCompletionStage<Void> runAfterEither(final CompletionStage<?> other,
      final Runnable action);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  EnhancedCompletionStage<Void> runAfterEitherAsync
      (final CompletionStage<?> other,
          final Runnable action);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  EnhancedCompletionStage<Void> runAfterEitherAsync(final CompletionStage<?> other,
      final Runnable action, final Executor executor);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  <U> EnhancedCompletionStage<U> thenCompose(
      final Function<? super T, ? extends CompletionStage<U>> fn);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  <U> EnhancedCompletionStage<U> thenComposeAsync(
      final Function<? super T, ? extends CompletionStage<U>> fn);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  <U> EnhancedCompletionStage<U> thenComposeAsync(
      final Function<? super T, ? extends CompletionStage<U>> fn, final Executor executor);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  EnhancedCompletionStage<T> exceptionally(final Function<Throwable, ? extends T> fn);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  EnhancedCompletionStage<T> whenComplete(
      final BiConsumer<? super T, ? super Throwable> action);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  EnhancedCompletionStage<T> whenCompleteAsync(
      final BiConsumer<? super T, ? super Throwable> action);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  EnhancedCompletionStage<T> whenCompleteAsync(
      final BiConsumer<? super T, ? super Throwable> action, final Executor executor);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  <U> EnhancedCompletionStage<U> handle(
      final BiFunction<? super T, Throwable, ? extends U> fn);

  @Override
  <U> EnhancedCompletionStage<U> handleAsync(
      final BiFunction<? super T, Throwable, ? extends U> fn);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  @Override
  <U> EnhancedCompletionStage<U> handleAsync(
      final BiFunction<? super T, Throwable, ? extends U> fn, final Executor executor);

}
