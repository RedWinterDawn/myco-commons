package com.jive.myco.commons.callbacks;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import com.google.common.util.concurrent.MoreExecutors;

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
  private static final String MULTIPLE_INVOCATION_ERROR_MSG =
      "Runnable attempted to invoke the callback more than once.";

  @NonNull
  private final Callback<T> callback;

  private Executor defaultExecutor = MoreExecutors.sameThreadExecutor();

  private final AtomicBoolean done = new AtomicBoolean();

  /**
   * Creates a new {@code SafeCallbackRunnable} with the given {@code callback} and sets the default
   * executor to the provided {@link Executor}.
   * <p>
   * Note that the default executor for {@link #SafeCallbackRunnable(Callback)} is one that runs the
   * callback on the same thread.
   *
   * @param callback
   *          the callback to invoke safely
   * @param defaultExecutor
   *          the default executor to run the callback on when using the {@link #onSuccess(Object)}
   *          or {@link #onFailure(Throwable)} variants where an executor is not provided.
   */
  public SafeCallbackRunnable(final Callback<T> callback, final Executor defaultExecutor)
  {
    this.callback = callback;
    this.defaultExecutor = defaultExecutor;
  }

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
   * Safely invokes {@link Callback#onSuccess(Object)} with the supplied {@code result} on the
   * default executor for this instance, potentially invoking {@link #onCallbackFailure(Throwable)}
   * in the event that the callback invocation fails. NOTE: This method is public to facilitate easy
   * access from doubly nested anonymous inner classes. The alternative is more verbose. The
   * following pattern demonstrates a typical nested usage:
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
    onSuccess(result, defaultExecutor);
  }

  /**
   * Safely invokes {@link Callback#onSuccess(Object)} with the supplied {@code result} on the
   * provided executor, potentially invoking {@link #onCallbackFailure(Throwable)} in the event that
   * the callback invocation fails. NOTE: This method is public to facilitate easy access from
   * doubly nested anonymous inner classes. The alternative is more verbose. The following pattern
   * demonstrates a typical nested usage:
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
   * @param executor
   *          the executor to invoke the callback on
   */
  public final void onSuccess(final T result, final Executor executor)
  {
    if (done.compareAndSet(false, true))
    {
      executor.execute(new Runnable()
      {
        @Override
        public void run()
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
      });
    }
    else
    {
      log.error(MULTIPLE_INVOCATION_ERROR_MSG,
          new IllegalStateException(MULTIPLE_INVOCATION_ERROR_MSG));
    }
  }

  /**
   * Safely invokes {@link Callback#onFailure(Throwable)} with the supplied {@code cause} on the
   * default executor for this instance, potentially invoking {@link #onCallbackFailure(Throwable)}
   * in the event that the callback invocation fails. NOTE: This method is public to facilitate easy
   * access from doubly nested anonymous inner classes. The alternative is more verbose. The
   * following pattern demonstrates a typical nested usage:
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
    onFailure(cause, defaultExecutor);
  }

  /**
   * Safely invokes {@link Callback#onFailure(Throwable)} with the supplied {@code cause} on the
   * provided executor, potentially invoking {@link #onCallbackFailure(Throwable)} in the event that
   * the callback invocation fails. NOTE: This method is public to facilitate easy access from
   * doubly nested anonymous inner classes. The alternative is more verbose. The following pattern
   * demonstrates a typical nested usage:
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
   * @param executor
   *          the executor to invoke the callback on
   */
  public final void onFailure(final Throwable cause, final Executor executor)
  {
    if (done.compareAndSet(false, true))
    {
      executor.execute(new Runnable()
      {
        @Override
        public void run()
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
      });
    }
    else
    {
      log.error(MULTIPLE_INVOCATION_ERROR_MSG,
          new IllegalStateException(MULTIPLE_INVOCATION_ERROR_MSG));
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
