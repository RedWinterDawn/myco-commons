package com.jive.myco.callbacks;

import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;

/**
 * This Callback will call onSuccess or OnFailure and catch any
 * exceptions that occur while processing the result. This class
 * will also guarantee that the callback is invoked only once.
 *
 * Date: 2/12/14
 *
 * @author zmorin
 */
@Slf4j
public abstract class SafeCallback<T> implements Callback<T>
{
  public abstract void handleSuccess(T result);

  public abstract void handleFailure(Throwable cause);

  private AtomicBoolean runOnceFlag = new AtomicBoolean(false);

  @Override
  public void onSuccess(T result)
  {
    if (runOnceFlag.compareAndSet(false, true))
    {
      try
      {
        handleSuccess(result);

      }
      catch (Exception e)
      {
        log.error("Exception occurred while handling callback result [{}]", result, e);
      }
    }
  }

  @Override
  public void onFailure(Throwable cause)
  {
    if (runOnceFlag.compareAndSet(false, true))
    {
      try
      {
        handleFailure(cause);
      }
      catch (Exception e)
      {
        log.error("Exception occurred while handling callback failure [{}]", cause, e);
      }
    }
  }
}
