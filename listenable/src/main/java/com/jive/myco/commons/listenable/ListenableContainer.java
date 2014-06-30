package com.jive.myco.commons.listenable;

import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public interface ListenableContainer<T> extends Listenable<T>
{
  /**
   * Apply the given action to all of the listeners in the {@code Listenable}, executing them on the
   * executor they were registered with.
   *
   * @param action
   */
  void forEach(final Consumer<? super T> action);

  /**
   * Clear all listeners
   */
  void clear();

  /**
   * Adds a new listener. The listener is invoked on the calling thread of
   * {@link #forEach(Consumer)}. Calling this method with a listener that has already been added
   * will update the listener's associated executor.
   *
   * @param listener
   *          the listener to add
   */
  @Override
  void addListener(final T listener);

  /**
   * Adds a new listener. The listener is invoked on the supplied {@code executor} when triggered
   * via {@link #forEach(Consumer)}. Calling this method with a listener that has already been added
   * will update the listener's associated executor.
   *
   * @param listener
   *          the listener to add
   * @param executor
   *          executor to run the listener in
   */
  @Override
  void addListener(final T listener, final Executor executor);

  /**
   * Adds a new listener, passing the listener to {@code action} after successfully adding it on the
   * calling thread. The listener is invoked on the calling thread of {@link #forEach(Consumer)}.
   * There is no guarantee of ordering between execution of the supplied {@code action} and actions
   * applied to listeners via {@link #forEach(Consumer)}. Clients wishing to ensure strict ordering
   * of applied actions must provide external coordination. Calling this method with a listener that
   * has already been added will update the listener's associated executor but will not invoke the
   * provided action.
   *
   * @param listener
   *          the listener to add
   * @param action
   *          the action to perform on the listener
   */
  void addListenerWithInitialAction(final T listener, final Consumer<? super T> action);

  /**
   * Adds a new listener, passing the listener to {@code action} on the supplied {@code executor}
   * after successfully adding the listener on the calling thread. The listener is invoked on the
   * supplied {@code executor} when triggered via {@link #forEach(Consumer)}. There is no guarantee
   * of ordering between execution of the supplied {@code action} and actions applied to listeners
   * via {@link #forEach(Consumer)}. Clients wishing to ensure strict ordering of applied actions
   * must provide external coordination. Calling this method with a listener that has already been
   * added will update the listener's associated executor but will not invoke the provided action.
   *
   * @param listener
   *          the listener to add
   * @param executor
   *          executor to run the listener in
   * @param action
   *          the action to perform on the listener
   */
  void addListenerWithInitialAction(final T listener, final Executor executor,
      final Consumer<? super T> action);
}
