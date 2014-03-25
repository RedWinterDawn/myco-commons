package com.jive.myco.commons.retry;

import lombok.Getter;
import lombok.experimental.Builder;

import com.google.common.base.Preconditions;

/**
 * Policy settings relating to retry attempts.
 *
 * @author David Valeri
 */
@Builder
public final class RetryPolicy
{
  /**
   * The maximum number of times a retry will be attempted after the initial failure.
   * A value less than one indicates that we will try indefinitely.
   */
  @Getter
  private final int maximumRetries;

  /**
   * The maximum amount of time in milliseconds between retries.
   */
  @Getter
  private final long maximumRetryDelay;

  /**
   * The initial delay, in milliseconds, before the first retry is attempted.
   */
  @Getter
  private final long initialRetryDelay;

  /**
   * The multiplier to use on top of the previous delay in the event that there are multiple
   * retries. If < 0 then this multiplier will not be used
   */
  @Getter
  private final double backoffMultiplier;

  private RetryPolicy(final int maximumRetries, final long maximumRetryDelay,
      final long initialRetryDelay, final double backoffMultiplier)
  {
    Preconditions.checkArgument(initialRetryDelay >= 0, "initialRetryDelay cannot be negative.");

    this.maximumRetries = maximumRetries;
    this.maximumRetryDelay = maximumRetryDelay;
    this.initialRetryDelay = initialRetryDelay;
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
    if (backoffMultiplier > 0 && lastDelay > 1)
    {
      return Math.min((long) (lastDelay * backoffMultiplier), maximumRetryDelay);
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

    private long maximumRetryDelay = Long.MAX_VALUE;

    private long initialRetryDelay = 1000;

    private double backoffMultiplier = -1;
  }
}
