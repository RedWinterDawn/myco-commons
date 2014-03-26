package com.jive.myco.commons.callbacks;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
 * <p>
 * Once you get a hold of a {@link ListenableFuture} then this class will allow you to do things
 * like the following:
 *
 * <pre>
 * <code>
 *  ListenableFuture&lt;Thing&gt; thing;
 *  ChainedFuture.of(thing)
 *    // translate Thing to an OtherThing
 *    .transform(new AsyncFunction&lt;Thing, OtherThing&gt;(){...})
 *    // Provide alternate output if the above transform fails
 *    .withFallback(new FutureFallback&lt;OtherThing&gt;(){...})
 *    // add a callback to handle the overall success or failure of the chain
 *    .addCallback(new FutureCallback&lt;OtherThing&gt;(){...});
 * </code>
 * </pre>
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

  @Override
  public void addListener(Runnable listener, Executor exec)
  {
    if (delegate == this)
    {
      super.addListener(listener, exec);
    }
    else
    {
      delegate.addListener(listener, exec);
    }
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning)
  {
    if (delegate == this)
    {
      return super.cancel(mayInterruptIfRunning);
    }
    else
    {
      return delegate.cancel(mayInterruptIfRunning);
    }
  }

  @Override
  public boolean isCancelled()
  {
    if (delegate == this)
    {
      return super.isCancelled();
    }
    else
    {
      return delegate.isCancelled();
    }
  }

  @Override
  public boolean isDone()
  {
    if (delegate == this)
    {
      return super.isDone();
    }
    else
    {
      return delegate.isDone();
    }
  }

  @Override
  public V get() throws InterruptedException, ExecutionException
  {
    if (delegate == this)
    {
      return super.get();
    }
    else
    {
      return delegate.get();
    }
  }

  @Override
  public V get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException,
      ExecutionException
  {
    if (delegate == this)
    {
      return super.get(timeout, unit);
    }
    else
    {
      return delegate.get(timeout, unit);
    }
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
   * Note that the return of this method is itself. Adding a callback is a terminal operation for
   * the future, it does not return a new future that can be continually chained AFTER the callback
   * completes. Any additional calls on the returned ChainedFuture will be invoked in parallel to
   * the callback added.
   *
   * @see Futures#addCallback(ListenableFuture, FutureCallback)
   *
   * @return ourself so you can keep chaining
   */
  public ChainedFuture<V> addCallback(FutureCallback<? super V> callback)
  {
    Futures.addCallback(delegate, callback);
    return this;
  }

  /**
   * Same notifications apply as on
   * {@link #addCallback(com.google.common.util.concurrent.FutureCallback)}
   *
   * @see #addCallback(com.google.common.util.concurrent.FutureCallback)
   * @see Futures#addCallback(ListenableFuture, FutureCallback, Executor)
   *
   * @return ourself so you can keep chaining
   */
  public ChainedFuture<V> addCallback(FutureCallback<? super V> callback, Executor executor)
  {
    Futures.addCallback(delegate, callback, executor);
    return this;
  }

  /**
   * Add a callback to invoke on success or failure of this particular future. The same
   * notifications apply as on
   * {@link #addCallback(com.google.common.util.concurrent.FutureCallback)} This variation allows
   * you to make use of our {@link Callback} class to act as the callback instead of wrapping it
   * yourself.
   *
   * @param callback
   *          the callback to invoke on success or failure
   *
   * @see #addCallback(com.google.common.util.concurrent.FutureCallback)
   *
   * @return ourself so you can keep chaining
   */
  public ChainedFuture<V> addCallback(Callback<? super V> callback)
  {
    Futures.addCallback(delegate, CallbackWrappers.wrap(callback));
    return this;
  }

  /**
   * Same rules as {@link #addCallback(com.jive.myco.commons.callbacks.Callback)}.
   *
   * @param callback
   *          the callback to invoke on success or failure
   * @param executor
   *          the executor to execute the callback on
   *
   * @see #addCallback(com.jive.myco.commons.callbacks.Callback, java.util.concurrent.Executor)
   * @see #addCallback(com.jive.myco.commons.callbacks.Callback)
   *
   * @return ourself so you can keep chaining
   */
  public ChainedFuture<V> addCallback(Callback<? super V> callback, Executor executor)
  {
    Futures.addCallback(delegate, CallbackWrappers.wrap(callback), executor);
    return this;
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
  public static <V> ChainedFuture<List<V>> allAsList(ListenableFuture<? extends V>... futures)
  {
    return ChainedFuture.of(Futures.allAsList(futures));
  }

  /**
   * @see Futures#allAsList(Iterable)
   */
  public static <V> ChainedFuture<List<V>> allAsList(
      Iterable<? extends ListenableFuture<? extends V>> futures)
  {
    return ChainedFuture.of(Futures.allAsList(futures));
  }

  /**
   * @see Futures#successfulAsList(ListenableFuture[])
   */
  public static <V> ChainedFuture<List<V>> successfulAsList(
      ListenableFuture<? extends V>... futures)
  {
    return ChainedFuture.of(Futures.successfulAsList(futures));
  }

  /**
   * @see Futures#successfulAsList(Iterable)
   */
  public static <V> ChainedFuture<List<V>> successfulAsList(
      Iterable<? extends ListenableFuture<? extends V>> futures)
  {
    return ChainedFuture.of(Futures.successfulAsList(futures));
  }

  /**
   * Static constructor to build a chainable future out of
   *
   * @param future
   *          the backing listenable future to use
   * @param <V>
   *          the type of result from the future
   * @return a chainable future
   */
  public static <V> ChainedFuture<V> of(ListenableFuture<V> future)
  {
    return new ChainedFuture<>(future);
  }
}
