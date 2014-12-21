package com.jive.myco.commons.callbacks;

/**
 * Generic callback contract for asynchronous processes.
 *
 * @param <T>
 *          the type of the result upon success
 *
 * @author David Valeri
 */
@Deprecated
public interface Callback<T>
{
  void onSuccess(final T result);

  void onFailure(final Throwable cause);
}
