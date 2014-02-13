package com.jive.myco.core.lifecycle;

import com.jive.myco.callbacks.Callback;

/**
 * Represents a resource that has a lifecycle.
 * 
 * @author David Valeri
 */
public interface Lifecycled
{
  /**
   * Initializes the resource asynchronously, invoking the callback under the following conditions:
   * <ul>
   * <li>
   * {@link Callback#onSuccess(Object)} is invoked when the initialization completes successfully.
   * If the resource is already successfully initialized, this callback method is invoked
   * immediately on the same thread that invoked this method.</li>
   * <li>
   * {@link Callback#onFailure(Throwable)} is invoked if the initialization fails to complete
   * successfully. Fail fast type failures may invoke this callback method immediately on the
   * current thread. If initialization is not possible from the resource's current stage, this
   * callback method is invoked immediately on the current thread.</li>
   * </ul>
   * 
   * @param callback
   *          the callback to invoke on success or failure
   */
  void init(final Callback<Void> callback);

  /**
   * Destroys the resource asynchronously, invoking the callback under the following conditions:
   * <ul>
   * <li>
   * {@link Callback#onSuccess(Object)} is invoked when the destruction completes successfully. If
   * the resource is already successfully destroyed, this callback method is invoked immediately on
   * the same thread that invoked this method.</li>
   * <li>
   * {@link Callback#onFailure(Throwable)} is invoked if the destruction fails to complete
   * successfully. If destruction is not possible from the resource's current stage, this callback
   * method is invoked immediately on the current thread.</li>
   * </ul>
   * <p/>
   * This method is intended to be reentrant to allow repeated attempts at destruction. Subsequent
   * attempts to destroy a resource after a failed attempt at destruction should attempt to destroy
   * any remaining resources managed by this resource.
   * 
   * @param callback
   *          the callback to invoke on success or failure
   */
  void destroy(final Callback<Void> callback);

  LifecycleStage getLifecycleStage();
}
