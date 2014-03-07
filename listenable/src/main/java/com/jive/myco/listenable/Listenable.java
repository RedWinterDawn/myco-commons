package com.jive.myco.listenable;

import java.util.concurrent.Executor;

/**
 * Provides ability to easily control listeners on a given object. This way you don't have to worry
 * about managing your own listeners when they are really all the same.
 *
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public interface Listenable<T>
{
  /**
   * Add the given listener. The listener will be executed in the containing instance's thread.
   *
   * @param listener
   *          listener to add
   */
  public void addListener(T listener);

  /**
   * Add the given listener. The listener will be executed using the given executor
   *
   * @param listener
   *          listener to add
   * @param executor
   *          executor to run listener in
   */
  public void addListener(T listener, Executor executor);

  /**
   * Remove the given listener
   *
   * @param listener
   *          listener to remove
   */
  public void removeListener(T listener);
}
