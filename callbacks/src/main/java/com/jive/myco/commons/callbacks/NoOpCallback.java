package com.jive.myco.commons.callbacks;

/**
 * A simple callback that performs no actions on success or failure.
 *
 * @param <T>
 *          the type of the result on success
 *
 * @author David Valeri
 */
public class NoOpCallback<T> implements Callback<T>
{
  private static final NoOpCallback<?> INSTANCE = new NoOpCallback<>();

  /**
   * Returns a singleton instance of a {@link NoOpCallback}.
   *
   * @return a singleton instance of the callback
   */
  @SuppressWarnings("unchecked")
  public static <T> NoOpCallback<T> getInstance()
  {
    return (NoOpCallback<T>) INSTANCE;
  }

  /**
   * Constructs a new instance.
   *
   * @deprecated use {@link #getInstance()} instead to reduce GC overhead
   */
  @Deprecated
  public NoOpCallback()
  {
    // No-op
  }

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
