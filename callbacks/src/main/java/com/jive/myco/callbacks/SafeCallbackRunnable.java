package com.jive.myco.callbacks;

import java.util.concurrent.atomic.AtomicBoolean;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * A runnable that ensures exceptions get propagated back to the callback and also handles the case
 * where the callback throws an exception during invocation.
 *
 * @author David Valeri
 *
 * @param <T>
 *          the input type of the callback
 */
@RequiredArgsConstructor
@ToString
@Slf4j
public abstract class SafeCallbackRunnable<T> implements Runnable
{

  @NonNull
  private final Callback<T> callback;

  private final AtomicBoolean done = new AtomicBoolean();

  @Override
  public final void run()
  {
    try
    {
      doRun();
    }
    catch (final Exception e)
    {
      onFailure(e);
    }
  }

  /**
   * Execute the actual work in a safe manner that ensures proper execution of the callback.
   */
  protected abstract void doRun() throws Exception;

  /**
   * Safely invokes {@link Callback#onSuccess(Object)} with the supplied {@code result}, potentially
   * invoking {@link #onCallbackFailure(Throwable)} in the event that the callback invocation fails.
   * NOTE: This method is public to facilitate easy access from doubly nested anonymous inner
   * classes. The alternative is more verbose. The following pattern demonstrates a typical nested
   * usage:
   *
   * <pre>
   * lifecycleQueue.execute(new SafeCallbackRunnable&lt;Void&gt;(callback)
   * {
   *   &#064;Override
   *   protected void doRun()
   *   {
   *     something.doSomthingAsync(
   *       final SafeCallbackRunnable&lt;Void&gt; that = this;
   *
   *       transport.destroy(new Callback&lt;Void&gt;()
   *       {
   *         &#064;Override
   *         public void onSuccess(final Void result)
   *         {
   *           that.onSuccess(result);
   *         }
   *
   *         &#064;Override
   *         public void onFailure(final Throwable cause)
   *         {
   *           that.onFailure(cause);
   *         }
   *       });
   *     }
   *   }
   * });
   * </pre>
   *
   * @param result
   *          the result to pass to the callback
   */
  public final void onSuccess(final T result)
  {
    if (done.compareAndSet(false, true))
    {
      try
      {
        callback.onSuccess(result);
      }
      catch (final Exception e)
      {
        onCallbackFailure(e);
      }
    }
    else
    {
      log.error("Runnable attempted to invoke the callback more than once.");
    }
  }

  /**
   * Safely invokes {@link Callback#onFailure(Throwable)} with the supplied {@code cause},
   * potentially invoking {@link #onCallbackFailure(Throwable)} in the event that the callback
   * invocation fails. NOTE: This method is public to facilitate easy access from doubly nested
   * anonymous inner classes. The alternative is more verbose. The following pattern demonstrates a
   * typical nested usage:
   *
   * <pre>
   * lifecycleQueue.execute(new SafeCallbackRunnable&lt;Void&gt;(callback)
   * {
   *   &#064;Override
   *   protected void doRun()
   *   {
   *     something.doSomthingAsync(
   *       final SafeCallbackRunnable&lt;Void&gt; that = this;
   *
   *       transport.destroy(new Callback&lt;Void&gt;()
   *       {
   *         &#064;Override
   *         public void onSuccess(final Void result)
   *         {
   *           that.onSuccess(result);
   *         }
   *
   *         &#064;Override
   *         public void onFailure(final Throwable cause)
   *         {
   *           that.onFailure(cause);
   *         }
   *       });
   *     }
   *   }
   * });
   * </pre>
   *
   * @param cause
   *          the cause of failure to pass to the callback
   */
  public final void onFailure(final Throwable cause)
  {
    if (done.compareAndSet(false, true))
    {
      try
      {
        callback.onFailure(cause);
      }
      catch (final Exception e)
      {
        onCallbackFailure(e);
      }
    }
    else
    {
      log.error("Runnable attempted to invoke the callback more than once.", cause);
    }
  }

  /**
   * Invoked when the invocation of the callback triggers an exception.
   *
   * @param callbackError
   *          the exception thrown by the callback
   */
  protected void onCallbackFailure(final Throwable callbackError)
  {
    log.error("Callback threw exception on invocation.", callbackError);
  }
}
