/**
 *
 */
package com.jive.myco.commons.hawtdispatch;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
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
  public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay,
      final TimeUnit unit)
  {
    final ScheduledSettableFuture<V> p = ScheduledSettableFuture.create(delay, unit);
    dispatchQueue.executeAfter(delay, unit, () ->
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
    });
    return p;
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay,
      final long period, final TimeUnit unit)
  {
    // Set the delay to forever, since the command will be run repeatedly until cancelled.
    final ScheduledSettableFuture<?> future =
        ScheduledSettableFuture.create(Long.MAX_VALUE, TimeUnit.SECONDS);
    this.schedule(
        new CancellableRunnable(
            future,
            command,
            TimeUnit.SECONDS.convert(period, unit),
            false),
        initialDelay, unit);
    return future;
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay,
      final long delay, final TimeUnit unit)
  {
    // Set the delay to forever, since the command will be run repeatedly until cancelled.
    final ScheduledSettableFuture<?> future =
        ScheduledSettableFuture.create(Long.MAX_VALUE, TimeUnit.SECONDS);
    this.schedule(
        new CancellableRunnable(
            future,
            command,
            TimeUnit.SECONDS.convert(delay, unit),
            true),
        initialDelay, unit);
    return future;
  }

  /**
   * 
   * A runnable that reschedules itself if the contained future has not been cancelled, completed
   * 
   * @author dharris
   *
   */
  @RequiredArgsConstructor
  private class CancellableRunnable implements Runnable
  {

    private final ScheduledSettableFuture<?> future;
    private final Runnable delegate;
    private final long delayInSeconds;
    private final boolean fixedDelay;
    /**
     * Initial execution time. Used to calculate expected execution times.
     */
    private Instant initial;
    /**
     * Number of executions performed. Used to calculate expected execution times.
     */
    private long execution = 0;

    @Override
    public void run()
    {
      if (initial == null)
      {
        initial = Instant.now();
      }
      
      if (!future.isCancelled() && !future.isDone())
      {
        Instant nextExecution = null;
        if (!fixedDelay)
        {
          nextExecution = initial.plusSeconds(delayInSeconds * ++execution);
        }
        
        try
        {
          delegate.run();
        }
        catch (final Exception e)
        {
          future.setException(e);
        }
        if (fixedDelay)
        {
          schedule(this, delayInSeconds, TimeUnit.SECONDS);
        }
        else
        {
          final long seconds = Duration.between(Instant.now(), nextExecution).getSeconds();
          if (seconds <= 0)
          {
            execute(this);
          }
          else
          {
            schedule(this, seconds, TimeUnit.SECONDS);
          }
        }
      }
    }

  }
}
