package com.jive.myco.listenable;

import com.google.common.base.Function;

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
  public void forEach(Function<T, Void> action);
}
