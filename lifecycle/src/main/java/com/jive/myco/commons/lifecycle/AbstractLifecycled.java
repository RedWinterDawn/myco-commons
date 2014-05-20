package com.jive.myco.commons.lifecycle;

import java.util.concurrent.Executor;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jive.myco.commons.callbacks.Callback;
import com.jive.myco.commons.callbacks.SafeCallbackRunnable;

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

  /**
   * Helper method to run a {@link Runnable} on the lifecycle queue. If not currently on the
   * lifecycle queue the {@code runnable} will be launched on the lifecycle queue, otherwise it will
   * be run in the current thread.
   *
   * @param runnable
   *          the task to run on the lifecycle queue
   */
  protected final void runOnLifecycleQueue(Runnable runnable)
  {
    if (lifecycleQueue.isExecuting())
    {
      runnable.run();
    }
    else
    {
      lifecycleQueue.execute(runnable);
    }
  }

  @Override
  public final void init(Callback<Void> callback)
  {
    runOnLifecycleQueue(new SafeCallbackRunnable<Void>(callback, getCallbackExecutor())
    {

      @Override
      protected void doRun() throws Exception
      {
        if (lifecycleStage == LifecycleStage.UNINITIALIZED
            || (isRestartable() && lifecycleStage == LifecycleStage.DESTROYED))
        {
          lifecycleStage = LifecycleStage.INITIALIZING;
          SafeCallbackRunnable<Void> that = this;
          lifecycleQueue.suspend();
          try
          {
            initInternal(new Callback<Void>()
            {
              @Override
              public void onSuccess(Void result)
              {
                lifecycleStage = LifecycleStage.INITIALIZED;
                lifecycleQueue.resume();
                that.onSuccess(null);
              }

              @Override
              public void onFailure(Throwable cause)
              {
                initFailure(cause);
              }
            });
          }
          catch (Exception e)
          {
            initFailure(e);
          }
        }
        else if (lifecycleStage == LifecycleStage.INITIALIZED)
        {
          onSuccess(null);
        }
        else
        {
          onFailure(new IllegalStateException(String.format(
              "Cannot initialize in [%s] state", lifecycleStage)));
        }
      }

      private void initFailure(final Throwable initFailure)
      {
        lifecycleStage = LifecycleStage.INITIALIZATION_FAILED;

        try
        {
          handleInitFailure(new Callback<Void>()
          {
            @Override
            public void onSuccess(Void result)
            {
              doDestroy(initFailure);
            }

            @Override
            public void onFailure(Throwable e)
            {
              log.error("Error occurred during cleanup after failed initialization", e);
              doDestroy(initFailure);
            }
          });
        }
        catch (Exception e)
        {
          log.error("Error occurred while invoking failed init handler", e);
          doDestroy(initFailure);
        }
      }

      private void doDestroy(Throwable initFailure)
      {
        SafeCallbackRunnable<Void> that = this;
        destroy(new Callback<Void>()
        {
          @Override
          public void onSuccess(Void result)
          {
            that.onFailure(initFailure);
          }

          @Override
          public void onFailure(Throwable cause)
          {
            log.error("Error occurred during destroy after failed initialization", cause);
            that.onFailure(initFailure);
          }
        });
        lifecycleQueue.resume();
      }
    });
  }

  @Override
  public final void destroy(Callback<Void> callback)
  {
    runOnLifecycleQueue(new SafeCallbackRunnable<Void>(callback, getCallbackExecutor())
    {

      @Override
      protected void doRun() throws Exception
      {
        if (lifecycleStage == LifecycleStage.INITIALIZED
            || lifecycleStage == LifecycleStage.INITIALIZATION_FAILED
            || lifecycleStage == LifecycleStage.DESTROYING)
        {
          lifecycleStage = LifecycleStage.DESTROYING;
          SafeCallbackRunnable<Void> that = this;
          lifecycleQueue.suspend();
          try
          {
            destroyInternal(new Callback<Void>()
            {
              @Override
              public void onSuccess(Void result)
              {
                lifecycleStage = LifecycleStage.DESTROYED;
                lifecycleQueue.resume();
                that.onSuccess(null);
              }

              @Override
              public void onFailure(Throwable cause)
              {
                lifecycleQueue.resume();
                that.onFailure(cause);
              }
            });
          }
          catch (Exception e)
          {
            lifecycleQueue.resume();
            that.onFailure(e);
          }
        }
        else if (lifecycleStage == LifecycleStage.DESTROYED)
        {
          onSuccess(null);
        }
        else
        {
          onFailure(new IllegalStateException(String.format("Cannot destroy in the [%s] stage.",
              lifecycleStage)));
        }
      }
    });
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
