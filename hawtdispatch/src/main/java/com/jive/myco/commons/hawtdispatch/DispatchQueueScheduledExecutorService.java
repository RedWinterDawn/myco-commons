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

  public DispatchQueueScheduledExecutorService(DispatchQueue dispatchQueue)
  {
    super(dispatchQueue);
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
  {
    return this.schedule(() ->
    {
      command.run();
    }, delay, unit);
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit)
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
          catch (Exception e)
          {
            p.setException(e);
          }
        });
      }
      catch (CancellationException e)
      {
        log.debug("Future has been cancelled");
      }
    });
    return p;
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
      TimeUnit unit)
  {
    return this.schedule(() ->
    {
      this.schedule(command, period, TimeUnit.MILLISECONDS);
      command.run();
    }, initialDelay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
      TimeUnit unit)
  {
    return this.schedule(() ->
    {
      command.run();
      this.schedule(command, delay, unit);
    }, initialDelay, unit);
  }
}
