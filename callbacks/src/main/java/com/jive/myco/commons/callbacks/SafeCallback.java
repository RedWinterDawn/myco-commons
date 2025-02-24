package com.jive.myco.commons.callbacks;

import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;

/**
 * This callback will call {@link #handleSuccess(Object)} or {@link #handleFailure(Throwable)},
 * catching any exceptions that occur while processing the result. This class will also guarantee
 * that the callback is invoked only once, logging a warning when invoked more than once.
 *
 * @author zmorin
 */
@Deprecated
@Slf4j
public abstract class SafeCallback<T> implements Callback<T>
{
  private static final String MULTIPLE_INVOCATION_ERROR_MSG = "Callback invoked multiple times.";

  private final AtomicBoolean runOnceFlag = new AtomicBoolean(false);

  @Override
  public void onSuccess(final T result)
  {
    if (runOnceFlag.compareAndSet(false, true))
    {
      try
      {
        handleSuccess(result);

      }
      catch (final Exception e)
      {
        log.error("Exception occurred while handling callback result [{}]", result, e);
      }
    }
    else
    {
      log.warn(MULTIPLE_INVOCATION_ERROR_MSG, new IllegalStateException(
          MULTIPLE_INVOCATION_ERROR_MSG));
    }
  }

  @Override
  public void onFailure(final Throwable cause)
  {
    if (runOnceFlag.compareAndSet(false, true))
    {
      try
      {
        handleFailure(cause);
      }
      catch (final Exception e)
      {
        log.error("Exception occurred while handling callback failure [{}]", cause, e);
      }
    }
    else
    {
      log.warn(MULTIPLE_INVOCATION_ERROR_MSG, new IllegalStateException(
          MULTIPLE_INVOCATION_ERROR_MSG));
    }
  }

  protected abstract void handleSuccess(final T result);

  protected abstract void handleFailure(final Throwable cause);
}
