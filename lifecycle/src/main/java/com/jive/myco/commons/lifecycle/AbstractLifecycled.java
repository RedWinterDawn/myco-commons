package com.jive.myco.commons.lifecycle;

import static com.jive.myco.commons.concurrent.Pnky.*;

import java.util.concurrent.Executor;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.MoreExecutors;
import com.jive.myco.commons.callbacks.Callback;
import com.jive.myco.commons.concurrent.Pnky;
import com.jive.myco.commons.concurrent.PnkyPromise;

/**
 * Provides a common pattern for asynchronously initializing a {@link Lifecycled} service.
 *
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
@RequiredArgsConstructor
public abstract class AbstractLifecycled implements Lifecycled
{
  protected volatile LifecycleStage lifecycleStage = LifecycleStage.UNINITIALIZED;

  @NonNull
  protected final DispatchQueue lifecycleQueue;

  // Don't use @SLF4J annotation, we want to know the actual implementing class
  private final Logger log = LoggerFactory.getLogger(getClass());


  @Override
  public final void init(final Callback<Void> callback)
  {
    init().alwaysAccept(callback::onSuccess, callback::onFailure);
  }

  @Override
  public final PnkyPromise<Void> init()
  {
    return composeAsync(() ->
    {
      if (lifecycleStage == LifecycleStage.UNINITIALIZED
          || (isRestartable() && lifecycleStage == LifecycleStage.DESTROYED))
      {
        lifecycleStage = LifecycleStage.INITIALIZING;
        lifecycleQueue.suspend();
        try
        {
          return initInternal()
              .thenAccept((result) ->
              {
                lifecycleStage = LifecycleStage.INITIALIZED;
                lifecycleQueue.resume();
              })
              .composeFallback(this::initFailure);
        }
        catch (Exception e)
        {
          return initFailure(e);
        }
      }
      else if (lifecycleStage == LifecycleStage.INITIALIZED)
      {
        return Pnky.immediateFuture(null);
      }
      else
      {
        return Pnky.immediateFailedFuture(new IllegalStateException(String.format(
            "Cannot initialize in [%s] state", lifecycleStage)));
      }
    }, lifecycleQueue.isExecuting() ? MoreExecutors.sameThreadExecutor() : lifecycleQueue);
  }

  private PnkyPromise<Void> initFailure(final Throwable initError)
  {
    lifecycleStage = LifecycleStage.INITIALIZATION_FAILED;

    PnkyPromise<Void> initFailureFuture;
    try
    {
      initFailureFuture = handleInitFailure();
    }
    catch (Exception e)
    {
      initFailureFuture = Pnky.immediateFailedFuture(e);
    }

    return initFailureFuture
        .alwaysCompose((result, error) ->
        {
          if (error != null)
          {
            log.error("Error occurred during handling of failed init", error);
          }

          return destroy();
        })
        .alwaysCompose((res, destroyError) ->
        {
          if (destroyError != null)
          {
            log.error(
                "Error occurred during cleanup after failed initialization", destroyError);
          }

          return Pnky.<Void> immediateFailedFuture(initError);
        })
        .alwaysAccept((result, error) -> lifecycleQueue.resume());
  }

  @Override
  public final void destroy(final Callback<Void> callback)
  {
    destroy().alwaysAccept(callback::onSuccess, callback::onFailure);
  }

  @Override
  public final PnkyPromise<Void> destroy()
  {
    return composeAsync(() ->
    {
      if (lifecycleStage == LifecycleStage.INITIALIZED
          || lifecycleStage == LifecycleStage.INITIALIZATION_FAILED
          || lifecycleStage == LifecycleStage.DESTROYING)
      {
        lifecycleStage = LifecycleStage.DESTROYING;
        lifecycleQueue.suspend();
        try
        {
          return destroyInternal()
              .alwaysAccept((result, error) ->
              {
                if (error == null)
                {
                  lifecycleStage = LifecycleStage.DESTROYED;
                }
                lifecycleQueue.resume();
              });
        }
        catch (Exception e)
        {
          lifecycleQueue.resume();
          return immediateFailedFuture(e);
        }
      }
      else if (lifecycleStage == LifecycleStage.DESTROYED)
      {
        return immediateFuture(null);
      }
      else
      {
        return immediateFailedFuture(new IllegalStateException());
      }
    }, lifecycleQueue.isExecuting() ? MoreExecutors.sameThreadExecutor() : lifecycleQueue);
  }

  @Override
  public final LifecycleStage getLifecycleStage()
  {
    // note: can't use @Getter since it is not final
    return lifecycleStage;
  }

  /**
   * Override this method to perform additional cleanup after a call to {@link #init} has failed.
   * This will be invoked prior to calling {@link #destroy}.
   *
   * @param callback
   *          callback to invoke when cleanup is complete
   */
  protected void handleInitFailure(Callback<Void> callback)
  {
    callback.onSuccess(null);
  }

  /**
   * Initialize this {@link Lifecycled} instance. This will be run on the {@link #lifecycleQueue} so
   * all you need to do is initialize your instance and its components and protect the lifecycle
   * queue during initialization.
   * <p>
   * The {@code callback} here is used to trigger the setting of the state of the lifecycle stage
   * and trigger the {@code callback} passed to {@link #init(Callback)}. You do not need to set the
   * lifecycle stage in this method.
   *
   * @param callback
   *          callback to invoke when initialization is complete
   */
  protected abstract void initInternal(Callback<Void> callback);

  /**
   * Destroy this {@link Lifecycled} instance. This will be run on the {@link #lifecycleQueue} so
   * all you need to do is destroy your instance and its components and protect the lifecycle queue
   * during the destroy process.
   * <p>
   * The {@code callback} here is used to trigger the setting of the state of the lifecycle stage
   * and trigger the {@code callback} passed to {@link #destroy(Callback)}. You do not need to set
   * the lifecycle stage in this method.
   *
   * @param callback
   *          callback to invoke when initialization is complete
   */
  protected abstract void destroyInternal(Callback<Void> callback);

  /**
   * Override this method to perform additional cleanup after a call to {@link #init} has failed.
   * This will be invoked prior to calling {@link #destroy}.
   *
   * @return completion stage to handle when cleanup is complete
   */
  protected PnkyPromise<Void> handleInitFailure()
  {
    PnkyCallback<Void> pnky = new PnkyCallback<>();
    handleInitFailure(pnky);
    return pnky;
  }

  /**
   * Initialize this {@link Lifecycled} instance. This will be run on the {@link #lifecycleQueue} so
   * all you need to do is initialize your instance and its components and protect the lifecycle
   * queue during initialization.
   * <p>
   * The {@code callback} here is used to trigger the setting of the state of the lifecycle stage
   * and trigger the {@code callback} passed to {@link #init}. You do not need to set the
   * lifecycle stage in this method.
   *
   * @return completion stage to handle when initialization is complete
   */
  protected PnkyPromise<Void> initInternal()
  {
    PnkyCallback<Void> pnky = new PnkyCallback<>();
    initInternal(pnky);
    return pnky;
  }

  /**
   * Destroy this {@link Lifecycled} instance. This will be run on the {@link #lifecycleQueue} so
   * all you need to do is destroy your instance and its components and protect the lifecycle queue
   * during the destroy process.
   * <p>
   * The {@code callback} here is used to trigger the setting of the state of the lifecycle stage
   * and trigger the {@code callback} passed to {@link #destroy}. You do not need to set
   * the lifecycle stage in this method.
   *
   * @return completion stage to handle when initialization is complete
   */
  protected PnkyPromise<Void> destroyInternal()
  {
    PnkyCallback<Void> pnky = new PnkyCallback<>();
    destroyInternal(pnky);
    return pnky;
  }

  /**
   * Retrieves the {@link Executor} that will be used to fire callbacks to {@link #init} and
   * {@link #destroy}. By default this uses the HawtDispatch global queue. Override this to control
   * that behavior.
   *
   * @return the executor to use
   */
  protected Executor getCallbackExecutor()
  {
    return Dispatch.getGlobalQueue();
  }

  /**
   * Indicator that this {@code Lifecycled} instance is able to be re-initialized after it has been
   * destroyed. In other words it would be possible to call {@link #init} after {@code #destroy}. By
   * default this is {@code false}.
   *
   * @return if we can call {@code init} after {@code destroy}
   */
  protected boolean isRestartable()
  {
    return false;
  }
}
