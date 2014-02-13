package com.jive.myco.core;

import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Builder;

/**
 * A manager for tracking retry attempts, scheduling delayed retries, and notifying a strategy of
 * ultimate failure after exhausting all retries.
 * <p/>
 * The manager maintains internal state and should not be shared across multiple threads without
 * external synchronization.
 * 
 * @author David Valeri
 */
public class RetryManager
{
  private final List<Throwable> causes = new LinkedList<>();

  @Getter
  @Setter
  @NonNull
  private volatile RetryPolicy retryPolicy = RetryPolicy.builder()
      .initialRetryDelay(50)
      .useBackoffMultiplier(true)
      .maximumRetries(3)
      .build();

  @Getter
  @Setter
  @NonNull
  private RetryStrategy retryStrategy;

  @Getter
  private volatile int retryCounter = 0;

  @Getter
  private volatile long lastDelay = 0;

  @Builder
  private RetryManager(@NonNull final RetryPolicy retryPolicy,
      @NonNull final RetryStrategy retryStrategy)
  {
    this.retryPolicy = retryPolicy;
    this.retryStrategy = retryStrategy;
  }

  /**
   * Clients invoke this method upon successful completion of the action that this manager is
   * handling retries for. This method resets internal state and readies this manager for use on a
   * subsequent request.
   */
  public void onSuccess()
  {
    retryCounter = 0;
    lastDelay = 0;
  }

  /**
   * Called when the action that this manager is handling retries for fails. The cause associated
   * with the failure is {@code null}.
   */
  public void onFailure()
  {
    onFailure(null);
  }

  /**
   * Called when the action that this manager is handling retries for fails. {@code cause} is
   * associated with the failure.
   * 
   * @param cause
   *          the cause of the failure of the action
   */
  public void onFailure(final Throwable cause)
  {
    causes.add(cause);
    
    retryCounter++;

    if (retryCounter > retryPolicy.getMaximumRetries())
    {
      retryCounter = 0;
      retryStrategy.onRetriesExhausted(causes);
    }
    else
    {
      lastDelay = retryPolicy.calculateDelay(lastDelay);
      retryStrategy.onFailure(retryCounter - 1, cause);
      retryStrategy.scheduleRetry(lastDelay);
    }
  }
}
