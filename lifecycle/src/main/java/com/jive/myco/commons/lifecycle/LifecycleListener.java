package com.jive.myco.commons.lifecycle;

/**
 * Listener that will respond to changes on a {@link ListenableLifecycled} instance.
 *
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public interface LifecycleListener
{
  /**
   * Notification that the {@link ListenableLifecycled} instance has changed to the following state.
   *
   * @param newStage
   *          the new lifecycle stage
   */
  void stateChanged(final LifecycleStage newStage);
}
