package com.jive.myco.commons.callbacks;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * Test for {@link SafeCallback}.
 *
 * @author David Valeri
 */
public class SafeCallbackTest
{
  @Test
  public void testMultipleInvocationsSuccessFirst()
  {
    final AtomicInteger invocationCount = new AtomicInteger();

    final SafeCallback<Void> callback = new SafeCallback<Void>()
    {
      @Override
      protected void handleSuccess(final Void result)
      {
        invocationCount.incrementAndGet();
      }

      @Override
      protected void handleFailure(final Throwable cause)
      {
        invocationCount.incrementAndGet();
      }
    };

    callback.onSuccess(null);
    callback.onSuccess(null);
    callback.onFailure(new Exception());

    assertEquals(1, invocationCount.get());
  }

  @Test
  public void testMultipleInvocationsFailureFirst()
  {
    final AtomicInteger invocationCount = new AtomicInteger();

    final SafeCallback<Void> callback = new SafeCallback<Void>()
    {
      @Override
      protected void handleSuccess(final Void result)
      {
        invocationCount.incrementAndGet();
      }

      @Override
      protected void handleFailure(final Throwable cause)
      {
        invocationCount.incrementAndGet();
      }
    };

    callback.onFailure(new Exception());
    callback.onFailure(new Exception());
    callback.onSuccess(null);

    assertEquals(1, invocationCount.get());
  }

  @Test
  public void testThrowsExceptionInSuccess()
  {
    final AtomicInteger invocationCount = new AtomicInteger();

    final SafeCallback<Void> callback = new SafeCallback<Void>()
    {
      @Override
      protected void handleSuccess(final Void result)
      {
        invocationCount.incrementAndGet();
        throw new RuntimeException();
      }

      @Override
      protected void handleFailure(final Throwable cause)
      {
        invocationCount.incrementAndGet();
      }
    };

    callback.onSuccess(null);
    callback.onSuccess(null);
    callback.onFailure(new Exception());

    assertEquals(1, invocationCount.get());
  }

  @Test
  public void testThrowsExceptionInFailure()
  {
    final AtomicInteger invocationCount = new AtomicInteger();

    final SafeCallback<Void> callback = new SafeCallback<Void>()
    {
      @Override
      protected void handleSuccess(final Void result)
      {
        invocationCount.incrementAndGet();
      }

      @Override
      protected void handleFailure(final Throwable cause)
      {
        invocationCount.incrementAndGet();
        throw new RuntimeException();
      }
    };

    callback.onFailure(new Exception());
    callback.onFailure(new Exception());
    callback.onSuccess(null);

    assertEquals(1, invocationCount.get());
  }
}
