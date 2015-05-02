package com.jive.myco.commons.retry;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.Builder;

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
  @Getter
  @Setter
  @NonNull
  private volatile RetryPolicy retryPolicy;

  @Getter
  @Setter
  @NonNull
  private RetryStrategy retryStrategy;

  private volatile RetryState retryState;

  @Builder
  private RetryManager(@NonNull final RetryPolicy retryPolicy,
      @NonNull final RetryStrategy retryStrategy)
  {
    this.retryPolicy = retryPolicy;
    this.retryStrategy = retryStrategy;
    this.retryState = new RetryState();
  }

  /**
   * Clients invoke this method upon successful completion of the action that this manager is
   * handling retries for. This method resets internal state and readies this manager for use on a
   * subsequent request.
   */
  public void onSuccess()
  {
    retryState = new RetryState();
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
    final RetryState currentState = retryState;

    if (retryPolicy.getMaximumRetries() >= 0)
    {
      currentState.causes.add(cause);
      if (currentState.causes.size() > 10)
      {
        currentState.causes.remove(0);
      }
    }

    if (willRetry())
    {
      currentState.lastDelay = retryPolicy.calculateDelay(currentState.lastDelay);
      retryStrategy.onFailure(currentState.retryCounter++, cause);
      retryStrategy.scheduleRetry(currentState.lastDelay);
    }
    else
    {
      currentState.retryCounter = 0;
      currentState.lastDelay = 0;
      retryStrategy.onRetriesExhausted(new ArrayList<>(currentState.causes));
      currentState.causes.clear();
    }
  }

  public boolean willRetry()
  {
    return retryState.retryCounter < retryPolicy.getMaximumRetries()
        || retryPolicy.getMaximumRetries() < 0;
  }

  public int getRetryCounter()
  {
    return retryState.retryCounter;
  }

  public long getLastDelay()
  {
    return retryState.lastDelay;
  }

  @ToString
  private static class RetryState
  {
    private volatile int retryCounter = 0;

    private volatile long lastDelay = 0;

    private final List<Throwable> causes = new LinkedList<>();
  }
}
