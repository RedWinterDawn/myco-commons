package com.jive.myco.commons.metrics;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

/**
 * A simple asynchronous "appender". This appender is not intended for fancy asynchronous use cases,
 * does not supply an asynchronous API, and mostly supports a single threaded background processor
 * handling the synchronous / blocking processing of the {@link #append(Object) appended} events.
 * The handling occurs on a dedicated thread and may perform blocking operations.
 *
 * @param <E>
 *          the type of the events
 *
 * @author David Valeri
 */
public abstract class AsyncAppender<E>
{
  public static final int DEFAULT_QUEUE_SIZE = 256;
  public static final long DEFAULT_HANDLE_EVENT_RETRY_DELAY = 500L;
  public static final int DEFAULT_HANDLE_EVENT_RETRY_COUNT = -1;
  public static final long DEFAULT_GRACEFUL_SHUTDOWN_TIMEOUT = 1000L;
  public static final long DEFAULT_FORCED_SHUTDOWN_TIMEOUT = 1000L;

  private final String id;
  private final boolean discarding;
  private final int discardThreshold;
  private final boolean blockOnFull;
  private final boolean retryHandleEvent;
  private final long retryHandleEventDelay;
  private final int retryHandleEventCount;
  private final long gracefulShutdownTimeout;
  private final long forcedShutdownTimeout;
  private final BlockingQueue<Runnable> queue;

  private ExecutorService workerService;
  private volatile boolean run = false;

  public AsyncAppender(
      final String id,
      final int queueSize,
      final boolean discarding,
      final Integer discardThreshold,
      final boolean blockOnFull,
      final boolean retryHandleEvent,
      final Long retryHandleEventDelay,
      final Integer retryHandleEventCount,
      final Long gracefulShutdownTimeout,
      final Long forcedShutdownTimeout)
  {
    this.id = id;
    this.discarding = discarding;

    this.discardThreshold = Optional
        .ofNullable(discardThreshold)
        // Default to 80% full
        .orElseGet(() -> queueSize / 5);

    this.blockOnFull = blockOnFull;
    this.retryHandleEvent = retryHandleEvent;
    this.retryHandleEventDelay = Optional
        .ofNullable(retryHandleEventDelay)
        .orElse(DEFAULT_HANDLE_EVENT_RETRY_DELAY);
    this.retryHandleEventCount = Optional
        .ofNullable(retryHandleEventCount)
        .orElse(DEFAULT_HANDLE_EVENT_RETRY_COUNT);
    this.gracefulShutdownTimeout = Optional
        .ofNullable(gracefulShutdownTimeout)
        .orElse(DEFAULT_GRACEFUL_SHUTDOWN_TIMEOUT);
    this.forcedShutdownTimeout = Optional
        .ofNullable(forcedShutdownTimeout)
        .orElse(DEFAULT_FORCED_SHUTDOWN_TIMEOUT);
    this.queue = new ArrayBlockingQueue<>(queueSize);

    // Validate the configuration

    if (this.discarding && this.discardThreshold > queueSize)
    {
      throw new IllegalArgumentException();
    }
    else if (this.retryHandleEvent && this.retryHandleEventDelay < 0)
    {
      throw new IllegalArgumentException();
    }
    else if (this.gracefulShutdownTimeout <= 0)
    {
      throw new IllegalArgumentException();
    }
    else if (this.forcedShutdownTimeout <= 0)
    {
      throw new IllegalArgumentException();
    }
  }

  public final String getId()
  {
    return id;
  }

  public final synchronized void init()
  {
    if (workerService == null)
    {
      workerService =
          new ThreadPoolExecutor(1, 1, Long.MAX_VALUE, TimeUnit.MILLISECONDS,
              queue,
              // NOT using Guava builder here because we would like to keep as many dependencies out
              // of this module as possible.
              new ThreadFactory()
              {
                private final AtomicInteger counter = new AtomicInteger();

                private final ThreadGroup group = (System.getSecurityManager() != null)
                    ? System.getSecurityManager().getThreadGroup() : Thread.currentThread()
                        .getThreadGroup();

                private final String nameTemplate = "async-appender-" + id + "-worker-%d";

                @Override
                public Thread newThread(final Runnable r)
                {
                  final Thread t =
                      new Thread(group, r,
                          String.format(nameTemplate, counter.getAndIncrement()), 0);

                  if (t.isDaemon())
                  {
                    t.setDaemon(false);
                  }

                  if (t.getPriority() != Thread.NORM_PRIORITY)
                  {
                    t.setPriority(Thread.NORM_PRIORITY);
                  }

                  return t;
                }
              },
              // Hybrid rejection handler that supports blocking
              (runnable, executor) ->
              {
                try
                {
                  if (blockOnFull)
                  {
                    executor.getQueue().put(runnable);
                  }
                  else
                  {
                    throw new RejectedExecutionException("Task " + runnable.toString() +
                        " rejected from " + executor.toString());
                  }
                }
                catch (final InterruptedException e)
                {
                  throw new RejectedExecutionException("Interrupted.", e);
                }
              });

      run = true;
    }
  }

  public final synchronized void destroy() throws InterruptedException
  {
    if (workerService != null)
    {
      // Try graceful shutdown
      workerService.shutdown();

      // If graceful fails, be more direct
      if (!workerService.awaitTermination(gracefulShutdownTimeout, TimeUnit.MILLISECONDS))
      {
        // Signal to stop retrying / trying and interrupt it
        run = false;
        workerService.shutdownNow();

        // Wait again
        workerService.awaitTermination(forcedShutdownTimeout, TimeUnit.MILLISECONDS);
      }

      workerService = null;
    }
  }

  /**
   * Appends an event to the queue for asynchronous processing. Access to this method does not
   * require synchronization; however, clients should take care not to invoke this method when the
   * appender has not been initialized or has been destroyed.
   *
   * @param event
   *          the event to append asynchronously
   *
   * @throws NullPointerException
   *           if {@code event} is {@code null}
   * @throws InterruptedException
   *           if {@link #blockOnFull} is {@code true} and the append is interrupted while waiting
   *           for space on the internal queue
   */
  public final void append(final E event) throws InterruptedException
  {
    boolean discard = false;

    if (discarding && queue.size() > discardThreshold)
    {
      discard = isDiscardable(event);
    }

    if (!discard)
    {
      try
      {
        if (workerService != null)
        {
          workerService.submit(() -> handleEventInternal(event));
        }
        else
        {
          handleRejectedEvent(event);
        }
      }
      catch (final RejectedExecutionException e)
      {
        handleRejectedEvent(event);
      }
    }
    else
    {
      handleDiscardedEvent(event);
    }
  }

  /**
   * Returns true if the given event may be discarded based on the appender crossing the
   * {@link #discardThreshold} when {@link #discarding} is {@code true}.
   *
   * @param event
   *          the event to determine discardability for
   *
   * @return {@code true} if {@code event} may be discarded, false otherwise
   */
  protected boolean isDiscardable(final E event)
  {
    return true;
  }

  /**
   * Called when an event is discarded via {@link #isDiscardable(Object)}.
   *
   * @param event
   *          the event that was discarded
   */
  protected void handleDiscardedEvent(final E event)
  {
    // No-op
  }

  /**
   * Called with events that were rejected due to the internal queue being full with
   * {@link #blockOnFull} being {@code false} or the appender not being in an initialized state.
   *
   * @param event
   *          the event that was rejected
   */
  protected void handleRejectedEvent(final E event)
  {
    // No-op
  }

  /**
   * Processes the queued event.
   *
   * @param event
   *          the event to process
   */
  protected abstract void handleEvent(final E event) throws InterruptedException;

  /**
   * Logs {@code message} at the {@code TRACE} level, replacing tokens with the values supplied via
   * {@code args} as in {@link Logger#trace(String, Object...)}.
   *
   * @param message
   *          the message template to log
   * @param args
   *          the args supplied to the template
   */
  protected abstract void trace(final String message, final Object... args);

  /**
   * Logs {@code message} at the {@code DEBUG} level, replacing tokens with the values supplied via
   * {@code args} as in {@link Logger#debug(String, Object...)}.
   *
   * @param message
   *          the message template to log
   * @param args
   *          the args supplied to the template
   */
  protected abstract void debug(final String message, final Object... args);

  /**
   * Logs {@code message} at the {@code INFO} level, replacing tokens with the values supplied via
   * {@code args} as in {@link Logger#info(String, Object...)}.
   *
   * @param message
   *          the message template to log
   * @param args
   *          the args supplied to the template
   */
  protected abstract void info(final String message, final Object... args);

  /**
   * Logs {@code message} at the {@code WARN} level, replacing tokens with the values supplied via
   * {@code args} as in {@link Logger#debug(String, Object...)}.
   *
   * @param message
   *          the message template to log
   * @param args
   *          the args supplied to the template
   */
  protected abstract void warn(final String message, final Object... args);

  /**
   * Logs {@code message} at the {@code ERROR} level, replacing tokens with the values supplied via
   * {@code args} as in {@link Logger#debug(String, Object...)}.
   *
   * @param message
   *          the message template to log
   * @param args
   *          the args supplied to the template
   */
  protected abstract void error(final String message, final Object... args);

  private void handleEventInternal(final E event)
  {
    boolean handled = false;

    int retriesRemaining = retryHandleEventCount;

    while (!Thread.interrupted() && run && !handled)
    {
      try
      {
        handleEvent(event);
        handled = true;
      }
      catch (final InterruptedException e)
      {
        if (!run)
        {
          Thread.currentThread().interrupt();
        }
      }
      catch (final Exception e)
      {
        error("[{}]: Error handling event.", getId(), e);
        handled = !retryHandleEvent;

        if (!handled)
        {
          // Anything less than one effectively means infinite retries. If greater than one,
          // we eventually count down to 0 and stop retrying.
          if (retriesRemaining != 0)
          {
            if (retryHandleEventDelay > 0)
            {
              debug("[{}]: Retrying handle event in [{}] ms.", getId(), retryHandleEventDelay);
              try
              {
                Thread.sleep(retryHandleEventDelay);
              }
              catch (final InterruptedException ie)
              {
                Thread.currentThread().interrupt();
              }
            }
            else
            {
              debug("[{}]: Retrying handle event immediately.", getId());
            }
          }
          else
          {
            handled = true;
          }

          if (retriesRemaining > 0)
          {
            retriesRemaining--;
          }
        }
      }
    }
  }
}
