package com.jive.myco.commons.lifecycle;

import com.jive.myco.commons.callbacks.Callback;
import com.jive.myco.commons.concurrent.Pnky;
import com.jive.myco.commons.concurrent.PnkyPromise;

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
   * callback method may be invoked immediately on the current thread.</li>
   * </ul>
   *
   * @param callback
   *          the callback to invoke on success or failure
   *
   * @deprecated in favor of the promise approach via {@link #init()}
   */
  @Deprecated
  void init(Callback<Void> callback);

  /**
   * Initializes the resource asynchronously, completing the returned promise under the following
   * conditions:
   * <ul>
   * <li>
   * The promise is completed successfully when initialization completes successfully or is already
   * initialized.</li>
   * <li>
   * The promise is completed exceptionally if the initialization fails to complete successfully or
   * if initialization is not possible from the resource's current stage.</li>
   * </ul>
   *
   * @return a promise that can be watched for success or error
   */
  default PnkyPromise<Void> init()
  {
    Pnky<Void> pnky = Pnky.create();

    init(new Callback<Void>()
    {
      @Override
      public void onSuccess(final Void result)
      {
        pnky.set(null);
      }

      @Override
      public void onFailure(final Throwable cause)
      {
        pnky.setException(cause);
      }
    });

    return pnky;
  }

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
   * method may be invoked immediately on the current thread.</li>
   * </ul>
   * <p>
   * This method is intended to be reentrant to allow repeated attempts at destruction. Subsequent
   * attempts to destroy a resource after a failed attempt at destruction should attempt to destroy
   * any remaining resources managed by this resource.
   * </p>
   *
   * @param callback
   *          the callback to invoke on success or failure
   *
   * @deprecated in favor of the promise approach via {@link #destroy()}
   */
  @Deprecated
  void destroy(Callback<Void> callback);

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
   *
   * @return a promise that can be watched for success or error
   */
  default PnkyPromise<Void> destroy()
  {
    Pnky<Void> pnky = Pnky.create();

    destroy(new Callback<Void>()
    {
      @Override
      public void onSuccess(final Void result)
      {
        pnky.set(null);
      }

      @Override
      public void onFailure(final Throwable cause)
      {
        pnky.setException(cause);
      }
    });

    return pnky;
  }

  LifecycleStage getLifecycleStage();
}
