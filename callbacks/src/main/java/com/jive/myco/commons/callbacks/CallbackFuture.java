package com.jive.myco.commons.callbacks;

import java.util.concurrent.Future;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * A callback that delivers results to a {@link ListenableFuture}. This class is useful for
 * converting callback based APIs into future based or blocking APIs, particularly for complex
 * sequences of asynchronous actions. {@code ListenableFuture}s may be easily joined together to
 * provide an adhoc Promise framework.
 *
 * @param <T>
 *          the result type returned from the future / provided to the callback
 *
 * @author David Valeri
 */
public class CallbackFuture<T> extends ChainedFuture<T> implements Callback<T>
{
  @Override
  public void onSuccess(final T result)
  {
    set(result);
  }

  @Override
  public void onFailure(final Throwable cause)
  {
    setException(cause);
  }

  /**
   * Returns a future that tracks the state of the callback.
   *
   * @deprecated this object is now a Future itself, so just use the methods on it directly
   *
   * @return this
   */
  @Deprecated
  public Future<T> getFuture()
  {
    return this;
  }
}
