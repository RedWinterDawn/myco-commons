package com.jive.myco.commons.callbacks;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * Test for {@link SafeCallbackRunnable}.
 *
 * @author David Valeri
 */
@SuppressWarnings("deprecation")
public class SafeCallbackRunnableTest
{
  @Test
  public void testMultipleInvocationsSuccessFirst()
  {
    final AtomicInteger invocationCount = new AtomicInteger();

    final Callback<Void> callback = new Callback<Void>()
    {
      @Override
      public void onSuccess(final Void result)
      {
        invocationCount.incrementAndGet();
      }

      @Override
      public void onFailure(final Throwable cause)
      {
        invocationCount.incrementAndGet();
      }
    };

    final SafeCallbackRunnable<Void> scr = new SafeCallbackRunnable<Void>(callback)
    {

      @Override
      protected void doRun() throws Exception
      {
        onSuccess(null);
        onSuccess(null);
        onFailure(new Exception());
      }
    };

    scr.run();

    assertEquals(1, invocationCount.get());
  }

  @Test
  public void testMultipleInvocationsFailureFirst()
  {
    final AtomicInteger invocationCount = new AtomicInteger();

    final Callback<Void> callback = new Callback<Void>()
    {
      @Override
      public void onSuccess(final Void result)
      {
        invocationCount.incrementAndGet();
      }

      @Override
      public void onFailure(final Throwable cause)
      {
        invocationCount.incrementAndGet();
      }
    };

    final SafeCallbackRunnable<Void> scr = new SafeCallbackRunnable<Void>(callback)
    {

      @Override
      protected void doRun() throws Exception
      {
        onFailure(new Exception());
        onFailure(new Exception());
        onSuccess(null);
      }
    };

    scr.run();

    assertEquals(1, invocationCount.get());
  }

  @Test
  public void testThrowsExceptionInSuccess()
  {
    final AtomicInteger invocationCount = new AtomicInteger();

    final Callback<Void> callback = new Callback<Void>()
    {
      @Override
      public void onSuccess(final Void result)
      {
        invocationCount.incrementAndGet();
        throw new RuntimeException();
      }

      @Override
      public void onFailure(final Throwable cause)
      {
        invocationCount.incrementAndGet();
      }
    };

    final SafeCallbackRunnable<Void> scr = new SafeCallbackRunnable<Void>(callback)
    {

      @Override
      protected void doRun() throws Exception
      {
        onSuccess(null);
        onSuccess(null);
        onFailure(new Exception());
      }
    };

    scr.run();

    assertEquals(1, invocationCount.get());
  }

  @Test
  public void testThrowsExceptionInFailure()
  {
    final AtomicInteger invocationCount = new AtomicInteger();

    final Callback<Void> callback = new Callback<Void>()
    {
      @Override
      public void onSuccess(final Void result)
      {
        invocationCount.incrementAndGet();
      }

      @Override
      public void onFailure(final Throwable cause)
      {
        invocationCount.incrementAndGet();
        throw new RuntimeException();
      }
    };

    final SafeCallbackRunnable<Void> scr = new SafeCallbackRunnable<Void>(callback)
    {

      @Override
      protected void doRun() throws Exception
      {
        onFailure(new Exception());
        onFailure(new Exception());
        onSuccess(null);
      }
    };

    scr.run();

    assertEquals(1, invocationCount.get());
  }
}
