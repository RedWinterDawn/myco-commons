package com.jive.myco.commons.lifecycle;

import static com.jive.myco.commons.concurrent.Pnky.*;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.NonNull;

import org.fusesource.hawtdispatch.Dispatch;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jive.myco.commons.concurrent.Pnky;
import com.jive.myco.commons.concurrent.PnkyPromise;
import com.jive.myco.commons.listenable.DefaultListenableContainer;
import com.jive.myco.commons.listenable.Listenable;
import com.jive.myco.commons.listenable.ListenableContainer;

/**
 * Provides a common pattern for asynchronously initializing a {@link Lifecycled} service.
 *
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */

public abstract class AbstractLifecycled implements ListenableLifecycled
{
  private static final AtomicInteger COUNTER = new AtomicInteger();

  /**
   * @deprecated this attribute will move to private scope soon, sub-classes should transition to
   *             using {@link #getLifecycleStage()} rather than referencing this field directly.
   */
  @Deprecated
  protected volatile LifecycleStage lifecycleStage = LifecycleStage.UNINITIALIZED;

  protected final DispatchQueue lifecycleQueue;

  // Don't use @SLF4J annotation, we want to know the actual implementing class
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final InternalListenable listenable = new InternalListenable();

  private final String id;

  public AbstractLifecycled(final DispatchQueue lifecycleQueue)
  {
    this(null, lifecycleQueue);
  }

  public AbstractLifecycled(final String id, @NonNull final DispatchQueue lifecycleQueue)
  {
    if (id == null)
    {
      this.id = getClass().getSimpleName() + "-" + COUNTER.getAndIncrement();
    }
    else
    {
      this.id = id;
    }

    this.lifecycleQueue = lifecycleQueue;
  }

  @Override
  public final PnkyPromise<Void> init()
  {
    return composeAsync(() ->
    {
      log.debug("[{}]: Initializing.", id);

      if (lifecycleStage == LifecycleStage.UNINITIALIZED
          || isRestartable() && lifecycleStage == LifecycleStage.DESTROYED)
      {
        setLifecycleStage(LifecycleStage.INITIALIZING);
        lifecycleQueue.suspend();
        try
        {
          return initInternal()
              .thenAccept((result) ->
              {
                setLifecycleStage(LifecycleStage.INITIALIZED);
                lifecycleQueue.resume();
                log.info("[{}]: Initialized.", id);
              })
              .composeFallback(this::initFailure);
        }
        catch (final Exception e)
        {
          return initFailure(e);
        }
      }
      else if (lifecycleStage == LifecycleStage.INITIALIZED)
      {
        log.debug("[{}]: Already initialized.", id);
        return Pnky.immediatelyComplete(null);
      }
      else
      {
        final String message = String.format(
            "[%s]: Cannot initialize in [%s] state",
            id, lifecycleStage);

        log.error(message);
        return Pnky.immediatelyFailed(new IllegalStateException(message));
      }
    }, lifecycleQueue);
  }

  @Override
  public String toString()
  {
    final StringBuilder builder = new StringBuilder();
    builder.append(getClass().getSimpleName());
    builder.append(" [id=");
    builder.append(id);
    builder.append("]");
    return builder.toString();
  }

  private PnkyPromise<Void> initFailure(final Throwable initError)
  {
    setLifecycleStage(LifecycleStage.INITIALIZATION_FAILED);

    log.error("[{}]: Initialization failure.  Handling cleanup...", id);

    PnkyPromise<Void> initFailureFuture;
    try
    {
      initFailureFuture = handleInitFailure();
    }
    catch (final Exception e)
    {
      initFailureFuture = Pnky.immediatelyFailed(e);
    }

    return initFailureFuture
        .alwaysCompose((result, error) ->
        {
          if (error != null)
          {
            log.error(
                "[{}]: Error occurred during handling of failed init.  Continuing "
                    + "with destruction...",
                id,
                error);
          }

          final PnkyPromise<Void> destroy = destroy();
          lifecycleQueue.resume();
          return destroy;
        })
        .alwaysCompose((res, destroyError) ->
        {
          if (destroyError != null)
          {
            log.error(
                "[{}]: Error occurred during destroy after failed initialization",
                id, destroyError);
          }

          return Pnky.immediatelyFailed(initError);
        });
  }

  @Override
  public final PnkyPromise<Void> destroy()
  {
    return composeAsync(() ->
    {
      log.debug("[{}]: Destroying.", id);

      if (lifecycleStage == LifecycleStage.INITIALIZED
          || lifecycleStage == LifecycleStage.UNINITIALIZED
          || lifecycleStage == LifecycleStage.INITIALIZATION_FAILED
          || lifecycleStage == LifecycleStage.DESTROYING)
      {
        setLifecycleStage(LifecycleStage.DESTROYING);
        lifecycleQueue.suspend();
        try
        {
          return destroyInternal()
              .alwaysAccept((result, error) ->
              {
                if (error == null)
                {
                  setLifecycleStage(LifecycleStage.DESTROYED);
                  log.info("[{}]: Destroyed.", id);
                }
                else
                {
                  log.error("[{}]: Destroy failed.", id);
                }

                lifecycleQueue.resume();
              });
        }
        catch (final Exception e)
        {
          lifecycleQueue.resume();
          return Pnky.immediatelyFailed(e);
        }
      }
      else if (lifecycleStage == LifecycleStage.DESTROYED)
      {
        log.debug("[{}]: Already destroyed.", id);
        return immediatelyComplete(null);
      }
      else
      {
        final String message = String.format(
            "[%s]: Cannot destroy in [%s] state",
            id, lifecycleStage);

        log.error(message);
        return Pnky.immediatelyFailed(new IllegalStateException(message));
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
  protected PnkyPromise<Void> handleInitFailure()
  {
    return Pnky.immediatelyComplete(null);
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
  protected abstract PnkyPromise<Void> initInternal();

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
  protected abstract PnkyPromise<Void> destroyInternal();

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
  @Override
  public boolean isRestartable()
  {
    return false;
  }

  @Override
  public Listenable<LifecycleListener> getLifecycleListenable()
  {
    return listenable;
  }

  /**
   * Set the lifecycle stage of this {@code Lifecycled} instance and notify any listeners of the
   * state change. This should <strong>ONLY</strong> be done on the lifecycle queue or with the
   * lifecycle queue suspended.
   *
   * @param newState
   *          the new lifecycle state
   */
  protected final void setLifecycleStage(final LifecycleStage newState)
  {
    lifecycleStage = newState;
    lifecycleQueue.execute(() -> listenable.stateChanged(newState));
  }

  /**
   * Manages the listeners that are watching this {@code ListenableLifecycled} instance and notifies
   * the listeners when there is a state change.
   */
  private final class InternalListenable implements Listenable<LifecycleListener>
  {
    private final ListenableContainer<LifecycleListener> listenable =
        new DefaultListenableContainer<>();

    @Override
    public void addListener(final LifecycleListener listener, final Executor executor)
    {
      lifecycleQueue.execute(() ->
      {
        final LifecycleStage currentState = lifecycleStage;
        listenable.addListenerWithInitialAction(listener, executor,
            (newListener) -> notifyStateChanged(newListener, currentState));
      });
    }

    @Override
    public void removeListener(final Object listener)
    {
      lifecycleQueue.execute(() -> listenable.removeListener(listener));
    }

    private void notifyStateChanged(final LifecycleListener listener, final LifecycleStage state)
    {
      try
      {
        listener.stateChanged(state);
      }
      catch (final Exception e)
      {
        log.error(
            "[{}]: Lifecycled listener had error while processing state change [{}]",
            id, state, e);
      }
    }

    private void stateChanged(final LifecycleStage newState)
    {
      listenable.forEach((listener) -> notifyStateChanged(listener, newState));
    }
  }
}
