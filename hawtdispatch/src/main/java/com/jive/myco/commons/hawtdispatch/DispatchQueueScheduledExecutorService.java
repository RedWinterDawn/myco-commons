/**
 *
 */
package com.jive.myco.commons.hawtdispatch;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.fusesource.hawtdispatch.DispatchQueue;

/**
 * @author Army Dave
 *
 */
@Slf4j
public class DispatchQueueScheduledExecutorService extends DispatchQueueExecutorService implements
    ScheduledExecutorService
{

  public DispatchQueueScheduledExecutorService(final DispatchQueue dispatchQueue)
  {
    super(dispatchQueue);
  }

  @Override
  public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit)
  {
    return this.schedule(() ->
    {
      command.run();
      return null;
    }, delay, unit);
  }

  @Override
  public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit)
  {
    final ScheduledSettableFuture<V> p = ScheduledSettableFuture.create(delay, unit);
    dispatchQueue.executeAfter(delay, unit, () ->
    {
      try
      {
        if (p.isCancelled())
        {
          p.setException(new CancellationException());
          return;
        }

        this.execute(() ->
        {
          try
          {
            p.set(callable.call());
          }
          catch (final Exception e)
          {
            p.setException(e);
          }
        });
      }
      catch (final CancellationException e)
      {
        log.debug("Future has been cancelled");
      }
    });
    return p;
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period,
      final TimeUnit unit)
  {
    final Runnable runner = new Runnable()
    {
      @Override
      public void run()
      {
        DispatchQueueScheduledExecutorService.this.schedule(this, period, unit);
        command.run();
      }
    };
    return this.schedule(runner, initialDelay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay,
      final TimeUnit unit)
  {
    return this.schedule(new Runnable()
    {
      @Override
      public void run()
      {
        command.run();
        DispatchQueueScheduledExecutorService.this.schedule(this, delay, unit);
      }
    }, initialDelay, unit);
  }
}
