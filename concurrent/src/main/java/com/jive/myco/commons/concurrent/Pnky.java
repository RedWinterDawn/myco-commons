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
  private static final int WAITING = 0;
  private static final int RUNNING = 1;
  private static final int CANCELLING = 2;

  private final AtomicInteger state = new AtomicInteger(WAITING);

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
   * Completes the promise successfully with a {@code null} value.
   *
   * @return true if this call completed the promise
   */
  public boolean resolve()
  {
    return super.set(null);
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
    return alwaysAccept(onSuccess, onFailure, ImmediateExecutor.getInstance());
  }

  @Override
  public PnkyPromise<V> alwaysAccept(final ExceptionalConsumer<? super V> onSuccess,
      final ExceptionalConsumer<Throwable> onFailure, final Executor executor)
  {
    final Pnky<V> pnky = create();

    addCallback(
        pnky,
        notifyOnSuccess(pnky, onSuccess),
        notifyOnFailure(pnky, onFailure),
        executor);

    return pnky;
  }

  @Override
  public <O> PnkyPromise<O> alwaysTransform(final ExceptionalFunction<? super V, O> onSuccess,
      final ExceptionalFunction<Throwable, O> onFailure)
  {
    return alwaysTransform(onSuccess, onFailure, ImmediateExecutor.getInstance());
  }

  @Override
  public <O> PnkyPromise<O> alwaysTransform(final ExceptionalFunction<? super V, O> onSuccess,
      final ExceptionalFunction<Throwable, O> onFailure, final Executor executor)
  {
    final Pnky<O> pnky = create();

    addCallback(
        pnky,
        transformResult(pnky, onSuccess),
        transformFailure(pnky, onFailure),
        executor);

    return pnky;
  }

  @Override
  public <O> PnkyPromise<O> alwaysCompose(
      final ExceptionalFunction<? super V, PnkyPromise<O>> onSuccess,
      final ExceptionalFunction<Throwable, PnkyPromise<O>> onFailure)
  {
    return alwaysCompose(onSuccess, onFailure, ImmediateExecutor.getInstance());
  }

  @Override
  public <O> PnkyPromise<O> alwaysCompose(
      final ExceptionalFunction<? super V, PnkyPromise<O>> onSuccess,
      final ExceptionalFunction<Throwable, PnkyPromise<O>> onFailure, final Executor executor)
  {
    final Pnky<O> pnky = create();

    addCallback(
        pnky,
        composeResult(pnky, onSuccess),
        composeFailure(pnky, onFailure),
        executor);

    return pnky;
  }

  @Override
  public PnkyPromise<V> alwaysAccept(final ExceptionalBiConsumer<? super V, Throwable> handler)
  {
    return alwaysAccept(handler, ImmediateExecutor.getInstance());
  }

  @Override
  public PnkyPromise<V> alwaysAccept(final ExceptionalBiConsumer<? super V, Throwable> handler,
      final Executor executor)
  {
    final Pnky<V> pnky = create();

    addCallback(
        pnky,
        notifyOnSuccess(pnky, (result) -> handler.accept(result, null)),
        notifyOnFailure(pnky, (error) -> handler.accept(null, error)),
        executor);

    return pnky;
  }

  @Override
  public <O> PnkyPromise<O> alwaysTransform(
      final ExceptionalBiFunction<? super V, Throwable, O> handler)
  {
    return alwaysTransform(handler, ImmediateExecutor.getInstance());
  }

  @Override
  public <O> PnkyPromise<O> alwaysTransform(
      final ExceptionalBiFunction<? super V, Throwable, O> handler,
      final Executor executor)
  {
    final Pnky<O> pnky = create();

    addCallback(
        pnky,
        transformResult(pnky, (result) -> handler.apply(result, null)),
        transformFailure(pnky, (error) -> handler.apply(null, error)),
        executor);

    return pnky;
  }

  @Override
  public <O> PnkyPromise<O> alwaysCompose(
      final ExceptionalBiFunction<? super V, Throwable, PnkyPromise<O>> handler)
  {
    return alwaysCompose(handler, ImmediateExecutor.getInstance());
  }

  @Override
  public <O> PnkyPromise<O> alwaysCompose(
      final ExceptionalBiFunction<? super V, Throwable, PnkyPromise<O>> handler,
      final Executor executor)
  {
    final Pnky<O> pnky = create();

    addCallback(
        pnky,
        composeResult(pnky, (result) -> handler.apply(result, null)),
        composeFailure(pnky, (error) -> handler.apply(null, error)),
        executor);

    return pnky;
  }

  @Override
  public PnkyPromise<V> alwaysRun(final ExceptionalRunnable runnable)
  {
    return alwaysRun(runnable, ImmediateExecutor.getInstance());
  }

  @Override
  public PnkyPromise<V> alwaysRun(final ExceptionalRunnable runnable, final Executor executor)
  {
    final Pnky<V> pnky = create();

    addCallback(
        pnky,
        runAndPassThroughResult(pnky, runnable),
        runAndPassThroughFailure(runnable, pnky),
        executor);

    return pnky;
  }

  @Override
  public PnkyPromise<V> thenAccept(final ExceptionalConsumer<? super V> onSuccess)
  {
    return thenAccept(onSuccess, ImmediateExecutor.getInstance());
  }

  @Override
  public PnkyPromise<V> thenAccept(final ExceptionalConsumer<? super V> onSuccess,
      final Executor executor)
  {
    final Pnky<V> pnky = create();

    addCallback(
        pnky,
        notifyOnSuccess(pnky, onSuccess),
        passThroughException(pnky),
        executor);

    return pnky;
  }

  @Override
  public <O> PnkyPromise<O> thenTransform(final ExceptionalFunction<? super V, O> onSuccess)
  {
    return thenTransform(onSuccess, ImmediateExecutor.getInstance());
  }

  @Override
  public <O> PnkyPromise<O> thenTransform(final ExceptionalFunction<? super V, O> onSuccess,
      final Executor executor)
  {
    final Pnky<O> pnky = create();

    addCallback(
        pnky,
        transformResult(pnky, onSuccess),
        passThroughException(pnky),
        executor);

    return pnky;
  }

  @Override
  public <O> PnkyPromise<O> thenCompose(
      final ExceptionalFunction<? super V, PnkyPromise<O>> onSuccess)
  {
    return thenCompose(onSuccess, ImmediateExecutor.getInstance());
  }

  @Override
  public <O> PnkyPromise<O> thenCompose(
      final ExceptionalFunction<? super V, PnkyPromise<O>> onSuccess,
      final Executor executor)
  {
    final Pnky<O> pnky = create();

    addCallback(
        pnky,
        composeResult(pnky, onSuccess),
        passThroughException(pnky),
        executor);

    return pnky;
  }

  @Override
  public PnkyPromise<V> thenRun(final ExceptionalRunnable runnable)
  {
    return thenRun(runnable, ImmediateExecutor.getInstance());
  }

  @Override
  public PnkyPromise<V> thenRun(final ExceptionalRunnable runnable, final Executor executor)
  {
    final Pnky<V> pnky = create();

    addCallback(
        pnky,
        runAndPassThroughResult(pnky, runnable),
        passThroughException(pnky),
        executor);

    return pnky;
  }

  @Override
  public PnkyPromise<V> onFailure(final ExceptionalConsumer<Throwable> onFailure)
  {
    return onFailure(onFailure, ImmediateExecutor.getInstance());
  }

  @Override
  public PnkyPromise<V> onFailure(final ExceptionalConsumer<Throwable> onFailure,
      final Executor executor)
  {
    final Pnky<V> pnky = create();

    addCallback(
        pnky,
        passThroughResult(pnky),
        notifyOnFailure(pnky, onFailure),
        executor);

    return pnky;
  }

  @Override
  public PnkyPromise<V> withFallback(final ExceptionalFunction<Throwable, V> onFailure)
  {
    return withFallback(onFailure, ImmediateExecutor.getInstance());
  }

  @Override
  public PnkyPromise<V> withFallback(final ExceptionalFunction<Throwable, V> onFailure,
      final Executor executor)
  {
    final Pnky<V> pnky = create();

    addCallback(
        pnky,
        passThroughResult(pnky),
        transformFailure(pnky, onFailure),
        executor);

    return pnky;
  }

  @Override
  public PnkyPromise<V> composeFallback(
      final ExceptionalFunction<Throwable, PnkyPromise<V>> onFailure)
  {
    return composeFallback(onFailure, ImmediateExecutor.getInstance());
  }

  @Override
  public PnkyPromise<V> composeFallback(
      final ExceptionalFunction<Throwable, PnkyPromise<V>> onFailure, final Executor executor)
  {
    final Pnky<V> pnky = create();

    addCallback(
        pnky,
        passThroughResult(pnky),
        composeFailure(pnky, onFailure),
        executor);

    return pnky;
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning)
  {
    // Shield any other action from occurring while we are cancelling
    if (state.compareAndSet(WAITING, CANCELLING))
    {
      if (!super.cancel(false))
      {
        // This should not ever happen
        throw new IllegalStateException(
            "Unable to mark promise as cancelled after changing Pnky state");
      }

      return true;
    }
    return false;
  }

  @Override
  public boolean cancel()
  {
    return cancel(false);
  }

  private void addCallback(final Pnky<?> pnky, final ExceptionalConsumer<V> onSuccess,
      final ExceptionalConsumer<Throwable> onFailure, final Executor executor)
  {
    Futures.addCallback(this, new FutureCallback<V>()
    {
      @Override
      public void onSuccess(@Nullable final V result)
      {
        if (pnky.state.compareAndSet(Pnky.WAITING, Pnky.RUNNING))
        {
          try
          {
            onSuccess.accept(result);
          }
          catch (final Exception e)
          {
            pnky.reject(e);
          }
        }
      }

      @Override
      public void onFailure(@Nonnull final Throwable t)
      {
        if (pnky.state.compareAndSet(Pnky.WAITING, Pnky.RUNNING))
        {
          try
          {
            onFailure.accept(t);
          }
          catch (final Exception e)
          {
            pnky.reject(e);
          }
        }
      }
    }, executor);
  }

  private ExceptionalConsumer<V> passThroughResult(final Pnky<V> pnky)
  {
    return new ExceptionalConsumer<V>()
    {
      @Override
      public void accept(final V input) throws Exception
      {
        pnky.resolve(input);
      }
    };
  }

  private ExceptionalConsumer<Throwable> passThroughException(final Pnky<?> pnky)
  {
    return new ExceptionalConsumer<Throwable>()
    {
      @Override
      public void accept(final Throwable input) throws Exception
      {
        pnky.reject(input);
      }
    };
  }

  private <O> ExceptionalConsumer<Throwable> composeFailure(final Pnky<O> pnky,
      final ExceptionalFunction<Throwable, PnkyPromise<O>> onFailure)
  {
    return new ExceptionalConsumer<Throwable>()
    {
      @Override
      public void accept(final Throwable input) throws Exception
      {
        onFailure.apply(input)
            .alwaysAccept((newValue, error) ->
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
    };
  }

  private <O> ExceptionalConsumer<Throwable> transformFailure(final Pnky<O> pnky,
      final ExceptionalFunction<Throwable, O> onFailure)
  {
    return new ExceptionalConsumer<Throwable>()
    {
      @Override
      public void accept(final Throwable input) throws Exception
      {
        final O newValue = onFailure.apply(input);
        pnky.resolve(newValue);
      }
    };
  }

  private <O> ExceptionalConsumer<V> composeResult(final Pnky<O> pnky,
      final ExceptionalFunction<? super V, PnkyPromise<O>> onSuccess)
  {
    return new ExceptionalConsumer<V>()
    {
      @Override
      public void accept(final V input) throws Exception
      {
        onSuccess.apply(input)
            .alwaysAccept((newValue, error) ->
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
    };
  }

  private <O> ExceptionalConsumer<V> transformResult(final Pnky<O> pnky,
      final ExceptionalFunction<? super V, O> onSuccess)
  {
    return new ExceptionalConsumer<V>()
    {
      @Override
      public void accept(final V input) throws Exception
      {
        final O newValue = onSuccess.apply(input);
        pnky.resolve(newValue);
      }
    };
  }

  private ExceptionalConsumer<V> notifyOnSuccess(final Pnky<V> pnky,
      final ExceptionalConsumer<? super V> onSuccess)
  {
    return new ExceptionalConsumer<V>()
    {
      @Override
      public void accept(final V input) throws Exception
      {
        onSuccess.accept(input);
        pnky.resolve(input);
      }
    };
  }

  private ExceptionalConsumer<Throwable> notifyOnFailure(final Pnky<V> pnky,
      final ExceptionalConsumer<Throwable> onFailure)
  {
    return new ExceptionalConsumer<Throwable>()
    {
      @Override
      public void accept(final Throwable input) throws Exception
      {
        onFailure.accept(input);
        pnky.reject(input);
      }
    };
  }

  private ExceptionalConsumer<V> runAndPassThroughResult(final Pnky<V> pnky,
      final ExceptionalRunnable runnable)
  {
    return new ExceptionalConsumer<V>()
    {
      @Override
      public void accept(final V input) throws Exception
      {
        runnable.run();
        pnky.resolve(input);
      }
    };
  }

  private ExceptionalConsumer<Throwable> runAndPassThroughFailure(
      final ExceptionalRunnable runnable, final Pnky<V> pnky)
  {
    return new ExceptionalConsumer<Throwable>()
    {
      @Override
      public void accept(final Throwable input) throws Exception
      {
        runnable.run();
        pnky.reject(input);
      }
    };
  }

  // =======================
  // Public utility methods
  // =======================

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
      if (pnky.state.compareAndSet(WAITING, RUNNING))
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
      if (pnky.state.compareAndSet(WAITING, RUNNING))
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
      if (pnky.state.compareAndSet(WAITING, RUNNING))
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
   * Creates a new {@link PnkyPromise future} that is successfully completed with a {@code null}
   * value.
   *
   * @param <V>
   *          the type of future
   *
   * @return a new successfully completed {@link PnkyPromise future}
   */
  public static <V> PnkyPromise<V> immediatelyComplete()
  {
    final Pnky<V> pnky = create();
    pnky.resolve(null);
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
   * See {@link #allFailingFast(Iterable)}
   */
  @SafeVarargs
  public static <V> PnkyPromise<List<V>> allFailingFast(final PnkyPromise<? extends V>... promises)
  {
    return allFailingFast(Lists.newArrayList(promises));
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
   * See {@link #all(Iterable)}
   */
  @SafeVarargs
  public static <V> PnkyPromise<List<V>> all(final PnkyPromise<? extends V>... promises)
  {
    return all(Lists.newArrayList(promises));
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

  @SafeVarargs
  public static <V> PnkyPromise<List<V>> any(final PnkyPromise<? extends V>... promises)
  {
    return any(Lists.newArrayList(promises));
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

  /**
   * See {@link #first(Iterable)}
   */
  @SafeVarargs
  public static <V> PnkyPromise<V> first(final PnkyPromise<? extends V>... promises)
  {
    return first(Lists.newArrayList(promises));
  }

  /**
   * Create a new {@link PnkyPromise future} that completes successfully with the result from the
   * provided {@code future} when it is complete. The returned future will complete exceptionally if
   * the provided {@code future} completes exceptionally.
   *
   * @param future
   *          the listenable future to wrap
   * @return a new {@link PnkyPromise future}
   */
  public static <V> PnkyPromise<V> from(final ListenableFuture<? extends V> future)
  {
    final Pnky<V> pnky = Pnky.create();

    Futures.addCallback(future, new FutureCallback<V>()
    {

      @Override
      public void onSuccess(final V result)
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
}
