package com.jive.myco.commons.listenable;

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
  public void forEach(Consumer<? super T> action);

  /**
   * Clear all listeners
   */
  public void clear();
}
