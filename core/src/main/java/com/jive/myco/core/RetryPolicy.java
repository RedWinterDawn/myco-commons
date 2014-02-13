package com.jive.myco.core;

import lombok.Getter;
import lombok.experimental.Builder;

import com.google.common.base.Preconditions;

/**
 * Policy settings relating to retry attempts.
 * 
 * @author David Valeri
 */
final class RetryPolicy
{
  /**
   * The maximum number of times a retry will be attempted after the initial failure.
   */
  @Getter
  private final int maximumRetries;

  /**
   * The initial delay, in milliseconds, before the first retry is attempted.
   */
  @Getter
  private final long initialRetryDelay;

  /**
   * If the retry delays should be calculated using a backoff multiplier instead of a constant
   * delay.
   */
  @Getter
  private final boolean useBackoffMultiplier;

  /**
   * The multiplier to use on top of the previous delay in the event that there are multiple
   * retries and {@link #useBackoffMultiplier} is {@code true}.
   */
  @Getter
  private final double backoffMultiplier;

  @Builder
  private RetryPolicy(final int maximumRetries, final long initialRetryDelay,
      final boolean useBackoffMultiplier,
      final int backoffMultiplier)
  {
    Preconditions.checkArgument(maximumRetries >= 0, "maximumRetries cannot be negative.");
    Preconditions.checkArgument(initialRetryDelay >= 0, "initialRetryDelay cannot be negative.");
    Preconditions.checkArgument(backoffMultiplier > 0,
        "backoffMultiplier must be greater than zero.");

    this.maximumRetries = maximumRetries;
    this.initialRetryDelay = initialRetryDelay;
    this.useBackoffMultiplier = useBackoffMultiplier;
    this.backoffMultiplier = backoffMultiplier;
  }

  /**
   * Calculates the delay for the next retry based on the last delay used and this policy's
   * configuration.
   * 
   * @param lastDelay
   *          the last delay used or 0 if there was no previous retry
   * 
   * @return the delay in milliseconds to use for the next retry attempt
   */
  public long calculateDelay(final long lastDelay)
  {
    if (useBackoffMultiplier && lastDelay > 1)
    {
      return (long) (lastDelay * backoffMultiplier);
    }
    else
    {
      return initialRetryDelay;
    }
  }

  /**
   * A builder for a retry policy.
   */
  public static class RetryPolicyBuilder
  {
    private int maximumRetries = 3;

    private long initialRetryDelay = 1000;

    private boolean useBackoffMultiplier = false;

    private int backoffMultiplier = 2;
  }
}
