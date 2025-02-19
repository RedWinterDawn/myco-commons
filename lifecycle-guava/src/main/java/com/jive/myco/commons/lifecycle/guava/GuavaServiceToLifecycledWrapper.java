package com.jive.myco.commons.lifecycle.guava;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.fusesource.hawtdispatch.DispatchQueue;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.Service.Listener;
import com.google.common.util.concurrent.Service.State;
import com.jive.myco.commons.concurrent.ImmediateExecutor;
import com.jive.myco.commons.concurrent.Pnky;
import com.jive.myco.commons.concurrent.PnkyPromise;
import com.jive.myco.commons.hawtdispatch.DispatchQueueBuilder;
import com.jive.myco.commons.lifecycle.LifecycleListener;
import com.jive.myco.commons.lifecycle.LifecycleStage;
import com.jive.myco.commons.lifecycle.Lifecycled;
import com.jive.myco.commons.lifecycle.ListenableLifecycled;
import com.jive.myco.commons.listenable.DefaultListenableContainer;
import com.jive.myco.commons.listenable.Listenable;
import com.jive.myco.commons.listenable.ListenableContainer;

/**
 * A wrapper for a Guava {@link Service} to turn it into a {@link Lifecycled}.
 *
 * @author David Valeri
 */
@Slf4j
public class GuavaServiceToLifecycledWrapper implements ListenableLifecycled
{
  private static final AtomicInteger COUNTER = new AtomicInteger();

  private final Service service;
  private final DispatchQueue dispatchQueue;
  private final InternalListenable listenable = new InternalListenable();
  private volatile LifecycleStage lifecycleStage;
  private final Pnky<Void> creation = Pnky.create();
  private final Pnky<Void> destruction = Pnky.create();

  public GuavaServiceToLifecycledWrapper(@NonNull final Service service,
      @NonNull final DispatchQueueBuilder dispatchQueueBuilder)
  {
    this.service = service;
    this.dispatchQueue =
        dispatchQueueBuilder.segment("LifecycledGuavaService",
            String.valueOf(COUNTER.getAndIncrement())).build();

    service.addListener(
        new Listener()
        {
          @Override
          public void starting()
          {
            lifecycleStage = LifecycleStage.INITIALIZING;
            listenable.stateChanged(lifecycleStage);
          }

          @Override
          public void running()
          {
            lifecycleStage = LifecycleStage.INITIALIZED;
            listenable.stateChanged(lifecycleStage);
            creation.resolve();
          }

          @Override
          public void stopping(final State from)
          {
            lifecycleStage = LifecycleStage.DESTROYING;
            listenable.stateChanged(lifecycleStage);
          }

          @Override
          public void terminated(final State from)
          {
            lifecycleStage = LifecycleStage.DESTROYED;
            listenable.stateChanged(lifecycleStage);
            destruction.resolve();
          }

          @Override
          public void failed(final State from, final Throwable failure)
          {
            if (from == State.STARTING)
            {
              lifecycleStage = LifecycleStage.INITIALIZATION_FAILED;
              listenable.stateChanged(lifecycleStage);
              creation.reject(failure);
            }

            if (from != State.STOPPING)
            {
              lifecycleStage = LifecycleStage.DESTROYING;
              listenable.stateChanged(lifecycleStage);
            }

            lifecycleStage = LifecycleStage.DESTROYED;
            listenable.stateChanged(lifecycleStage);
            destruction.reject(failure);
          }

        }, ImmediateExecutor.INSTANCE);

    Preconditions.checkState(service.state() == State.NEW);
    lifecycleStage = LifecycleStage.UNINITIALIZED;
  }

  @Override
  public PnkyPromise<Void> init()
  {
    service.startAsync();
    return creation;
  }

  @Override
  public PnkyPromise<Void> destroy()
  {
    service.stopAsync();
    return destruction;
  }

  @Override
  public Listenable<LifecycleListener> getLifecycleListenable()
  {
    return listenable;
  }

  @Override
  public LifecycleStage getLifecycleStage()
  {
    return lifecycleStage;
  }

  @Override
  public boolean isRestartable()
  {
    return false;
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
      dispatchQueue.execute(() ->
      {
        final LifecycleStage currentState = lifecycleStage;
        listenable.addListenerWithInitialAction(listener, executor,
            (newListener) -> notifyStateChanged(newListener, currentState));
      });
    }

    @Override
    public void removeListener(final Object listener)
    {
      dispatchQueue.execute(() -> listenable.removeListener(listener));
    }

    private void notifyStateChanged(final LifecycleListener listener, final LifecycleStage state)
    {
      try
      {
        listener.stateChanged(state);
      }
      catch (final Exception e)
      {
        log.error("Lifecycled listener had error while processing state change [{}]", state, e);
      }
    }

    private void stateChanged(final LifecycleStage newState)
    {
      dispatchQueue.execute(() ->
          listenable.forEach((listener) -> notifyStateChanged(listener, newState)));
    }
  }
}
