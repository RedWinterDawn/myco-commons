package com.jive.myco.commons.callbacks;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * A {@link ListenableFuture} with chaining abilities through the use of Guava's {@link Futures}
 * class.
 *
 * @param <V>
 *          the type of result returned from the future
 *
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ChainedFuture<V> extends AbstractFuture<V>
{
  private final ListenableFuture<V> delegate;

  /**
   * Only useful for sub-classes that will actually implement {@link AbstractFuture#set(Object)} and
   * {@link AbstractFuture#setException(Throwable)} where the delegate {@code ListenableFuture} will
   * actually be {@code this}, otherwise, just make use of the static {@link #of} constructor.
   */
  protected ChainedFuture()
  {
    this.delegate = this;
  }

  /**
   * @see Futures#withFallback(ListenableFuture, FutureFallback)
   */
  public ChainedFuture<V> withFallback(FutureFallback<? extends V> fallback)
  {
    return ChainedFuture.of(Futures.withFallback(delegate, fallback));
  }

  /**
   * @see Futures#withFallback(ListenableFuture, FutureFallback, Executor)
   */
  public ChainedFuture<V> withFallback(FutureFallback<? extends V> fallback, Executor executor)
  {
    return ChainedFuture.of(Futures.withFallback(delegate, fallback, executor));
  }

  /**
   * @see Futures#transform(ListenableFuture, AsyncFunction)
   */
  public <O> ChainedFuture<O> transform(AsyncFunction<? super V, ? extends O> function)
  {
    return ChainedFuture.of(Futures.transform(delegate, function));
  }

  /**
   * @see Futures#transform(ListenableFuture, AsyncFunction, Executor)
   */
  public <O> ChainedFuture<O> transform(AsyncFunction<? super V, ? extends O> function,
      Executor executor)
  {
    return ChainedFuture.of(Futures.transform(delegate, function, executor));
  }

  /**
   * @see Futures#transform(ListenableFuture, Function)
   */
  public <O> ChainedFuture<O> transform(Function<? super V, ? extends O> function)
  {
    return ChainedFuture.of(Futures.transform(delegate, function));
  }

  /**
   * @see Futures#transform(ListenableFuture, Function, Executor)
   */
  public <O> ChainedFuture<O> transform(Function<? super V, ? extends O> function, Executor executor)
  {
    return ChainedFuture.of(Futures.transform(delegate, function, executor));
  }

  /**
   * @see Futures#nonCancellationPropagating(ListenableFuture)
   */
  public ChainedFuture<V> nonCancellationPropagating()
  {
    return ChainedFuture.of(Futures.nonCancellationPropagating(delegate));
  }

  /**
   * @see Futures#addCallback(ListenableFuture, FutureCallback)
   */
  public void addCallback(FutureCallback<? super V> callback)
  {
    Futures.addCallback(delegate, callback);
  }

  /**
   * @see Futures#addCallback(ListenableFuture, FutureCallback, Executor)
   */
  public void addCallback(FutureCallback<? super V> callback, Executor executor)
  {
    Futures.addCallback(delegate, callback, executor);
  }

  /**
   * @see Futures#get(Future, Class)
   */
  public <X extends Exception> V get(Class<X> exceptionClass) throws X
  {
    return Futures.get(delegate, exceptionClass);
  }

  /**
   * @see Futures#get(Future, long, TimeUnit, Class)
   */
  public <X extends Exception> V get(long timeout, TimeUnit unit, Class<X> exceptionClass) throws X
  {
    return Futures.get(delegate, timeout, unit, exceptionClass);
  }

  /**
   * @see Futures#getUnchecked(Future)
   */
  public V getUnchecked()
  {
    return Futures.getUnchecked(delegate);
  }

  /**
   * @see Futures#allAsList(ListenableFuture[])
   */
  public static <V> ChainedFuture allAsList(ListenableFuture<? extends V>... futures)
  {
    return ChainedFuture.of(Futures.allAsList(futures));
  }

  /**
   * @see Futures#allAsList(Iterable)
   */
  public static <V> ChainedFuture allAsList(
      Iterable<? extends ListenableFuture<? extends V>> futures)
  {
    return ChainedFuture.of(Futures.allAsList(futures));
  }

  /**
   * @see Futures#successfulAsList(ListenableFuture[])
   */
  public static <V> ChainedFuture successfulAsList(ListenableFuture<? extends V>... futures)
  {
    return ChainedFuture.of(Futures.successfulAsList(futures));
  }

  /**
   * @see Futures#successfulAsList(Iterable)
   */
  public static <V> ChainedFuture successfulAsList(
      Iterable<? extends ListenableFuture<? extends V>> futures)
  {
    return ChainedFuture.of(Futures.successfulAsList(futures));
  }

  /**
   * Static constructor to build a chainable future out of
   *
   * @param future
   *          the backing listenable future to use
   * @param <T>
   *          the type of result from the future
   * @return a chainable future
   */
  public static <T> ChainedFuture<T> of(ListenableFuture<T> future)
  {
    return new ChainedFuture<>(future);
  }
}
