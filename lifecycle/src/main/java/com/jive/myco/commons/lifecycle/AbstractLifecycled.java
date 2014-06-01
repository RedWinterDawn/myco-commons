package com.jive.myco.commons.lifecycle;

import static com.jive.myco.commons.concurrent.CompletableFutures.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  public final CompletionStage<Void> init()
  {
    CompletableFuture<Void> dfd = new CompletableFuture<>();

    runOnLifecycleQueue(new Runnable()
    {
      @Override
      public void run()
      {
        if (lifecycleStage == LifecycleStage.UNINITIALIZED
            || (isRestartable() && lifecycleStage == LifecycleStage.DESTROYED))
        {
          lifecycleStage = LifecycleStage.INITIALIZING;
          lifecycleQueue.suspend();
          try
          {
            initInternal()
                .whenComplete((result, initError) ->
                {
                  if (initError == null)
                  {
                    lifecycleStage = LifecycleStage.INITIALIZED;
                    lifecycleQueue.resume();
                    dfd.complete(null);
                  }
                  else
                  {
                    initFailure()
                        .whenComplete((eResult, eError) -> dfd.completeExceptionally(initError));
                  }
                });
          }
          catch (Exception e)
          {
            initFailure()
                .whenComplete((eResult, eError) -> dfd.completeExceptionally(e));
          }
        }
        else if (lifecycleStage == LifecycleStage.INITIALIZED)
        {
          dfd.complete(null);
        }
        else
        {
          dfd.completeExceptionally(new IllegalStateException(String.format(
              "Cannot initialize in [%s] state", lifecycleStage)));
        }
      }

      private CompletionStage<Void> initFailure()
      {
        lifecycleStage = LifecycleStage.INITIALIZATION_FAILED;

        try
        {
          return handleInitFailure()
              .handle((result, error) ->
              {
                if (error != null)
                {
                  log.error("Error occurred during cleanup after failed initialization", error);
                }
                return doDestroy();
              })
              .thenCompose(Function.identity());
        }
        catch (Exception e)
        {
          return doDestroy();
        }
      }

      private CompletionStage<Void> doDestroy()
      {
        CompletionStage<Void> destroyed = destroy()
            .whenComplete((result, error) ->
            {
              if (error != null)
              {
                log.error("Error occurred during destroy after failed initialization", error);
              }
            });
        lifecycleQueue.resume();
        return destroyed;
      }
    });

    return dfd;
  }

  @Override
  public final CompletionStage<Void> destroy()
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
              .whenComplete((result, error) ->
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
          return immediatelyFailed(e);
        }
      }
      else if (lifecycleStage == LifecycleStage.DESTROYED)
      {
        return immediatelyComplete(null);
      }
      else
      {
        return immediatelyFailed(new IllegalStateException());
      }
    }, lifecycleQueue);
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
   * @return completion stage to handle when cleanup is complete
   */
  protected CompletionStage<Void> handleInitFailure()
  {
    return CompletableFuture.completedFuture(null);
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
  protected abstract CompletionStage<Void> initInternal();

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
  protected abstract CompletionStage<Void> destroyInternal();

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
