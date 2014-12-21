package com.jive.myco.commons.callbacks;

import java.util.concurrent.Executor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
@Deprecated
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Util
{
  /**
   * Invoke a callback with the given response on the given executor. If the {@code response} object
   * is an instance of {@code Throwable} then the callback's {@link Callback#onFailure} method will
   * be invoked, otherwise the {@link Callback#onSuccess} method is invoked.
   *
   * @param callback
   *          the callback to invoke
   * @param response
   *          the response to send to the callback
   * @param executor
   *          the executor to run the callback on
   */
  @SuppressWarnings("unchecked")
  public static void runCallback(final Callback<?> callback, final Object response,
      final Executor executor)
  {
    new SafeCallbackRunnable<Object>((Callback<Object>) callback, executor)
    {

      @Override
      protected void doRun() throws Exception
      {
        if (response instanceof Throwable)
        {
          onFailure((Throwable) response);
        }
        else
        {
          onSuccess(response);
        }
      }
    }.run();
  }
}
