package com.jive.myco.commons.metrics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.SocketFactory;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * A custom Graphite client instance for Metrics.
 * <p>
 * This client instance retains an open socket between reporting intervals, does not flush on every
 * metric write, and uses the Pickle format for writing metric values.
 *
 * @author David Valeri
 */
@Slf4j
public class Graphite extends com.codahale.metrics.graphite.Graphite
{
  private static final long DEFAULT_RECONNECT_DELAY = 5000;
  private static final AtomicInteger GRAPHITE_COUNTER = new AtomicInteger();
  private static final int DEFAULT_QUEUE_SIZE = 1000;
  private static final int DEFAULT_BATCH_SIZE = 50;
  private static final long DEFAULT_BATCH_TIMEOUT_TIME = 500;

  // Pickle Op Codes, cause Pyrolite is the suck and we have to Pickle it ourselves.
  private static final byte MARK = '(';
  private static final byte STOP = '.';
  private static final byte LONG = 'L';
  private static final byte STRING = 'S';
  private static final byte APPEND = 'a';
  private static final byte LIST = 'l';
  private static final byte TUPLE = 't';

  private final String id;
  private final InetSocketAddress address;
  private final SocketFactory socketFactory;
  private final int queueSize;
  private final long reconnectionDelay;
  private final int batchSize;
  private final long batchTimeoutTime;

  private final Appender appender;
  private final List<ReportEvent> aggregateEvents;

  /**
   * An executor service used to run the connection monitor.
   */
  private ScheduledExecutorService executorService;

  private Socket socket;
  private volatile int failures;
  private volatile boolean run;
  private volatile ScheduledFuture<?> batchTimeoutTaskFuture;

  @Builder
  public Graphite(@NonNull final String id, final InetSocketAddress address,
      final SocketFactory socketFactory, final Integer queueSize, final Long reconnectionDelay,
      final Integer batchSize, final Long batchTimeoutTime)
  {
    // Stupid freaking Metrics uses a class and not an interface to define the Graphite client.
    super(address, socketFactory);
    this.id = id;
    this.address = address;
    this.socketFactory = socketFactory;

    this.queueSize = Optional.ofNullable(queueSize).orElse(DEFAULT_QUEUE_SIZE);
    this.reconnectionDelay = Optional.ofNullable(reconnectionDelay).orElse(DEFAULT_RECONNECT_DELAY);
    this.batchSize = Optional.ofNullable(batchSize).orElse(DEFAULT_BATCH_SIZE);
    this.batchTimeoutTime =
        Optional.ofNullable(batchTimeoutTime).orElse(DEFAULT_BATCH_TIMEOUT_TIME);

    if (this.batchTimeoutTime <= 0)
    {
      throw new IllegalArgumentException();
    }
    else if (this.batchSize <= 0)
    {
      throw new IllegalArgumentException();
    }
    else if (this.reconnectionDelay < 0)
    {
      throw new IllegalArgumentException();
    }

    this.appender = Appender.builder()
        .id("graphite-" + id)
        .queueSize(this.queueSize)
        .discarding(false)
        .retryHandleEvent(false)
        .graphite(this)
        .build();

    this.aggregateEvents = new ArrayList<>(this.batchSize);
  }

  /**
   * Initializes the internal book keeping resources for this instance. Must be called prior to
   * calling {@link #send(String, String, long)}.
   */
  public synchronized void init()
  {
    if (!run)
    {
      appender.init();

      executorService = new ScheduledThreadPoolExecutor(2,
          // NOT using Guava builder here because we would like to keep as many dependencies out
          // of this class as possible.
          new ThreadFactory()
          {
            private final AtomicInteger counter = new AtomicInteger();

            private final ThreadGroup group = (System.getSecurityManager() != null)
                ? System.getSecurityManager().getThreadGroup() : Thread.currentThread()
                    .getThreadGroup();
            private final String nameTemplate = "graphite-" + id + "-worker-%d"
                + GRAPHITE_COUNTER.getAndIncrement();

            @Override
            public Thread newThread(final Runnable r)
            {
              final Thread t = new Thread(group, r,
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
          });

      run = true;
    }
  }

  /**
   * Destroys the internal book keeping resources for this instance.
   */
  public synchronized void destroy() throws InterruptedException
  {
    appender.destroy();

    run = false;
    if (executorService != null)
    {
      executorService.shutdownNow();
    }

    closeSocket(socket);
  }

  @Override
  public void connect() throws IllegalStateException, IOException
  {
    // No-op
  }

  @Override
  public void close() throws IOException
  {
    // No-op
  }

  @Override
  public void send(final String name, final String value, final long timestamp) throws IOException
  {
    try
    {
      appender.append(
          ReportEvent.builder()
              .name(sanitize(name))
              .value(sanitize(value))
              .timestamp(timestamp)
              .build());
    }
    catch (final InterruptedException e)
    {
      throw new IOException(e);
    }
  }

  /**
   * Returns the number of failed writes to the server.
   *
   * @return the number of failed writes to the server
   */
  @Override
  public int getFailures()
  {
    return failures;
  }

  /**
   * Called by the internal appender or the batch timeout task. Synchronized to ensure orderly
   * access between the two.
   *
   * @param batchTimeOut
   *          if the method is being invoked due to a timeout rather than a new incoming event
   * @param event
   *          the event being passed in, {@code null} when timed out
   */
  private void handleEventOrBatchTimeout(final boolean batchTimeOut,
      final ReportEvent event)
  {
    synchronized (aggregateEvents)
    {
      // Not a batch timeout so add the event to the collection.
      if (!batchTimeOut)
      {
        aggregateEvents.add(event);

        log.debug("[{}]: Added event [{}] to batch.",
            id,
            event);

        // If null, this is the first event in a new batch so schedule the timeout task.
        if (batchTimeoutTaskFuture == null)
        {
          batchTimeoutTaskFuture =
              executorService.schedule(
                  () ->
                  {
                    try
                    {
                      handleEventOrBatchTimeout(true, null);
                    }
                    catch (final Exception e)
                    {
                      // Ignore it
                }
              },
                  batchTimeoutTime, TimeUnit.MILLISECONDS);

          log.debug("[{}]: Scheduled batch timeout for new batch.", id);
        }
      }
      // It is a batch timeout, so clear out the pointer to the scheduled task that is currently
      // executing this method
      else
      {
        batchTimeoutTaskFuture = null;
      }

      // The batch is ready or timed out, send it
      if (aggregateEvents.size() == batchSize || batchTimeOut)
      {
        // If not null, the batch completed before timing out so cancel the task that was scheduled.
        if (batchTimeoutTaskFuture != null)
        {
          log.debug("[{}]: Batch size reached, cancelling batch timeout and writing batch.", id);
          batchTimeoutTaskFuture.cancel(true);
          batchTimeoutTaskFuture = null;
        }
        else
        {
          log.debug("[{}]: Batch timeout reached, writing batch.", id);
        }

        writeBatch();
        aggregateEvents.clear();

        log.debug("[{}]: Wrote batch.", id);
      }
    }
  }

  private final void writeBatch()
  {
    synchronized (aggregateEvents)
    {
      // Just a little insurance.
      if (!aggregateEvents.isEmpty())
      {
        boolean written = false;

        while (!Thread.interrupted() && !written && run)
        {
          final Socket currentSocket = getOrInitSocket();

          if (currentSocket != null)
          {
            try (final ByteArrayOutputStream baos = new ByteArrayOutputStream())
            {
              baos.write(MARK);

              // Start the list
              // @formatter:off
              /*
               I(name='LIST',
                   code='l',
                   arg=None,
                   stack_before=[markobject, stackslice],
                   stack_after=[pylist],
                   proto=0,
                   doc="""Build a list out of the topmost stack slice, after markobject.

                   All the stack entries following the topmost markobject are placed into
                   a single Python list, which single list object replaces all of the
                   stack from the topmost markobject onward.  For example,

                   Stack before: ... markobject 1 2 3 'abc'
                   Stack after:  ... [1, 2, 3, 'abc']
                  """)
              */
              // @formatter:on
              baos.write(LIST);

              for (final ReportEvent event : aggregateEvents)
              {
                // Start metric tuple
                baos.write(MARK);

                // Metric Name
                // @formatter:off
                /*
                  From: http://svn.python.org/projects/python/trunk/Lib/pickletools.py
                  I(name='STRING',
                      code='S',
                      arg=stringnl,
                      stack_before=[],
                      stack_after=[pystring],
                      proto=0,
                      doc="""Push a Python string object.

                      The argument is a repr-style string, with bracketing quote characters,
                      and perhaps embedded escapes.  The argument extends until the next
                      newline character.
                      """),
                 */
                // @formatter:on
                baos.write(STRING);
                baos.write('\'');
                // NOTE: UTF-8 and certain ISOs overlap with ASCII. It is unclear what to do for
                // encoding if a non-ASCII character is encountered.
                baos.write(event.getName().getBytes("UTF-8"));
                baos.write('\'');
                baos.write('\n');

                // Start metric value and timestamp tuple
                baos.write(MARK);

                // Metric Timetstamp
                // @formatter:off
                /*
                  From: http://svn.python.org/projects/python/trunk/Lib/pickletools.py
                  I(name='LONG',
                      code='L',
                      arg=decimalnl_long,
                      stack_before=[],
                      stack_after=[pylong],
                      proto=0,
                      doc="""Push a long integer.

                      The same as INT, except that the literal ends with 'L', and always
                      unpickles to a Python long.  There doesn't seem a real purpose to the
                      trailing 'L'.

                      Note that LONG takes time quadratic in the number of digits when
                      unpickling (this is simply due to the nature of decimal->binary
                      conversion).  Proto 2 added linear-time (in C; still quadratic-time
                      in Python) LONG1 and LONG4 opcodes.
                      """),
                 */
                // @formatter:on
                baos.write(LONG);
                baos.write(String.valueOf(event.getTimestamp()).getBytes("UTF-8"));
                baos.write('L');
                baos.write('\n');


                // Metric Value
                // @formatter:off
                /*
                  From: http://svn.python.org/projects/python/trunk/Lib/pickletools.py
                  I(name='STRING',
                      code='S',
                      arg=stringnl,
                      stack_before=[],
                      stack_after=[pystring],
                      proto=0,
                      doc="""Push a Python string object.

                      The argument is a repr-style string, with bracketing quote characters,
                      and perhaps embedded escapes.  The argument extends until the next
                      newline character.
                      """),
                 */
                // @formatter:on
                baos.write(STRING);
                baos.write('\'');
                baos.write(event.getValue().getBytes("UTF-8"));
                baos.write('\'');
                baos.write('\n');

                // Close metric value and timestamp tuple
                baos.write(TUPLE);

                // Close metric tuple
                baos.write(TUPLE); // outer close

                // @formatter:off
                /*
                 From: http://svn.python.org/projects/python/trunk/Lib/pickletools.py
                 I(name='APPEND',
                     code='a',
                     arg=None,
                     stack_before=[pylist, anyobject],
                     stack_after=[pylist],
                     proto=0,
                     doc="""Append an object to a list.

                     Stack before:  ... pylist anyobject
                     Stack after:   ... pylist+[anyobject]

                     although pylist is really extended in-place.
                    """),
                 */
                // @formatter:on
                baos.write(APPEND);
              }

              baos.write(STOP);

              final OutputStream out = currentSocket.getOutputStream();
              // Write the length header
              out.write(ByteBuffer.allocate(4).putInt(baos.size()).array());
              // Write the payload bytes
              out.write(baos.toByteArray());
              // Flush the message to the server
              out.flush();
              written = true;
              failures = 0;
            }
            catch (final IOException e)
            {
              failures++;
              log.error("[{}]: Error encoding log event to socket.", id, e);
              closeSocket(currentSocket);
            }
          }
        }
      }
    }
  }

  /**
   * Creates or retrieves the previously created socket. Attempts to establish the socket are made
   * repeatedly until this instance establishes a connection or the instance is stopped.
   *
   * @return the socket to use or {@code null} if the appender was stopped without a socket
   *         available
   */
  private synchronized Socket getOrInitSocket()
  {
    if (socket != null && !socket.isClosed())
    {
      return socket;
    }
    else
    {
      while (socket == null && run)
      {
        try
        {
          socket = socketFactory.createSocket(address.getAddress(), address.getPort());
          // Setting this such that the watcher's read operation never times out until the socket
          // is actually dead / closed.
          socket.setSoTimeout(0);
          socket.setKeepAlive(true);

          // Fire the watcher task off to run in the background
          final Socket watchedSocket = socket;
          executorService.submit(new Runnable()
          {
            @Override
            public void run()
            {
              try
              {
                while (watchedSocket.getInputStream().read() != -1)
                {
                  // No-Op
                }

                if (run)
                {
                  log.info("[{}]: Watcher detected socket close.", id);
                  closeSocket(watchedSocket);
                }
              }
              catch (final IOException e)
              {
                if (run)
                {
                  log.info("[{}]: Watcher detected socket close.", id, e);
                  closeSocket(watchedSocket);
                }
              }
            }
          });

          if (!run)
          {
            closeSocket(socket);
          }
        }
        catch (final Exception e)
        {
          if (run)
          {
            log.info(
                "[{}]: Connection failure to: [{}:{}].", id, address.getAddress(),
                address.getPort(), e);
            try
            {
              Thread.sleep(reconnectionDelay);
            }
            catch (final InterruptedException e2)
            {
              log.info("[{}]: Interrupted while connecting.", id, e2);
              Thread.interrupted();
            }
          }
        }
      }

      return socket;
    }
  }

  /**
   * Closes the socket {@code socketToClose}, if it is the socket currently in use.
   *
   * @param socketToClose
   *          the socket to close
   */
  private synchronized void closeSocket(final Socket socketToClose)
  {
    if (socket != null && socketToClose == socket)
    {
      try
      {
        socket.close();
      }
      catch (final IOException e)
      {
        log.info("Error closing socket.", e);
      }

      socket = null;
    }
  }

  /**
   * A struct containing the value of a single field in a metric.
   *
   * @author David Valeri
   */
  @Builder
  @Getter
  private static final class ReportEvent
  {
    private final String name;
    private final String value;
    private final long timestamp;

    @Override
    public String toString()
    {
      final StringBuilder builder = new StringBuilder();
      builder.append("ReportEvent [name=");
      builder.append(name);
      builder.append(", value=");
      builder.append(value);
      builder.append(", timestamp=");
      builder.append(timestamp);
      builder.append("]");
      return builder.toString();
    }
  }

  /**
   * An internal async appender separating the arrival of metrics from the sending of metrics to the
   * Carbon endpoint.
   *
   * @author David Valeri
   */
  private static final class Appender extends AsyncAppender<ReportEvent>
  {
    private final Graphite graphite;

    @Builder()
    public Appender(final String id, final int queueSize, final boolean discarding,
        final Integer discardThreshold,
        final boolean blockOnFull, final boolean retryHandleEvent,
        final Long retryHandleEventDelay,
        final Integer retryHandleEventCount, final Long gracefulShutdownTimeout,
        final Long forcedShutdownTimeout,
        @NonNull final Graphite graphite)
    {
      super(id, queueSize, discarding, discardThreshold, blockOnFull, retryHandleEvent,
          retryHandleEventDelay, retryHandleEventCount, gracefulShutdownTimeout,
          forcedShutdownTimeout);
      this.graphite = graphite;
    }

    @Override
    protected void handleEvent(final ReportEvent event)
    {
      graphite.handleEventOrBatchTimeout(false, event);
    }

    @Override
    protected void handleDiscardedEvent(final ReportEvent event)
    {
      log.warn("[{}]: Discarding event [{}].", graphite.id, event);
    }

    @Override
    protected void handleRejectedEvent(final ReportEvent event)
    {
      log.warn("[{}]: Rejected event [{}].", graphite.id, event);
    }

    @Override
    protected void trace(final String message, final Object... args)
    {
      log.trace(message, args);
    }

    @Override
    protected void debug(final String message, final Object... args)
    {
      log.debug(message, args);
    }

    @Override
    protected void info(final String message, final Object... args)
    {
      log.info(message, args);
    }

    @Override
    protected void warn(final String message, final Object... args)
    {
      log.warn(message, args);
    }

    @Override
    protected void error(final String message, final Object... args)
    {
      log.error(message, args);
    }
  }
}
