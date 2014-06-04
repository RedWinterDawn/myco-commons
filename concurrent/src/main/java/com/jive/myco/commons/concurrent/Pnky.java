package com.jive.myco.commons.concurrent;

import java.util.concurrent.Executor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.jive.myco.commons.function.ExceptionalBiConsumer;
import com.jive.myco.commons.function.ExceptionalBiFunction;
import com.jive.myco.commons.function.ExceptionalConsumer;
import com.jive.myco.commons.function.ExceptionalFunction;
import com.jive.myco.commons.function.ExceptionalRunnable;
import com.jive.myco.commons.function.ExceptionalSupplier;

/**
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Pnky<V> extends AbstractFuture<V> implements PnkyPromise<V>
{
  public static <V> Pnky<V> create()
  {
    return new Pnky<>();
  }

  public boolean set(V value)
  {
    return super.set(value);
  }

  public boolean setException(@Nonnull Throwable error)
  {
    return super.setException(error);
  }

  @Override
  public PnkyPromise<V> thenHandle(final ExceptionalConsumer<V> onSuccess,
      final ExceptionalConsumer<Throwable> onFailure)
  {
    return thenHandle(onSuccess, onFailure, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public PnkyPromise<V> thenHandle(final ExceptionalConsumer<V> onSuccess,
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
          pnky.set(result);
        }
        catch (Exception e)
        {
          pnky.setException(e);
        }
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        // TODO: handle cancellation
        try
        {
          onFailure.accept(t);
          pnky.setException(t);
        }
        catch (Exception e)
        {
          pnky.setException(e);
        }
      }
    }, executor);

    return pnky;
  }

  @Override
  public <O> PnkyPromise<O> thenTransform(final ExceptionalFunction<V, O> onSuccess,
      final ExceptionalFunction<Throwable, O> onFailure)
  {
    return thenTransform(onSuccess, onFailure, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public <O> PnkyPromise<O> thenTransform(final ExceptionalFunction<V, O> onSuccess,
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
          pnky.set(newValue);
        }
        catch (Exception e)
        {
          pnky.setException(e);
        }
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        // TODO: handle cancellation
        try
        {
          O newValue = onFailure.apply(t);
          pnky.set(newValue);
        }
        catch (Exception e)
        {
          pnky.setException(e);
        }
      }
    }, executor);

    return pnky;
  }

  @Override
  public <O> PnkyPromise<O> thenCompose(final ExceptionalFunction<V, PnkyPromise<O>> onSuccess,
      final ExceptionalFunction<Throwable, PnkyPromise<O>> onFailure)
  {
    return thenCompose(onSuccess, onFailure, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public <O> PnkyPromise<O> thenCompose(final ExceptionalFunction<V, PnkyPromise<O>> onSuccess,
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
          onSuccess.apply(result).thenHandle((newValue, error) ->
          {
            if (error != null)
            {
              pnky.setException(error);
            }
            else
            {
              pnky.set(newValue);
            }
          });
        }
        catch (Exception e)
        {
          pnky.setException(e);
        }
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        // TODO: handle cancellation
        try
        {
          PnkyPromise<O> newPromise = onFailure.apply(t);
          newPromise.thenHandle((newValue, error) ->
          {
            if (error != null)
            {
              pnky.setException(error);
            }
            else
            {
              pnky.setException(t);
            }
          });
        }
        catch (Exception e)
        {
          pnky.setException(e);
        }
      }
    }, executor);

    return pnky;
  }

  @Override
  public PnkyPromise<V> thenHandle(final ExceptionalBiConsumer<V, Throwable> handler)
  {
    return thenHandle(handler, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public PnkyPromise<V> thenHandle(final ExceptionalBiConsumer<V, Throwable> handler,
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
          pnky.set(result);
        }
        catch (Exception e)
        {
          pnky.setException(e);
        }
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        // TODO: handle cancellation
        try
        {
          handler.accept(null, t);
          pnky.setException(t);
        }
        catch (Exception e)
        {
          pnky.setException(e);
        }
      }
    }, executor);

    return pnky;
  }

  @Override
  public <O> PnkyPromise<O> thenTransform(final ExceptionalBiFunction<V, Throwable, O> handler)
  {
    return thenTransform(handler, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public <O> PnkyPromise<O> thenTransform(final ExceptionalBiFunction<V, Throwable, O> handler,
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
          pnky.set(newValue);
        }
        catch (Exception e)
        {
          pnky.setException(e);
        }
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        // TODO: handle cancellation
        try
        {
          O newValue = handler.apply(null, t);
          pnky.set(newValue);
        }
        catch (Exception e)
        {
          pnky.setException(e);
        }
      }
    }, executor);

    return pnky;
  }

  @Override
  public <O> PnkyPromise<O> thenCompose(
      final ExceptionalBiFunction<V, Throwable, PnkyPromise<O>> handler)
  {
    return thenCompose(handler, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public <O> PnkyPromise<O> thenCompose(
      final ExceptionalBiFunction<V, Throwable, PnkyPromise<O>> handler,
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
          handler.apply(result, null).thenHandle((newValue, error) ->
          {
            if (error != null)
            {
              pnky.setException(error);
            }
            else
            {
              pnky.set(newValue);
            }
          });
        }
        catch (Exception e)
        {
          pnky.setException(e);
        }
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        // TODO: handle cancellation
        try
        {
          PnkyPromise<O> newPromise = handler.apply(null, t);
          newPromise.thenHandle((newValue, error) ->
          {
            if (error != null)
            {
              pnky.setException(error);
            }
            else
            {
              pnky.setException(t);
            }
          });
        }
        catch (Exception e)
        {
          pnky.setException(e);
        }
      }
    }, executor);

    return pnky;
  }

  @Override
  public PnkyPromise<V> thenHandle(final ExceptionalConsumer<V> onSuccess)
  {
    return thenHandle(onSuccess, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public PnkyPromise<V> thenHandle(final ExceptionalConsumer<V> onSuccess, final Executor executor)
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
          pnky.set(result);
        }
        catch (Exception e)
        {
          pnky.setException(e);
        }
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        pnky.setException(t);
      }
    }, executor);

    return pnky;
  }

  @Override
  public <O> PnkyPromise<O> thenTransform(final ExceptionalFunction<V, O> onSuccess)
  {
    return thenTransform(onSuccess, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public <O> PnkyPromise<O> thenTransform(final ExceptionalFunction<V, O> onSuccess,
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
          pnky.set(newValue);
        }
        catch (Exception e)
        {
          pnky.setException(e);
        }
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        pnky.setException(t);
      }
    }, executor);

    return pnky;
  }

  @Override
  public <O> PnkyPromise<O> thenCompose(final ExceptionalFunction<V, PnkyPromise<O>> onSuccess)
  {
    return thenCompose(onSuccess, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public <O> PnkyPromise<O> thenCompose(final ExceptionalFunction<V, PnkyPromise<O>> onSuccess,
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
          onSuccess.apply(result).thenHandle((newValue, error) ->
          {
            if (error != null)
            {
              pnky.setException(error);
            }
            else
            {
              pnky.set(newValue);
            }
          });
        }
        catch (Exception e)
        {
          pnky.setException(e);
        }
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        pnky.setException(t);
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
        pnky.set(result);
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        try
        {
          onFailure.accept(t);
          pnky.setException(t);
        }
        catch (Exception e)
        {
          pnky.setException(e);
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
        pnky.set(result);
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        try
        {
          V newValue = onFailure.apply(t);
          pnky.set(newValue);
        }
        catch (Exception e)
        {
          pnky.setException(e);
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
        pnky.set(result);
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        try
        {
          onFailure.apply(t).thenHandle((result, error) ->
          {
            if (error != null)
            {
              pnky.setException(error);
            }
            else
            {
              pnky.set(result);
            }
          });
        }
        catch (Exception e)
        {
          pnky.setException(e);
        }
      }
    }, executor);

    return pnky;
  }

  // ---
  // Public utility methods
  //

  public static PnkyPromise<Void> runAsync(ExceptionalRunnable runnable,
      Executor executor)
  {
    Pnky<Void> pnky = Pnky.create();

    executor.execute(() ->
    {
      try
      {
        runnable.run();
        pnky.set(null);
      }
      catch (Exception e)
      {
        pnky.setException(e);
      }
    });

    return pnky;
  }

  public static <V> PnkyPromise<V> supplyAsync(final ExceptionalSupplier<V> supplier,
      final Executor executor)
  {
    Pnky<V> pnky = Pnky.create();

    executor.execute(() ->
    {
      try
      {
        V value = supplier.get();
        pnky.set(value);
      }
      catch (Exception e)
      {
        pnky.setException(e);
      }
    });

    return pnky;
  }

  public static <V> PnkyPromise<V> composeAsync(final ExceptionalSupplier<PnkyPromise<V>> supplier,
      final Executor executor)
  {
    Pnky<V> pnky = Pnky.create();

    executor.execute(() ->
    {
      try
      {
        supplier.get().thenHandle((result, error) ->
        {
          if (error != null)
          {
            pnky.setException(error);
          }
          else
          {
            pnky.set(result);
          }
        });
      }
      catch (Exception e)
      {
        pnky.setException(e);
      }
    });

    return pnky;
  }

  public static <V> PnkyPromise<V> immediateFuture(final V value)
  {
    Pnky<V> pnky = create();
    pnky.set(value);
    return pnky;
  }

  public static <V> PnkyPromise<V> immediateFailedFuture(final Throwable e)
  {
    Pnky<V> pnky = create();
    pnky.setException(e);
    return pnky;
  }
}
