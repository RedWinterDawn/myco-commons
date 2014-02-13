package com.jive.myco.core.retry;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.google.common.collect.Lists;
import com.jive.myco.core.retry.RetryManager;
import com.jive.myco.core.retry.RetryPolicy;
import com.jive.myco.core.retry.RetryStrategy;

@RunWith(MockitoJUnitRunner.class)
public class RetryManagerTest
{
  @Mock
  private RetryStrategy strategy;
  private RetryManager retryManager;

  @Test
  public void testRetryHappened()
  {
    doAnswer(new Answer<Void>()
    {

      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable
      {
        retryManager.onFailure();
        return null;
      }

    }).when(strategy).onFailure(0, null);
    RetryPolicy policy = RetryPolicy.builder().maximumRetries(1).build();
    retryManager = RetryManager.builder().retryPolicy(policy).retryStrategy(strategy).build();

    retryManager.onFailure();
    verify(strategy, times(1)).onRetriesExhausted(anyListOf(Throwable.class));
    verify(strategy, times(1)).scheduleRetry(anyLong());
  }

  @Test
  public void testCausesGetPassedOnRetriesExhausted()
  {
    final IllegalStateException ise = new IllegalStateException();
    final NullPointerException npe = new NullPointerException();
    doAnswer(new Answer<Void>()
    {

      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable
      {
        retryManager.onFailure(npe);
        return null;
      }

    }).when(strategy).onFailure(eq(0), eq(ise));

    RetryPolicy policy = RetryPolicy.builder().maximumRetries(1).build();
    retryManager = RetryManager.builder().retryPolicy(policy).retryStrategy(strategy).build();

    retryManager.onFailure(ise);
    verify(strategy, times(1)).onRetriesExhausted(eq(Lists.<Throwable>newArrayList(ise, npe)));
  }

  @Test
  public void testCorrectDelay()
  {
    RetryPolicy policy = RetryPolicy.builder().maximumRetries(2).
        backoffMultiplier(2).initialRetryDelay(5).useBackoffMultiplier(true).build();
    retryManager = RetryManager.builder().retryPolicy(policy).retryStrategy(strategy).build();

    retryManager.onFailure();
    verify(strategy).scheduleRetry(5);
    reset(strategy);
    retryManager.onFailure();
    verify(strategy).scheduleRetry(10);
  }

  @Test
  public void testSuccessResetsManager()
  {
    RetryPolicy policy = RetryPolicy.builder().maximumRetries(1).build();
    retryManager = RetryManager.builder().retryPolicy(policy).retryStrategy(strategy).build();
    retryManager.onFailure();
    retryManager.onSuccess();
    retryManager.onFailure();
    verify(strategy, never()).onRetriesExhausted(anyListOf(Throwable.class));
  }
}
