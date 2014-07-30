package com.jive.myco.commons.lifecycle;

import com.jive.myco.commons.listenable.Listenable;

/**
 * A lifecycled instance that adds the ability to watch for when the lifecycle state changes.
 *
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public interface ListenableLifecycled extends Lifecycled
{
  /**
   * Get the {@link Listenable} that can be used to add listeners to respond to lifecycle change
   * events.
   */
  Listenable<LifecycleListener> getLifecycleListenable();
}
