package com.jive.myco.commons.callbacks;

import static lombok.AccessLevel.*;
import lombok.NoArgsConstructor;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Utilities for wrapping our {@link Callback} for use with other libraries.
 *
 * @author Brandon Pedersen <bpedersen@getjive.com>
 */
@Deprecated
@NoArgsConstructor(access = PRIVATE)
public final class CallbackWrappers
{
  /**
   * Wrap a {@link Callback} in a {@link FutureCallback} that can be used with Guava's
   * {@link ListenableFuture}
   *
   * @param <T>
   *          the result type of the callback
   * @param callback
   *          the callback to use as the {@code FutureCallback} delegate
   * @return a {@code FutureCallback} wrapping the given callback
   */
  public static <T> FutureCallback<T> wrap(final Callback<T> callback)
  {
    return new FutureCallback<T>()
    {
      @Override
      public void onSuccess(final T result)
      {
        callback.onSuccess(result);
      }

      @Override
      public void onFailure(final Throwable t)
      {
        callback.onFailure(t);
      }
    };
  }
}
