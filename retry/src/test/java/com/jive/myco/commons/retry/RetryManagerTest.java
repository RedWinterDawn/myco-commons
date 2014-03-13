package com.jive.myco.commons.retry;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.google.common.collect.Lists;

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
      public Void answer(final InvocationOnMock invocation) throws Throwable
      {
        retryManager.onFailure();
        return null;
      }

    }).when(strategy).onFailure(0, null);
    final RetryPolicy policy = RetryPolicy.builder().maximumRetries(1).build();
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
      public Void answer(final InvocationOnMock invocation) throws Throwable
      {
        retryManager.onFailure(npe);
        return null;
      }

    }).when(strategy).onFailure(eq(0), eq(ise));

    final RetryPolicy policy = RetryPolicy.builder().maximumRetries(1).build();
    retryManager = RetryManager.builder().retryPolicy(policy).retryStrategy(strategy).build();

    retryManager.onFailure(ise);
    verify(strategy, times(1)).onRetriesExhausted(eq(Lists.<Throwable>newArrayList(ise, npe)));
  }

  @Test
  public void testCorrectDelay()
  {
    final RetryPolicy policy = RetryPolicy.builder().maximumRetries(2).
        backoffMultiplier(2).initialRetryDelay(5).build();
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
    final RetryPolicy policy = RetryPolicy.builder().maximumRetries(1).build();
    retryManager = RetryManager.builder().retryPolicy(policy).retryStrategy(strategy).build();
    retryManager.onFailure();
    retryManager.onSuccess();
    retryManager.onFailure();
    verify(strategy, never()).onRetriesExhausted(anyListOf(Throwable.class));
  }

  @Test
  public void testIndefiniteRetries()
  {
    final RetryPolicy policy = RetryPolicy.builder().maximumRetries(-1).initialRetryDelay(5).build();
    retryManager = RetryManager.builder().retryPolicy(policy).retryStrategy(strategy).build();

    for (int i = 0; i < 10000; i++)
    {
      retryManager.onFailure();
    }
    verify(strategy, times(10000)).scheduleRetry(eq(5L));
  }

  @Test
  public void testMaxRetryTime()
  {
    final InOrder inOrder = inOrder(strategy);
    final RetryPolicy policy = RetryPolicy.builder()
        .maximumRetries(-1)
        .initialRetryDelay(5L)
        .maximumRetryDelay(5L)
        .maximumRetryDelay(20)
        .backoffMultiplier(2.0)
        .build();
    retryManager = RetryManager.builder().retryPolicy(policy).retryStrategy(strategy).build();

    for (int i = 0; i < 5; i++)
    {
      retryManager.onFailure();
    }
    inOrder.verify(strategy).scheduleRetry(5L);
    inOrder.verify(strategy).scheduleRetry(10L);
    inOrder.verify(strategy).scheduleRetry(20L);
    inOrder.verify(strategy).scheduleRetry(20L);
    inOrder.verify(strategy).scheduleRetry(20L);
    inOrder.verifyNoMoreInteractions();
  }
}
