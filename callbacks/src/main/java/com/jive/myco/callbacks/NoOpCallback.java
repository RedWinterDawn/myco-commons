package com.jive.myco.callbacks;

/**
 * A simply callback that performs no actions on success or failure.
 *
 * @param <T> the type of the result on success
 * @author David Valeri
 */
public class NoOpCallback<T> implements Callback<T>
{
  @Override
  public void onSuccess(final T result)
  {
    // No-op
  }

  @Override
  public void onFailure(final Throwable cause)
  {
    // No-op
  }
}
