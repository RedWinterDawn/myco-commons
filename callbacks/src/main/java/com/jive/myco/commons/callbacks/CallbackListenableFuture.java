package com.jive.myco.commons.callbacks;

import com.google.common.util.concurrent.AbstractFuture;
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
public class CallbackListenableFuture<T> extends AbstractFuture<T> implements Callback<T>
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
}
