package com.jive.myco.commons.concurrent;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.jive.myco.commons.function.ExceptionalBiConsumer;
import com.jive.myco.commons.function.ExceptionalBiFunction;
import com.jive.myco.commons.function.ExceptionalConsumer;
import com.jive.myco.commons.function.ExceptionalFunction;
import com.jive.myco.commons.function.ExceptionalRunnable;
import com.jive.myco.commons.function.ExceptionalSupplier;

/**
 * Default implementation of a {@link PnkyPromise} that also provides some useful methods for
 * initiating a chain of asynchronous actions.
 *
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pnky<V> extends AbstractFuture<V> implements PnkyPromise<V>
{
  public static <V> Pnky<V> create()
  {
    return new Pnky<>();
  }

  public boolean resolve(V value)
  {
    return super.set(value);
  }

  public boolean reject(@Nonnull Throwable error)
  {
    return super.setException(error);
  }

  @Override
  public PnkyPromise<V> alwaysAccept(final ExceptionalConsumer<? super V> onSuccess,
      final ExceptionalConsumer<Throwable> onFailure)
  {
    return alwaysAccept(onSuccess, onFailure, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public PnkyPromise<V> alwaysAccept(final ExceptionalConsumer<? super V> onSuccess,
      final ExceptionalConsumer<Throwable> onFailure, final Executor executor)
  {
    Pnky<V> pnky = create();

    Futures.addCallback(this, new FutureCallback<V>()
    {
      @Override
      public void onSuccess(@Nullable final V result)
      {
        try
        {
          onSuccess.accept(result);
          pnky.resolve(result);
        }
        catch (Exception e)
        {
          pnky.reject(e);
        }
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        // TODO: handle cancellation
        try
        {
          onFailure.accept(t);
          pnky.reject(t);
        }
        catch (Exception e)
        {
          pnky.reject(e);
        }
      }
    }, executor);

    return pnky;
  }

  @Override
  public <O> PnkyPromise<O> alwaysTransform(final ExceptionalFunction<? super V, O> onSuccess,
      final ExceptionalFunction<Throwable, O> onFailure)
  {
    return alwaysTransform(onSuccess, onFailure, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public <O> PnkyPromise<O> alwaysTransform(final ExceptionalFunction<? super V, O> onSuccess,
      final ExceptionalFunction<Throwable, O> onFailure, final Executor executor)
  {
    Pnky<O> pnky = create();

    Futures.addCallback(this, new FutureCallback<V>()
    {
      @Override
      public void onSuccess(@Nullable final V result)
      {
        try
        {
          O newValue = onSuccess.apply(result);
          pnky.resolve(newValue);
        }
        catch (Exception e)
        {
          pnky.reject(e);
        }
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        // TODO: handle cancellation
        try
        {
          O newValue = onFailure.apply(t);
          pnky.resolve(newValue);
        }
        catch (Exception e)
        {
          pnky.reject(e);
        }
      }
    }, executor);

    return pnky;
  }

  @Override
  public <O> PnkyPromise<O> alwaysCompose(final ExceptionalFunction<? super V, PnkyPromise<O>> onSuccess,
      final ExceptionalFunction<Throwable, PnkyPromise<O>> onFailure)
  {
    return alwaysCompose(onSuccess, onFailure, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public <O> PnkyPromise<O> alwaysCompose(final ExceptionalFunction<? super V, PnkyPromise<O>> onSuccess,
      final ExceptionalFunction<Throwable, PnkyPromise<O>> onFailure, final Executor executor)
  {
    Pnky<O> pnky = create();

    Futures.addCallback(this, new FutureCallback<V>()
    {
      @Override
      public void onSuccess(@Nullable final V result)
      {
        try
        {
          onSuccess.apply(result).alwaysAccept((newValue, error) ->
          {
            if (error != null)
            {
              pnky.reject(error);
            }
            else
            {
              pnky.resolve(newValue);
            }
          });
        }
        catch (Exception e)
        {
          pnky.reject(e);
        }
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        // TODO: handle cancellation
        try
        {
          PnkyPromise<O> newPromise = onFailure.apply(t);
          newPromise.alwaysAccept((newValue, error) ->
          {
            if (error != null)
            {
              pnky.reject(error);
            }
            else
            {
              pnky.resolve(newValue);
            }
          });
        }
        catch (Exception e)
        {
          pnky.reject(e);
        }
      }
    }, executor);

    return pnky;
  }

  @Override
  public PnkyPromise<V> alwaysAccept(final ExceptionalBiConsumer<? super V, Throwable> handler)
  {
    return alwaysAccept(handler, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public PnkyPromise<V> alwaysAccept(final ExceptionalBiConsumer<? super V, Throwable> handler,
      final Executor executor)
  {
    Pnky<V> pnky = create();

    Futures.addCallback(this, new FutureCallback<V>()
    {
      @Override
      public void onSuccess(@Nullable final V result)
      {
        try
        {
          handler.accept(result, null);
          pnky.resolve(result);
        }
        catch (Exception e)
        {
          pnky.reject(e);
        }
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        // TODO: handle cancellation
        try
        {
          handler.accept(null, t);
          pnky.reject(t);
        }
        catch (Exception e)
        {
          pnky.reject(e);
        }
      }
    }, executor);

    return pnky;
  }

  @Override
  public <O> PnkyPromise<O> alwaysTransform(final ExceptionalBiFunction<? super V, Throwable, O> handler)
  {
    return alwaysTransform(handler, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public <O> PnkyPromise<O> alwaysTransform(final ExceptionalBiFunction<? super V, Throwable, O> handler,
      final Executor executor)
  {
    Pnky<O> pnky = create();

    Futures.addCallback(this, new FutureCallback<V>()
    {
      @Override
      public void onSuccess(@Nullable final V result)
      {
        try
        {
          O newValue = handler.apply(result, null);
          pnky.resolve(newValue);
        }
        catch (Exception e)
        {
          pnky.reject(e);
        }
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        // TODO: handle cancellation
        try
        {
          O newValue = handler.apply(null, t);
          pnky.resolve(newValue);
        }
        catch (Exception e)
        {
          pnky.reject(e);
        }
      }
    }, executor);

    return pnky;
  }

  @Override
  public <O> PnkyPromise<O> alwaysCompose(
      final ExceptionalBiFunction<? super V, Throwable, PnkyPromise<O>> handler)
  {
    return alwaysCompose(handler, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public <O> PnkyPromise<O> alwaysCompose(
      final ExceptionalBiFunction<? super V, Throwable, PnkyPromise<O>> handler,
      final Executor executor)
  {
    Pnky<O> pnky = create();

    Futures.addCallback(this, new FutureCallback<V>()
    {
      @Override
      public void onSuccess(@Nullable final V result)
      {
        try
        {
          handler.apply(result, null).alwaysAccept((newValue, error) ->
          {
            if (error != null)
            {
              pnky.reject(error);
            }
            else
            {
              pnky.resolve(newValue);
            }
          });
        }
        catch (Exception e)
        {
          pnky.reject(e);
        }
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        // TODO: handle cancellation
        try
        {
          PnkyPromise<O> newPromise = handler.apply(null, t);
          newPromise.alwaysAccept((newValue, error) ->
          {
            if (error != null)
            {
              pnky.reject(error);
            }
            else
            {
              pnky.resolve(newValue);
            }
          });
        }
        catch (Exception e)
        {
          pnky.reject(e);
        }
      }
    }, executor);

    return pnky;
  }

  @Override
  public PnkyPromise<V> thenAccept(final ExceptionalConsumer<? super V> onSuccess)
  {
    return thenAccept(onSuccess, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public PnkyPromise<V> thenAccept(final ExceptionalConsumer<? super V> onSuccess, final Executor executor)
  {
    Pnky<V> pnky = create();

    Futures.addCallback(this, new FutureCallback<V>()
    {
      @Override
      public void onSuccess(@Nullable final V result)
      {
        try
        {
          onSuccess.accept(result);
          pnky.resolve(result);
        }
        catch (Exception e)
        {
          pnky.reject(e);
        }
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        pnky.reject(t);
      }
    }, executor);

    return pnky;
  }

  @Override
  public <O> PnkyPromise<O> thenTransform(final ExceptionalFunction<? super V, O> onSuccess)
  {
    return thenTransform(onSuccess, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public <O> PnkyPromise<O> thenTransform(final ExceptionalFunction<? super V, O> onSuccess,
      final Executor executor)
  {
    Pnky<O> pnky = create();

    Futures.addCallback(this, new FutureCallback<V>()
    {
      @Override
      public void onSuccess(@Nullable final V result)
      {
        try
        {
          O newValue = onSuccess.apply(result);
          pnky.resolve(newValue);
        }
        catch (Exception e)
        {
          pnky.reject(e);
        }
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        pnky.reject(t);
      }
    }, executor);

    return pnky;
  }

  @Override
  public <O> PnkyPromise<O> thenCompose(final ExceptionalFunction<? super V, PnkyPromise<O>> onSuccess)
  {
    return thenCompose(onSuccess, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public <O> PnkyPromise<O> thenCompose(final ExceptionalFunction<? super V, PnkyPromise<O>> onSuccess,
      final Executor executor)
  {
    Pnky<O> pnky = create();

    Futures.addCallback(this, new FutureCallback<V>()
    {
      @Override
      public void onSuccess(@Nullable final V result)
      {
        try
        {
          onSuccess.apply(result).alwaysAccept((newValue, error) ->
          {
            if (error != null)
            {
              pnky.reject(error);
            }
            else
            {
              pnky.resolve(newValue);
            }
          });
        }
        catch (Exception e)
        {
          pnky.reject(e);
        }
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        pnky.reject(t);
      }
    }, executor);

    return pnky;
  }

  @Override
  public PnkyPromise<V> onFailure(final ExceptionalConsumer<Throwable> onFailure)
  {
    return onFailure(onFailure, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public PnkyPromise<V> onFailure(final ExceptionalConsumer<Throwable> onFailure,
      final Executor executor)
  {
    Pnky<V> pnky = create();

    Futures.addCallback(this, new FutureCallback<V>()
    {
      @Override
      public void onSuccess(@Nullable final V result)
      {
        pnky.resolve(result);
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        try
        {
          onFailure.accept(t);
          pnky.reject(t);
        }
        catch (Exception e)
        {
          pnky.reject(e);
        }
      }
    }, executor);

    return pnky;
  }

  @Override
  public PnkyPromise<V> withFallback(final ExceptionalFunction<Throwable, V> onFailure)
  {
    return withFallback(onFailure, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public PnkyPromise<V> withFallback(final ExceptionalFunction<Throwable, V> onFailure,
      final Executor executor)
  {
    Pnky<V> pnky = create();

    Futures.addCallback(this, new FutureCallback<V>()
    {
      @Override
      public void onSuccess(@Nullable final V result)
      {
        pnky.resolve(result);
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        try
        {
          V newValue = onFailure.apply(t);
          pnky.resolve(newValue);
        }
        catch (Exception e)
        {
          pnky.reject(e);
        }
      }
    }, executor);

    return pnky;
  }

  @Override
  public PnkyPromise<V> composeFallback(
      final ExceptionalFunction<Throwable, PnkyPromise<V>> onFailure)
  {
    return composeFallback(onFailure, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public PnkyPromise<V> composeFallback(
      final ExceptionalFunction<Throwable, PnkyPromise<V>> onFailure, final Executor executor)
  {
    Pnky<V> pnky = create();

    Futures.addCallback(this, new FutureCallback<V>()
    {
      @Override
      public void onSuccess(@Nullable final V result)
      {
        pnky.resolve(result);
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        try
        {
          onFailure.apply(t).alwaysAccept((result, error) ->
          {
            if (error != null)
            {
              pnky.reject(error);
            }
            else
            {
              pnky.resolve(result);
            }
          });
        }
        catch (Exception e)
        {
          pnky.reject(e);
        }
      }
    }, executor);

    return pnky;
  }

  // =======================
  // Public utility methods
  // =======================

  /**
   * Execute an action on the provided {@link Executor} and provide a promise indicating when the
   * action is complete or has failed.
   *
   * @param runnable
   *          the action to perform
   * @param executor
   *          the executor to process the action on
   * @return a new promise that will be completed after the runnable has executed
   */
  public static PnkyPromise<Void> runAsync(ExceptionalRunnable runnable,
      Executor executor)
  {
    Pnky<Void> pnky = Pnky.create();

    executor.execute(() ->
    {
      try
      {
        runnable.run();
        pnky.resolve(null);
      }
      catch (Exception e)
      {
        pnky.reject(e);
      }
    });

    return pnky;
  }

  /**
   * Execute a task that will provide an initial result when it is executed and provide a promise
   * that will be resolved with that value on success.
   *
   * @param <V>
   *          the type of value returned from the supplier
   * @param supplier
   *          function to invoke to provide an initial value
   * @param executor
   *          the executor to process the action on
   * @return a new promise that will be completed with the result of the supplier after it has
   *         executed
   */
  public static <V> PnkyPromise<V> supplyAsync(final ExceptionalSupplier<V> supplier,
      final Executor executor)
  {
    Pnky<V> pnky = Pnky.create();

    executor.execute(() ->
    {
      try
      {
        V value = supplier.get();
        pnky.resolve(value);
      }
      catch (Exception e)
      {
        pnky.reject(e);
      }
    });

    return pnky;
  }

  /**
   * Execute a task that will provide an initial result when it is executed and provide a promise
   * that will be resolved with that value on success.
   *
   * @param <V>
   *          the type of value returned from the supplier
   * @param supplier
   *          function to invoke to provide an initial value
   * @param executor
   *          the executor to process the action on
   * @return a new promise that will be completed with the result of the supplier after it has
   *         executed
   */
  public static <V> PnkyPromise<V> composeAsync(final ExceptionalSupplier<PnkyPromise<V>> supplier,
      final Executor executor)
  {
    Pnky<V> pnky = Pnky.create();

    executor.execute(() ->
    {
      try
      {
        supplier.get().alwaysAccept((result, error) ->
        {
          if (error != null)
          {
            pnky.reject(error);
          }
          else
          {
            pnky.resolve(result);
          }
        });
      }
      catch (Exception e)
      {
        pnky.reject(e);
      }
    });

    return pnky;
  }

  /**
   * Create a promise that is already completed with the provided value when it is returned.
   *
   * @param <V>
   *          the type of promise value
   * @param value
   *          the value to complete the promise with
   * @return a completed promise
   */
  public static <V> PnkyPromise<V> immediatelyComplete(final V value)
  {
    Pnky<V> pnky = create();
    pnky.resolve(value);
    return pnky;
  }

  /**
   * Create a promise that is failed with the provided error when it is returned.
   *
   * @param <V>
   *          the type of promise
   * @param e
   *          the error to fail the promise with
   * @return a failed promise
   */
  public static <V> PnkyPromise<V> immediatelyFailed(final Throwable e)
  {
    Pnky<V> pnky = create();
    pnky.reject(e);
    return pnky;
  }

  /**
   * Create a promise that is completed when all of the provided promises are complete.
   *
   * @param <V>
   *          the type of value for all promises
   * @param promises
   *          the set of promises to watch for completion
   * @return a promise that is completed when all provided promises are complete or failed when any
   *         of the promises fail
   */
  public static <V> PnkyPromise<List<V>> all(Iterable<? extends PnkyPromise<? extends V>> promises)
  {
    Pnky<List<V>> pnky = Pnky.create();
    ListenableFuture<List<V>> result = Futures.allAsList(promises);

    Futures.addCallback(result, new FutureCallback<List<V>>()
    {
      @Override
      public void onSuccess(@Nullable final List<V> result)
      {
        pnky.resolve(result);
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        pnky.reject(t);
      }
    });

    return pnky;
  }

  /**
   * Create a promise that is completed with the results of all successfully completed promises.
   *
   * @param <V>
   *          the type of value for all promises
   * @param promises
   *          the set of promises to watch for completion
   * @return a promise that is completed when all provided promises are complete any any are
   *         successful, or failed if all provided promises fail
   */
  public static <V> PnkyPromise<List<V>> any(Iterable<? extends PnkyPromise<? extends V>> promises)
  {
    Pnky<List<V>> pnky = Pnky.create();
    ListenableFuture<List<V>> result = Futures.successfulAsList(promises);

    Futures.addCallback(result, new FutureCallback<List<V>>()
    {
      @Override
      public void onSuccess(@Nullable final List<V> result)
      {
        pnky.resolve(result);
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        pnky.reject(t);
      }
    });

    return pnky;
  }

  /**
   * Create a promise that is completed when the first of any of the provided promises complete
   * successfully.
   *
   * @param <V>
   *          the type of value for all promises
   * @param promises
   *          the set of promises to watch for completion
   * @return a promise that is completed when the first successfully completed promise is done or
   *         failed if all of the promises fail
   */
  public static <V> PnkyPromise<V> first(Iterable<? extends PnkyPromise<? extends V>> promises)
  {
    Pnky<V> pnky = Pnky.create();

    final AtomicInteger remaining = new AtomicInteger(Iterables.size(promises));

    for (PnkyPromise<? extends V> promise : promises)
    {
      promise.alwaysAccept((result, error) ->
      {
        if (error == null)
        {
          // May be called multiple times but the contract guarantees that only the first call
          // will set the value on the promise
          pnky.resolve(result);
        }
        else if (remaining.decrementAndGet() == 0)
        {
          pnky.reject(error);
        }
      });
    }

    return pnky;
  }
}
