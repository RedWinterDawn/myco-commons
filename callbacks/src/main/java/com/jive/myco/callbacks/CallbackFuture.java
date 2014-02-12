package com.jive.myco.callbacks;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A callback that delivers results to a {@link java.util.concurrent.Future}. This class is useful for converting
 * callback based APIs into future based or blocking APIs.
 * 
 * @param <T>
 *          the result type returned from the future / provided to the callback
 * @author David Valeri
 */
public class CallbackFuture<T> implements Callback<T>
{
  private final FutureImpl<T> future = new FutureImpl<>();

  @Override
  public void onSuccess(final T result)
  {
    future.onSuccess(result);
  }

  @Override
  public void onFailure(final Throwable cause)
  {
    future.onFailure(cause);
  }

  /**
   * Returns a future that tracks the state of the callback.
   */
  public Future<T> getFuture()
  {
    return future;
  }

  private static final class FutureImpl<T> implements Future<T>
  {
    private final AtomicBoolean done = new AtomicBoolean();
    private final CountDownLatch latch = new CountDownLatch(1);
    private volatile boolean success = false;
    private volatile T value;
    private volatile Throwable cause;

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning)
    {
      return false;
    }

    @Override
    public boolean isCancelled()
    {
      return false;
    }

    @Override
    public boolean isDone()
    {
      return done.get();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException
    {
      latch.await();
      return getInternal();
    }

    @Override
    public T get(final long timeout, final TimeUnit unit) throws InterruptedException,
        ExecutionException,
        TimeoutException
    {
      latch.await(timeout, unit);
      if (!done.get())
      {
        throw new TimeoutException("Did not get result in time");
      }
      return getInternal();
    }

    private void onSuccess(final T value)
    {
      if (done.compareAndSet(false, true))
      {
        this.value = value;
        success = true;
        latch.countDown();
      }
    }

    private void onFailure(final Throwable cause)
    {
      if (done.compareAndSet(false, true))
      {
        this.cause = cause;
        success = false;
        latch.countDown();
      }
    }

    private T getInternal() throws ExecutionException
    {
      if (success)
      {
        return value;
      }
      else
      {
        throw new ExecutionException(cause);
      }
    }
  }
}
