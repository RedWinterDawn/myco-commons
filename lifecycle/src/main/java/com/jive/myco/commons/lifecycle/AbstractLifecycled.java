package com.jive.myco.commons.lifecycle;

import static com.jive.myco.commons.callbacks.Util.*;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;

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
  protected final AtomicReference<LifecycleStage> lifecycleStage =
      new AtomicReference<>(LifecycleStage.UNINITIALIZED);

  @NonNull
  protected final DispatchQueue lifecycleQueue;

  @Override
  public void init(Callback<Void> callback)
  {
    if (lifecycleStage.compareAndSet(LifecycleStage.UNINITIALIZED, LifecycleStage.INITIALIZING))
    {
      lifecycleQueue.execute(new SafeCallbackRunnable<Void>(callback, getCallbackExecutor())
      {

        @Override
        protected void doRun() throws Exception
        {
          SafeCallbackRunnable<Void> that = this;
          lifecycleQueue.suspend();
          try
          {
            initInternal(new Callback<Void>()
            {
              @Override
              public void onSuccess(Void result)
              {
                lifecycleStage.set(LifecycleStage.INITIALIZED);
                lifecycleQueue.resume();
                that.onSuccess(null);
              }

              @Override
              public void onFailure(Throwable cause)
              {
                lifecycleStage.set(LifecycleStage.INITIALIZATION_FAILED);
                lifecycleQueue.resume();
                that.onFailure(cause);
              }
            });
          }
          catch (Exception e)
          {
            lifecycleStage.set(LifecycleStage.INITIALIZATION_FAILED);
            lifecycleQueue.resume();
            that.onFailure(e);
          }
        }
      });
    }
    else if (lifecycleStage.get() == LifecycleStage.INITIALIZED)
    {
      runCallback(callback, null, getCallbackExecutor());
    }
    else
    {
      runCallback(
          callback,
          new IllegalStateException(String.format(
              "Cannot initialize controller in [%s] state", lifecycleStage.get())),
          getCallbackExecutor());
    }
  }

  @Override
  public void destroy(Callback<Void> callback)
  {
    if (lifecycleStage.compareAndSet(LifecycleStage.INITIALIZED, LifecycleStage.DESTROYING)
        || lifecycleStage.compareAndSet(LifecycleStage.INITIALIZATION_FAILED,
            LifecycleStage.DESTROYING)
        || lifecycleStage.compareAndSet(LifecycleStage.DESTROYING, LifecycleStage.DESTROYING))
    {
      lifecycleQueue.execute(new SafeCallbackRunnable<Void>(callback, getCallbackExecutor())
      {

        @Override
        protected void doRun() throws Exception
        {
          SafeCallbackRunnable<Void> that = this;
          lifecycleQueue.suspend();
          try
          {
            destroyInternal(new Callback<Void>()
            {
              @Override
              public void onSuccess(Void result)
              {
                lifecycleStage.set(LifecycleStage.DESTROYED);
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
      });
    }
    else if (lifecycleStage.get() == LifecycleStage.DESTROYED)
    {
      runCallback(callback, null, getCallbackExecutor());
    }
    else
    {
      runCallback(callback,
          new IllegalStateException(String.format("Cannot destroy in the [%s] stage.",
              lifecycleStage.get())), getCallbackExecutor());
    }
  }

  /**
   * Initialize this {@link Lifecycled} instance. This will be run on the {@link #lifecycleQueue} so
   * all you need to do is initialize your instance and it's components and protect the lifecycle
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
   * all you need to do is destroy your instance and it's components and protect the lifecycle queue
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

  @Override
  public LifecycleStage getLifecycleStage()
  {
    return lifecycleStage.get();
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
}
