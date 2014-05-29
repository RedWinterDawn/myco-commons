package com.jive.myco.commons.concurrent;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An enhanced version of {@link CompletionStage} providing value-added operations to reduce
 * boilerplate code and simplify asynchronous processing.
 * <p>
 * Implementations must follow the completion and exceptional completion rules as defined in
 * {@link CompletionStage}.
 *
 *
 * @author David Valeri
 * @author Brandon Pederson
 *
 * @param <T>
 *          the type of the completion result
 */
public interface EnhancedCompletionStage<T>
{
  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  <U> EnhancedCompletionStage<U> thenApply(final Function<? super T, ? extends U> fn);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  <U> EnhancedCompletionStage<U> thenApplyAsync(final Function<? super T, ? extends U> fn);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  <U> EnhancedCompletionStage<U> thenApplyAsync(final Function<? super T, ? extends U> fn,
      final Executor executor);

  /**
   * Returns a new {@code EnhancedCompletionStage} that, when this stage completes normally, is
   * executed with this stage's result as the argument to the supplied {@link ExceptionalFunction
   * function} as in {@link CompletionStage#thenApply(Function)}. When the supplied function
   * completes by throwing an exception, the returned {@code EnhancedCompletionStage} completes
   * exceptionally.
   * <p>
   * See the {@link CompletionStage} documentation for rules covering exceptional completion.
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
   * {@link CompletionStage#thenApplyAsync(Function)}. When the supplied function completes by
   * throwing an exception, the returned {@code EnhancedCompletionStage} completes exceptionally
   * also using this stage's default asynchronous execution facility.
   * <p>
   * See the {@link CompletionStage} documentation for rules covering exceptional completion.
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
   * {@link CompletionStage#thenApplyAsync(Function, Executor)}. When the supplied function
   * completes by throwing an exception, the returned {@code EnhancedCompletionStage} completes
   * exceptionally also using the supplied {@code Executor}.
   * <p>
   * See the {@link CompletionStage} documentation for rules covering exceptional completion.
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
  EnhancedCompletionStage<Void> thenAccept(final Consumer<? super T> action);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  EnhancedCompletionStage<Void> thenAcceptAsync(final Consumer<? super T> action);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  EnhancedCompletionStage<Void> thenAcceptAsync(final Consumer<? super T> action,
      final Executor executor);

  /**
   * Returns a new {@code EnhancedCompletionStage} that, when this stage completes normally, is
   * executed with this stage's result as the argument to the supplied {@link ExceptionalConsumer
   * action} as in {@link CompletionStage#thenAccept(Consumer)}. When the supplied action completes
   * by throwing an exception, the returned {@code EnhancedCompletionStage} completes exceptionally.
   * <p>
   * See the {@link CompletionStage} documentation for rules covering exceptional completion.
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
   * {@link CompletionStage#thenAcceptAsync(Consumer)}. When the supplied action completes by
   * throwing an exception, the returned {@code EnhancedCompletionStage} completes exceptionally
   * also using this stage's default asynchronous execution facility.
   * <p>
   * See the {@link CompletionStage} documentation for rules covering exceptional completion.
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
   * {@link CompletionStage#thenAcceptAsync(Consumer, Executor)}. When the supplied action completes
   * by throwing an exception, the returned {@code EnhancedCompletionStage} completes exceptionally
   * also using the supplied {@code Executor}.
   * <p>
   * See the {@link CompletionStage} documentation for rules covering exceptional completion.
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
  EnhancedCompletionStage<Void> thenRun(final Runnable action);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  EnhancedCompletionStage<Void> thenRunAsync(final Runnable action);

  /**
   * Returns a new {@code EnhancedCompletionStage} following the same contract as defined in the
   * super interface.
   *
   * @return the new {@code EnhancedCompletionStage}
   */
  EnhancedCompletionStage<Void> thenRunAsync(final Runnable action,
      final Executor executor);

  /**
   * Returns a new {@code EnhancedCompletionStage} that, when this stage completes normally,
   * executes the given {@link ExceptionalRunnable action} as in
   * {@link CompletionStage#thenRun(Runnable)}. When the supplied action completes by throwing an
   * exception, the returned {@code EnhancedCompletionStage} completes exceptionally.
   * <p>
   * See the {@link CompletionStage} documentation for rules covering exceptional completion.
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
   * execution facility as in {@link CompletionStage#thenRunAsync(Runnable)}. When supplied action
   * completes by throwing an exception, the returned {@code EnhancedCompletionStage} completes
   * exceptionally also using this stage's default asynchronous execution facility.
   * <p>
   * See the {@link CompletionStage} documentation for rules covering exceptional completion.
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
   * {@link CompletionStage#thenRunAsync(Runnable, Executor)}. When supplied action completes by
   * throwing an exception, the returned {@code EnhancedCompletionStage} completes exceptionally
   * also using the supplied {@code Executor}.
   * <p>
   * See the {@link CompletionStage} documentation for rules covering exceptional completion.
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
}
