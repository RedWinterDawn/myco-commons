package com.jive.myco.commons.listenable;

import java.util.concurrent.Executor;

import com.jive.myco.commons.concurrent.ImmediateExecutor;

/**
 * Provides ability to easily control listeners on a given object. This way you don't have to worry
 * about managing your own listeners when they are really all the same.
 *
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public interface Listenable<T>
{
  /**
   * Add the given listener. The listener will be executed in a thread managed by this instance.
   * Calling this method with a listener that has already been added will update the listener's
   * associated executor.
   *
   * @param listener
   *          listener to add
   */
  default void addListener(final T listener)
  {
    addListener(listener, ImmediateExecutor.INSTANCE);
  }

  /**
   * Add the given listener. The listener will be executed via the supplied {@code executor}.
   * Calling this method with a listener that has already been added will update the listener's
   * associated executor.
   *
   * @param listener
   *          listener to add
   * @param executor
   *          executor to run the listener in
   */
  void addListener(final T listener, final Executor executor);

  /**
   * Remove the given listener. If the listener has not been added, this method has no effect.
   *
   * @param listener
   *          listener to remove
   */
  void removeListener(final Object listener);
}
