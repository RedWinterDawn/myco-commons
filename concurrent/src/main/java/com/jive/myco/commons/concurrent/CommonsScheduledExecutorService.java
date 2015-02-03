package com.jive.myco.commons.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper for a ScheduledExecutorService
 * @author Binh Tran
 */
public class CommonsScheduledExecutorService extends CommonExecutorService implements ScheduledExecutorService
{
  private final ScheduledExecutorService delegate;
  private final boolean cancelSubsequenceActions;

  public static ScheduledExecutorService wrap(
      ScheduledExecutorService delegate,
      Thread.UncaughtExceptionHandler handler,
      boolean cancelSubsequenceActions)
  {
    return new CommonsScheduledExecutorService(delegate, handler, cancelSubsequenceActions);
  }

  public CommonsScheduledExecutorService (ScheduledExecutorService delegate,
      Thread.UncaughtExceptionHandler handler, boolean cancelSubsequenceActions)
  {
    super(delegate, handler);
    this.delegate = delegate;
    this.cancelSubsequenceActions = cancelSubsequenceActions;
  }

  @Override
  public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit)
  {
    return this.delegate.schedule(() ->
    {
      try
      {
        command.run();
      }
      catch (Throwable t)
      {
        handler.uncaughtException(Thread.currentThread(), t);
        if (cancelSubsequenceActions)
        {
          throw t;
        }
      }
    }, delay, unit);
  }

  @Override
  public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay,
      final TimeUnit unit)
  {
    return this.delegate.schedule(() -> {
      try
      {
        return callable.call();
      }
      catch (Throwable t)
      {
        handler.uncaughtException(Thread.currentThread(), t);
        throw t;
      }
    }, delay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay,
      final long period,
      final TimeUnit unit)
  {
    return this.delegate.scheduleAtFixedRate(() ->
    {
      try
      {
        command.run();
      }
      catch (Throwable t)
      {
        handler.uncaughtException(Thread.currentThread(), t);
        if (cancelSubsequenceActions)
        {
          throw t;
        }
      }
    }, initialDelay, period, unit);

  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay,
      final long delay,
      final TimeUnit unit)
  {
    return this.delegate.scheduleWithFixedDelay(() ->
    {
      try
      {
        command.run();
      }
      catch (Throwable t)
      {
        handler.uncaughtException(Thread.currentThread(), t);
        if (cancelSubsequenceActions)
        {
          throw t;
        }
      }
    }, initialDelay, delay, unit);
  }

}
