package com.jive.myco.core;

import java.util.List;

/**
 * The user defined strategy for handling events triggered by a {@link RetryManager}.
 * 
 * @author David Valeri
 */
public interface RetryStrategy
{
  /**
   * Called when the retry manager indicates that a retry should be attempted after {@code delay}
   * milliseconds.
   * 
   * @param delay
   *          the delay in milliseconds before another retry should be attempted
   */
  void scheduleRetry(final long delay);

  /**
   * Called when the retry manager encounters a failure. {@link #scheduleRetry(long)} will be
   * invoked after this call completes.
   * 
   * @param retryCount
   *          the number of retries that have been attempted; 0 for the first failure
   * @param cause
   *          the cause of the failure for a given retry attempt or {@code null} if the failure does
   *          not have an associated cause
   */
  void onFailure(final int retryCount, final Throwable cause);
  
  /**
   * Called when the retry manager has exhausted all retry attempts.
   * 
   * @param causes
   *          the ordered list of causes leading up to the ultimate failure; may contain
   *          {@code null} for failures without an associated cause
   */
  void onRetriesExhausted(final List<Throwable> causes);
}
