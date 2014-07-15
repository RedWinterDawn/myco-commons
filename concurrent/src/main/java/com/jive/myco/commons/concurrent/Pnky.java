package com.jive.myco.commons.concurrent;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
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
  /**
   * Returns a new incomplete instance that may be completed at a later time.
   *
   * @return a new instance
   */
  public static <V> Pnky<V> create()
  {
    return new Pnky<>();
  }

  /**
   * Completes the promise successfully with {@code value}.
   *
   * @param value
   *          the value used to complete the promise
   *
   * @return true if this call completed the promise
   */
  public boolean resolve(final V value)
  {
    return super.set(value);
  }

  /**
   * Completes the promise exceptionally with {@code error}.
   *
   * @param error
   *          the error used to exceptionally complete the promise
   *
   * @return true if this call completed the promise
   */
  public boolean reject(@Nonnull final Throwable error)
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
    final Pnky<V> pnky = create();

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
        catch (final Exception e)
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
        catch (final Exception e)
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
    final Pnky<O> pnky = create();

    Futures.addCallback(this, new FutureCallback<V>()
    {
      @Override
      public void onSuccess(@Nullable final V result)
      {
        try
        {
          final O newValue = onSuccess.apply(result);
          pnky.resolve(newValue);
        }
        catch (final Exception e)
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
          final O newValue = onFailure.apply(t);
          pnky.resolve(newValue);
        }
        catch (final Exception e)
        {
          pnky.reject(e);
        }
      }
    }, executor);

    return pnky;
  }

  @Override
  public <O> PnkyPromise<O> alwaysCompose(
      final ExceptionalFunction<? super V, PnkyPromise<O>> onSuccess,
      final ExceptionalFunction<Throwable, PnkyPromise<O>> onFailure)
  {
    return alwaysCompose(onSuccess, onFailure, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public <O> PnkyPromise<O> alwaysCompose(
      final ExceptionalFunction<? super V, PnkyPromise<O>> onSuccess,
      final ExceptionalFunction<Throwable, PnkyPromise<O>> onFailure, final Executor executor)
  {
    final Pnky<O> pnky = create();

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
        catch (final Exception e)
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
          final PnkyPromise<O> newPromise = onFailure.apply(t);
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
        catch (final Exception e)
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
    final Pnky<V> pnky = create();

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
        catch (final Exception e)
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
        catch (final Exception e)
        {
          pnky.reject(e);
        }
      }
    }, executor);

    return pnky;
  }

  @Override
  public <O> PnkyPromise<O> alwaysTransform(
      final ExceptionalBiFunction<? super V, Throwable, O> handler)
  {
    return alwaysTransform(handler, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public <O> PnkyPromise<O> alwaysTransform(
      final ExceptionalBiFunction<? super V, Throwable, O> handler,
      final Executor executor)
  {
    final Pnky<O> pnky = create();

    Futures.addCallback(this, new FutureCallback<V>()
    {
      @Override
      public void onSuccess(@Nullable final V result)
      {
        try
        {
          final O newValue = handler.apply(result, null);
          pnky.resolve(newValue);
        }
        catch (final Exception e)
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
          final O newValue = handler.apply(null, t);
          pnky.resolve(newValue);
        }
        catch (final Exception e)
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
    final Pnky<O> pnky = create();

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
        catch (final Exception e)
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
          final PnkyPromise<O> newPromise = handler.apply(null, t);
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
        catch (final Exception e)
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
  public PnkyPromise<V> thenAccept(final ExceptionalConsumer<? super V> onSuccess,
      final Executor executor)
  {
    final Pnky<V> pnky = create();

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
        catch (final Exception e)
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
    final Pnky<O> pnky = create();

    Futures.addCallback(this, new FutureCallback<V>()
    {
      @Override
      public void onSuccess(@Nullable final V result)
      {
        try
        {
          final O newValue = onSuccess.apply(result);
          pnky.resolve(newValue);
        }
        catch (final Exception e)
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
  public <O> PnkyPromise<O> thenCompose(
      final ExceptionalFunction<? super V, PnkyPromise<O>> onSuccess)
  {
    return thenCompose(onSuccess, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public <O> PnkyPromise<O> thenCompose(
      final ExceptionalFunction<? super V, PnkyPromise<O>> onSuccess,
      final Executor executor)
  {
    final Pnky<O> pnky = create();

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
        catch (final Exception e)
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
    final Pnky<V> pnky = create();

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
        catch (final Exception e)
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
    final Pnky<V> pnky = create();

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
          final V newValue = onFailure.apply(t);
          pnky.resolve(newValue);
        }
        catch (final Exception e)
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
    final Pnky<V> pnky = create();

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
        catch (final Exception e)
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
   * Creates a new {@link PnkyPromise future} that completes when the supplied operation completes,
   * executing the operation on the supplied executor. If the operation completes normally, the
   * returned future completes successfully. If the operation throws an exception, the returned
   * future completes exceptionally with the thrown exception.
   *
   * @param operation
   *          the operation to perform
   * @param executor
   *          the executor to process the action on
   *
   * @return a new {@link PnkyPromise future}
   */
  public static PnkyPromise<Void> runAsync(final ExceptionalRunnable operation,
      final Executor executor)
  {
    final Pnky<Void> pnky = Pnky.create();

    executor.execute(() ->
    {
      try
      {
        operation.run();
        pnky.resolve(null);
      }
      catch (final Exception e)
      {
        pnky.reject(e);
      }
    });

    return pnky;
  }

  /**
   * Creates a new {@link PnkyPromise future} that completes when the supplied operation completes,
   * executing the operation on the supplied executor. If the operation completes normally, the
   * returned future completes successfully with the result of the operation. If the operation
   * throws an exception, the returned future completes exceptionally with the thrown exception.
   *
   * @param operation
   *          the operation to perform
   * @param executor
   *          the executor to process the operation on
   *
   * @return a new {@link PnkyPromise future}
   */
  public static <V> PnkyPromise<V> supplyAsync(final ExceptionalSupplier<V> operation,
      final Executor executor)
  {
    final Pnky<V> pnky = Pnky.create();

    executor.execute(() ->
    {
      try
      {
        final V value = operation.get();
        pnky.resolve(value);
      }
      catch (final Exception e)
      {
        pnky.reject(e);
      }
    });

    return pnky;
  }

  /**
   * Creates a new {@link PnkyPromise future} that completes when the future returned by the
   * supplied operation completes, executing the operation on the supplied executor. If the
   * operation completes normally, the returned future completes with the result of the future
   * returned by the function. If the operation throws an exception, the returned future completes
   * exceptionally with the thrown exception.
   *
   * @param operation
   *          the operation to perform
   * @param executor
   *          the executor to process the operation on
   *
   * @return a new {@link PnkyPromise future}
   */
  public static <V> PnkyPromise<V> composeAsync(final ExceptionalSupplier<PnkyPromise<V>> operation,
      final Executor executor)
  {
    final Pnky<V> pnky = Pnky.create();

    executor.execute(() ->
    {
      try
      {
        operation.get().alwaysAccept((result, error) ->
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
      catch (final Exception e)
      {
        pnky.reject(e);
      }
    });

    return pnky;
  }

  /**
   * Creates a new {@link PnkyPromise future} that is successfully completed with the supplied
   * value.
   *
   * @param <V>
   *          the type of future
   * @param value
   *          the value used to complete the future
   *
   * @return a new successfully completed {@link PnkyPromise future}
   */
  public static <V> PnkyPromise<V> immediatelyComplete(final V value)
  {
    final Pnky<V> pnky = create();
    pnky.resolve(value);
    return pnky;
  }

  /**
   * Creates a new {@link PnkyPromise future} that is exceptionally completed with the supplied
   * error.
   *
   * @param <V>
   *          the type of future
   * @param e
   *          the cause used to complete the future exceptionally
   *
   * @return a new exceptionally completed {@link PnkyPromise future}
   */
  public static <V> PnkyPromise<V> immediatelyFailed(@NonNull final Throwable e)
  {
    final Pnky<V> pnky = create();
    pnky.reject(e);
    return pnky;
  }

  /**
   * Creates a new {@link PnkyPromise future} that completes successfully with the results of the
   * supplied futures that completed successfully, if and only if all of the supplied futures
   * complete successfully. The returned future completes exceptionally as soon as any of the
   * provided futures complete exceptionally.
   *
   * @param <V>
   *          the type of value for all promises
   * @param promises
   *          the promises to watch for completion
   *
   * @return a new {@link PnkyPromise future}
   */
  public static <V> PnkyPromise<List<V>> allFailingFast(
      final Iterable<? extends PnkyPromise<? extends V>> promises)
  {
    final Pnky<List<V>> pnky = Pnky.create();
    final ListenableFuture<List<V>> futureResults = Futures.allAsList(promises);

    Futures.addCallback(futureResults, new FutureCallback<List<V>>()
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
   * Creates a new {@link PnkyPromise future} that completes successfully with the results of the
   * supplied futures that completed successfully, if and only if all of the supplied futures
   * complete successfully. The returned future completes exceptionally with a
   * {@link CombinedException} if any of the provided futures complete exceptionally, but only when
   * all futures have been completed.
   * <p>
   * The returned future will be resolved with the values in the order that the future's were passed
   * to this method (not in completion order).
   * </p>
   * <p>
   * The {@link CombinedException} will contain a {@link CombinedException#getCauses() list} of
   * exceptions mapped to the order of the promises that were passed to this method. A {@code null}
   * value means that corresponding future was completed successfully.
   * </p>
   *
   * @param <V>
   *          the type of value for all promises
   * @param promises
   *          the set of promises to wait on
   * @return a new {@link PnkyPromise future}
   */
  public static <V> PnkyPromise<List<V>> all(
      final Iterable<? extends PnkyPromise<? extends V>> promises)
  {
    final Pnky<List<V>> pnky = Pnky.create();

    final int numberOfPromises = Iterables.size(promises);

    // Special case, no promises to wait for
    if (numberOfPromises == 0)
    {
      return Pnky.immediatelyComplete(Lists.newArrayList());
    }

    final AtomicInteger remaining = new AtomicInteger(numberOfPromises);
    @SuppressWarnings("unchecked")
    final V[] results = (V[]) new Object[numberOfPromises];
    final Throwable[] errors = new Throwable[numberOfPromises];
    final AtomicBoolean failed = new AtomicBoolean();

    int i = 0;
    for (final PnkyPromise<? extends V> promise : promises)
    {
      final int promiseNumber = i++;

      promise.alwaysAccept((result, error) ->
      {
        results[promiseNumber] = result;
        errors[promiseNumber] = error;
        if (error != null)
        {
          failed.set(true);
        }

        if (remaining.decrementAndGet() == 0)
        {
          if (failed.get())
          {
            pnky.reject(new CombinedException(Arrays.asList(errors)));
          }
          else
          {
            pnky.resolve(Arrays.asList(results));
          }
        }
      });
    }

    return pnky;
  }

  /**
   * Creates a new {@link PnkyPromise future} that completes successfully with the results of the
   * supplied futures that completed successfully if one or more of the supplied futures completes
   * successfully. The returned future completes exceptionally if all of the provided futures
   * complete exceptionally.
   *
   * @param <V>
   *          the type of value for all promises
   * @param promises
   *          the promises to watch for completion
   *
   * @return a new {@link PnkyPromise future}
   */
  public static <V> PnkyPromise<List<V>> any(
      final Iterable<? extends PnkyPromise<? extends V>> promises)
  {
    final Pnky<List<V>> pnky = Pnky.create();
    final ListenableFuture<List<V>> futureResults = Futures.successfulAsList(promises);

    Futures.addCallback(futureResults, new FutureCallback<List<V>>()
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
   * Creates a new {@link PnkyPromise future} that completes successfully with the result of the
   * first supplied future that completes successfully if any of the supplied futures complete
   * successfully. The returned future completes exceptionally if all of the provided futures
   * complete exceptionally.
   *
   * @param <V>
   *          the type of value for all promises
   * @param promises
   *          the promises to watch for completion
   *
   * @return a new {@link PnkyPromise future}
   */
  public static <V> PnkyPromise<V> first(final Iterable<? extends PnkyPromise<? extends V>> promises)
  {
    final Pnky<V> pnky = Pnky.create();

    final AtomicInteger remaining = new AtomicInteger(Iterables.size(promises));

    for (final PnkyPromise<? extends V> promise : promises)
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
