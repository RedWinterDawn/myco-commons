package com.jive.myco.commons.concurrent;

import static com.jive.myco.commons.concurrent.CommonsScheduledExecutorService.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class CommonsScheduledExecutorServiceTest
{

  private ScheduledExecutorService executorService;
  private AtomicInteger exceptionCaught;
  private final Thread.UncaughtExceptionHandler handler = (t, e) -> exceptionCaught.incrementAndGet();

  @Before
  public void setup() throws Exception
  {
    executorService = Executors.newScheduledThreadPool(50,
        new ThreadFactoryBuilder()
        .setDaemon(true)
        .setUncaughtExceptionHandler(handler)
        .build());

    executorService = wrap(executorService, handler, false);  // Comment out this line to verify jvm behavior

    exceptionCaught = new AtomicInteger();
  }

  @Test
  public void testSubmit() throws Exception
  {
    final Future<Void> future = executorService.submit((Callable<Void>) () ->
    {
      throw new RuntimeException();
    });

    try
    {
      future.get();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    assertEquals(1, exceptionCaught.get());
  }

  @Test
  public void testSubmitWithResult() throws Exception
  {
    final Future<Boolean> submit = executorService.submit(() ->
    {
      throw new RuntimeException();
    }, Boolean.TRUE);

    try
    {
      submit.get();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    assertEquals(1, exceptionCaught.get());
  }

  @Test
  public void testSubmitWithoutResult() throws Exception
  {
    final Future<Object> future = executorService.submit(() ->
    {
      throw new RuntimeException();
    });

    try
    {
      future.get();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    assertEquals(1, exceptionCaught.get());
  }

  @Test
  public void testSchedule() throws Exception
  {
    final ScheduledFuture<?> schedule = executorService.schedule((Runnable) () ->
    {
      throw new RuntimeException();
    }, 1, TimeUnit.SECONDS);

    try
    {
      schedule.get();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    assertEquals(1, exceptionCaught.get());
  }

  @Test
  public void testSchedule1() throws Exception
  {
    final ScheduledFuture<?> schedule = executorService.schedule(() ->
    {
      throw new IOException();    // Checked exception
    }
        , 1, TimeUnit.SECONDS);

    try
    {
      schedule.get();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    assertEquals(1, exceptionCaught.get());

  }

  @Test
  public void testScheduleAtFixedRate() throws Exception
  {
    CountDownLatch latch = new CountDownLatch(3);

    final ScheduledFuture<?> scheduledFuture = executorService.scheduleAtFixedRate(() ->
    {
      try
      {
        if (latch.getCount() != 0)
        {
          throw new RuntimeException();
        }
      }
      finally
      {
        latch.countDown();
      }
    }, 0, 1, TimeUnit.SECONDS);

    latch.await(10, TimeUnit.SECONDS);

    try
    {
      scheduledFuture.cancel(false);
      scheduledFuture.get();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    assertEquals(3, exceptionCaught.get());
  }

  @Test
  public void testScheduleWithFixedDelay() throws Exception
  {
    CountDownLatch latch = new CountDownLatch(3);

    final ScheduledFuture<?> scheduledFuture = executorService.scheduleWithFixedDelay(() ->
    {
      try
      {
        if (latch.getCount() != 0)
        {
          throw new RuntimeException();
        }
      }
      finally
      {
        latch.countDown();
      }
    }, 0, 1, TimeUnit.SECONDS);

    latch.await(10, TimeUnit.SECONDS);

    try
    {
      scheduledFuture.cancel(false);
      scheduledFuture.get();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    assertEquals(3, exceptionCaught.get());
  }

  @Test
  public void testScheduleAtFixedRateNoContinuation() throws Exception
  {
    CountDownLatch latch = new CountDownLatch(3);

    executorService = Executors.newScheduledThreadPool(50,
        new ThreadFactoryBuilder()
            .setDaemon(true)
            .setUncaughtExceptionHandler(handler)
            .build());

    executorService = wrap(executorService, handler, true);  // Comment out this line to verify jvm behavior

    final ScheduledFuture<?> scheduledFuture = executorService.scheduleAtFixedRate(() ->
    {
      try
      {
        if (latch.getCount() != 0)
        {
          throw new RuntimeException();
        }
      }
      finally
      {
        latch.countDown();
      }
    }, 0, 1, TimeUnit.SECONDS);

    latch.await(4, TimeUnit.SECONDS);

    try
    {
      scheduledFuture.cancel(false);
      scheduledFuture.get();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    assertEquals(1, exceptionCaught.get());
  }

  @Test
  public void testScheduleWithFixedDelayNoContinuation() throws Exception
  {
    CountDownLatch latch = new CountDownLatch(3);

    executorService = Executors.newScheduledThreadPool(50,
        new ThreadFactoryBuilder()
            .setDaemon(true)
            .setUncaughtExceptionHandler(handler)
            .build());

    executorService = wrap(executorService, handler, true);  // Comment out this line to verify jvm behavior

    final ScheduledFuture<?> scheduledFuture = executorService.scheduleWithFixedDelay(() ->
    {
      try
      {
        if (latch.getCount() != 0)
        {
          throw new RuntimeException();
        }
      }
      finally
      {
        latch.countDown();
      }
    }, 0, 1, TimeUnit.SECONDS);

    latch.await(4, TimeUnit.SECONDS);

    try
    {
      scheduledFuture.cancel(false);
      scheduledFuture.get();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

    assertEquals(1, exceptionCaught.get());
  }
}
