package com.jive.myco.commons.lifecycle;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.fusesource.hawtdispatch.DispatchQueue;

import com.jive.myco.commons.concurrent.Pnky;
import com.jive.myco.commons.concurrent.PnkyPromise;
import com.jive.myco.commons.function.ExceptionalBiConsumer;
import com.jive.myco.commons.function.ExceptionalBiFunction;
import com.jive.myco.commons.function.ExceptionalConsumer;
import com.jive.myco.commons.function.ExceptionalFunction;

/**
 * A listener for a required upstream dependency, the dependency. Usually used to defer the
 * completion of initialization of the downstream dependency, the dependent, based on the successful
 * initialization of an upstream dependency. This listener is itself a {@link PnkyPromise} that
 * completes based on the successful completion of the observed dependency. Successful completion of
 * initialization by the observed upstream dependency results in the successful completion of this
 * future. A failure to successfully complete initialization by the dependency results in an
 * exception completion of this future.
 *
 * Additionally, this listener will trigger the destruction of the supplied dependent in the event
 * that the entire dependency chain initializes successfully but the observed dependency is later
 * destroyed before the dependent.
 * <p>
 * A typical usage pattern in a dependent is as follows
 *
 * <pre>
 * initInternal()
 * {
 *   final List<PnkyPromise<Void>> dependencies = Lists.newArrayList();
 *
 *   // HTTP Server Manager Dependency
 *   DependencyLifecycleListener httpServerManagerLifecycleListener = new DependencyLifecycleListener(
 *       id, this, lifecycleQueue, &quot;HTTP Server Manager&quot;);
 *
 *   httpServerManager.getLifecycleListenable().addListener(httpServerManagerLifecycleListener);
 *
 *   dependencies.add(httpServerManagerLifecycleListener);
 *
 *   ...
 *
 *
 *   return Pnky
 *       // Wait for all of our deps to start up
 *       .all(dependencies)
 *       .thenTransform((results) -&gt; null);
 * }
 * </pre>
 *
 * @author David Valeri
 */
@RequiredArgsConstructor
@Slf4j
public class DependencyLifecycleListener implements LifecycleListener, PnkyPromise<Void>
{
  private final Pnky<Void> delegate = Pnky.create();

  /**
   * ID used for logging purposes, usually the ID of the {@link #dependant}
   */
  private final String id;

  /**
   * The service that depends on the service that this listener is added to.
   */
  @NonNull
  private final Lifecycled dependant;

  /**
   * The lifecycle queue of the {@link #dependant}
   */
  @NonNull
  private final DispatchQueue lifecycleQueue;

  /**
   * The name of the dependency to which this listener is added
   */
  @NonNull
  private final String dependencyName;

  @Override
  public void stateChanged(final LifecycleStage newState)
  {
    if (newState == LifecycleStage.INITIALIZED)
    {
      if (!delegate.isDone())
      {
        log.debug("[{}]: {} started.", id, dependencyName);
        delegate.resolve(null);
      }
    }
    // One of our dependencies went into a failed or destroying state
    else if (newState == LifecycleStage.INITIALIZATION_FAILED
        || newState.hasAchieved(LifecycleStage.DESTROYING))
    {
      // Reject the future, if it wasn't already handled.
      delegate.reject(new IllegalStateException(
          String.format(
              "[%s]: %s did not satisfy lifecycle requirements during initialization.",
              id, dependencyName)));

      // Do the next bit on the lifecycle queue since it needs to check our current state before
      // making a decision and we don't really need it to run if the dependant is properly
      // watching this instance during initialization. It is primarily for the case where shutdown
      // happens in the wrong order.
      Pnky.runAsync(() ->
      {
        // We are still initializing or we are initialized, which means we are going to fail too
        // since our dependency failed.
        if (dependant.getLifecycleStage() == LifecycleStage.INITIALIZING
            || dependant.getLifecycleStage() == LifecycleStage.INITIALIZED)
        {
          log.error(
              "[{}]: {} failed startup or was destroyed ([{}]) while this "
                  + "manager was initializing or initialized ([{}]).  Triggering destruction.",
              id, dependencyName, newState, dependant.getLifecycleStage());

          dependant.destroy()
              .alwaysAccept((result, cause) ->
              {
                if (cause == null)
                {
                  log.info(
                      "[{}]: Destroyed by upstream dependency failure.", id);
                }
                else
                {
                  log.error(
                      "[{}]: Error handling destroy triggered by upstream dependency.",
                      id, cause);
                }
              });
        }
      }, lifecycleQueue);
    }
  }

  /**
   * Returns a future that completes when the observed {@link ListenableLifecycled} completes
   * initialization either successfully or with a failure. The returned future completes
   * successfully if the observed instance initializes successfully and completes exceptionally if
   * the observed instance fails initialization.
   *
   * @deprecated {@link DependencyLifecycleListener} now implements {@link PnkyPromise}. Use this
   *             instance directly instead of the returned future.
   */
  @Deprecated
  public Pnky<Void> getFuture()
  {
    return delegate;
  }

  @Override
  public void addListener(final Runnable listener, final Executor executor)
  {
    delegate.addListener(listener, executor);
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning)
  {
    return delegate.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled()
  {
    return delegate.isCancelled();
  }

  @Override
  public boolean isDone()
  {
    return delegate.isDone();
  }

  @Override
  public Void get() throws InterruptedException, ExecutionException
  {
    return delegate.get();
  }

  @Override
  public Void get(final long timeout, final TimeUnit unit) throws InterruptedException,
      ExecutionException,
      TimeoutException
  {
    return delegate.get(timeout, unit);
  }

  @Override
  public PnkyPromise<Void> alwaysAccept(final ExceptionalConsumer<? super Void> onSuccess,
      final ExceptionalConsumer<Throwable> onFailure)
  {
    return delegate.alwaysAccept(onSuccess, onFailure);
  }

  @Override
  public PnkyPromise<Void> alwaysAccept(final ExceptionalConsumer<? super Void> onSuccess,
      final ExceptionalConsumer<Throwable> onFailure, final Executor executor)
  {
    return delegate.alwaysAccept(onSuccess, onFailure, executor);
  }

  @Override
  public <O> PnkyPromise<O> alwaysTransform(final ExceptionalFunction<? super Void, O> onSuccess,
      final ExceptionalFunction<Throwable, O> onFailure)
  {
    return delegate.alwaysTransform(onSuccess, onFailure);
  }

  @Override
  public <O> PnkyPromise<O> alwaysTransform(final ExceptionalFunction<? super Void, O> onSuccess,
      final ExceptionalFunction<Throwable, O> onFailure, final Executor executor)
  {
    return delegate.alwaysTransform(onSuccess, onFailure, executor);
  }

  @Override
  public <O> PnkyPromise<O> alwaysCompose(
      final ExceptionalFunction<? super Void, PnkyPromise<O>> onSuccess,
      final ExceptionalFunction<Throwable, PnkyPromise<O>> onFailure)
  {
    return delegate.alwaysCompose(onSuccess, onFailure);
  }

  @Override
  public <O> PnkyPromise<O> alwaysCompose(
      final ExceptionalFunction<? super Void, PnkyPromise<O>> onSuccess,
      final ExceptionalFunction<Throwable, PnkyPromise<O>> onFailure, final Executor executor)
  {
    return delegate.alwaysCompose(onSuccess, onFailure, executor);
  }

  @Override
  public PnkyPromise<Void> alwaysAccept(
      final ExceptionalBiConsumer<? super Void, Throwable> operation)
  {
    return delegate.alwaysAccept(operation);
  }

  @Override
  public PnkyPromise<Void> alwaysAccept(
      final ExceptionalBiConsumer<? super Void, Throwable> operation,
      final Executor executor)
  {
    return delegate.alwaysAccept(operation, executor);
  }

  @Override
  public <O> PnkyPromise<O> alwaysTransform(
      final ExceptionalBiFunction<? super Void, Throwable, O> function)
  {
    return delegate.alwaysTransform(function);
  }

  @Override
  public <O> PnkyPromise<O> alwaysTransform(
      final ExceptionalBiFunction<? super Void, Throwable, O> function, final Executor executor)
  {
    return delegate.alwaysTransform(function, executor);
  }

  @Override
  public <O> PnkyPromise<O> alwaysCompose(
      final ExceptionalBiFunction<? super Void, Throwable, PnkyPromise<O>> function)
  {
    return delegate.alwaysCompose(function);
  }

  @Override
  public <O> PnkyPromise<O> alwaysCompose(
      final ExceptionalBiFunction<? super Void, Throwable, PnkyPromise<O>> function,
      final Executor executor)
  {
    return delegate.alwaysCompose(function, executor);
  }

  @Override
  public PnkyPromise<Void> alwaysRun(final Runnable operation)
  {
    return delegate.alwaysRun(operation);
  }

  @Override
  public PnkyPromise<Void> alwaysRun(final Runnable operation, final Executor executor)
  {
    return delegate.alwaysRun(operation, executor);
  }

  @Override
  public PnkyPromise<Void> thenAccept(final ExceptionalConsumer<? super Void> onSuccess)
  {
    return delegate.thenAccept(onSuccess);
  }

  @Override
  public PnkyPromise<Void> thenAccept(final ExceptionalConsumer<? super Void> onSuccess,
      final Executor executor)
  {
    return thenAccept(onSuccess, executor);
  }

  @Override
  public <O> PnkyPromise<O> thenTransform(final ExceptionalFunction<? super Void, O> onSuccess)
  {
    return delegate.thenTransform(onSuccess);
  }

  @Override
  public <O> PnkyPromise<O> thenTransform(final ExceptionalFunction<? super Void, O> onSuccess,
      final Executor executor)
  {
    return delegate.thenTransform(onSuccess, executor);
  }

  @Override
  public <O> PnkyPromise<O> thenCompose(
      final ExceptionalFunction<? super Void, PnkyPromise<O>> onSuccess)
  {
    return delegate.thenCompose(onSuccess);
  }

  @Override
  public <O> PnkyPromise<O> thenCompose(
      final ExceptionalFunction<? super Void, PnkyPromise<O>> onSuccess, final Executor executor)
  {
    return delegate.thenCompose(onSuccess, executor);
  }

  @Override
  public PnkyPromise<Void> thenRun(final Runnable onSuccess)
  {
    return delegate.thenRun(onSuccess);
  }

  @Override
  public PnkyPromise<Void> thenRun(final Runnable runnable, final Executor executor)
  {
    return delegate.thenRun(runnable, executor);
  }

  @Override
  public PnkyPromise<Void> onFailure(final ExceptionalConsumer<Throwable> onFailure)
  {
    return delegate.onFailure(onFailure);
  }

  @Override
  public PnkyPromise<Void> onFailure(final ExceptionalConsumer<Throwable> onFailure,
      final Executor executor)
  {
    return delegate.onFailure(onFailure, executor);
  }

  @Override
  public PnkyPromise<Void> withFallback(final ExceptionalFunction<Throwable, Void> onFailure)
  {
    return delegate.withFallback(onFailure);
  }

  @Override
  public PnkyPromise<Void> withFallback(final ExceptionalFunction<Throwable, Void> onFailure,
      final Executor executor)
  {
    return delegate.withFallback(onFailure, executor);
  }

  @Override
  public PnkyPromise<Void> composeFallback(
      final ExceptionalFunction<Throwable, PnkyPromise<Void>> onFailure)
  {
    return delegate.composeFallback(onFailure);
  }

  @Override
  public PnkyPromise<Void> composeFallback(
      final ExceptionalFunction<Throwable, PnkyPromise<Void>> onFailure, final Executor executor)
  {
    return delegate.composeFallback(onFailure, executor);
  }
}
