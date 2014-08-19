package com.jive.myco.commons.lifecycle;

import com.jive.myco.commons.concurrent.PnkyPromise;

/**
 * Represents a resource that has a lifecycle. An instance moves through the {@link LifecycleStage}s
 * in the following manner.
 * <p>
 * Successful initialization through destruction: UNINITIALIZED, INITIALIZING, INITIALIZED,
 * DESTROYING, DESTROYED.
 *
 * <p>
 * Failed initialization through destruction: UNINITIALIZED, INITIALIZING, INITIALIZATION_FAILED,
 * DESTROYING, DESTROYED.
 * <p>
 * Destruction when uninitialized: UNINITIALIZED, DESTROYING, DESTROYED
 *
 * @author David Valeri
 */
public interface Lifecycled
{
  /**
   * Initializes the resource asynchronously, completing the returned promise under the following
   * conditions:
   * <ul>
   * <li>
   * The promise completes successfully when initialization completes successfully or if the
   * instance if already initialized.</li>
   * <li>
   * The promise completes exceptionally if the initialization fails to complete successfully or if
   * initialization is not possible from the resource's current stage.</li>
   * </ul>
   *
   * @return a promise that can be watched for success or error
   */
  PnkyPromise<Void> init();

  /**
   * Destroys the resource asynchronously, completing the returned promise under the following
   * conditions:
   * <ul>
   * <li>
   * The promise is completed successfully when destruction completes successfully or is already
   * destroyed.</li>
   * <li>
   * The promise is completed exceptionally if destruction fails to complete successfully or if
   * destruction is not possible from the resource's current stage.</li>
   * </ul>
   * <p>
   * This method is intended to be reentrant to allow repeated attempts at destruction. Subsequent
   * attempts to destroy a resource after a failed attempt at destruction should attempt to destroy
   * any remaining resources managed by this resource.
   * </p>
   * <p>
   * This method may be called when the service is uninitialized. This will prevent the instance
   * from being restarted if not {@link #isRestartable()}.
   * </p>
   *
   * @return a promise that can be watched for success or error
   */
  PnkyPromise<Void> destroy();

  /**
   * Get the current lifecycle stage of the instance.
   */
  LifecycleStage getLifecycleStage();

  /**
   * Returns an indicator as to whether or not this instance supports restarting after reaching the
   * {@link LifecycleStage#DESTROYED}.
   *
   * @return {@code true} if the instance can be restarted
   */
  boolean isRestartable();
}
