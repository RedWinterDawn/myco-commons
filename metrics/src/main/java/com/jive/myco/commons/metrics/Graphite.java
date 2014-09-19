package com.jive.myco.commons.metrics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
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

import org.slf4j.Logger;

import com.jive.myco.commons.retry.RetryManager;
import com.jive.myco.commons.retry.RetryPolicy;
import com.jive.myco.commons.retry.RetryStrategy;

/**
 * A custom Graphite client instance for Metrics.
 * <p>
 * This client instance retains an open socket between reporting intervals, does not flush on every
 * metric write, and optionally uses the Pickle format for writing metric values.
 *
 * @author David Valeri
 */
@Slf4j
public class Graphite extends com.codahale.metrics.graphite.Graphite
{
  private static final String DEFAULT_RECONNECT_DELAY_SYS_PROP_KEY = "metrics.reconnect.delay";
  // This is a low level tool and this configuration would be a nightmare to expose where needed
  // So allow configuration of the default value via a system property if this is causing any issues
  private static final long DEFAULT_RECONNECT_DELAY =
      Long.getLong(DEFAULT_RECONNECT_DELAY_SYS_PROP_KEY, 5000);
  private static final int DEFAULT_QUEUE_SIZE = 2000;
  private static final int DEFAULT_BATCH_SIZE = 50;
  private static final long DEFAULT_BATCH_TIMEOUT_TIME = 500;
  private static final SocketFactory DEFAULT_SOCKET_FACTORY = SocketFactory.getDefault();
  private static final boolean DEFAULT_PICKLE = false;

  /**
   * Charset used when pickle encoding.
   */
  private static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

  /**
   * Charset used for text encoding.
   */
  private static final Charset UTF_8 = Charset.forName("UTF-8");

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
  private final boolean pickle;

  private final Object lifecycleLock = new Object();
  private final Appender appender;
  private final List<ReportEvent> aggregateEvents;

  /**
   * An executor service used to run the connection monitor.
   */
  private ScheduledExecutorService executorService;

  private Socket socket;
  private final Object socketMonitor = new Object();
  private volatile int failures;
  private volatile boolean run;
  private volatile ScheduledFuture<?> batchTimeoutTaskFuture;

  private final RetryManager connectionRetryManager;
  private final ConnectionRetryStrategy connectionRetryStrategy;

  @Builder
  public Graphite(@NonNull final String id, final InetSocketAddress address,
      final SocketFactory socketFactory, final Integer queueSize, final Long reconnectionDelay,
      final Integer batchSize, final Long batchTimeoutTime, final Boolean pickle)
  {
    // Stupid freaking Metrics uses a class and not an interface to define the Graphite client.
    super(address, socketFactory);
    this.id = id;
    this.address = address;
    this.socketFactory = Optional.ofNullable(socketFactory).orElse(DEFAULT_SOCKET_FACTORY);

    this.queueSize = Optional.ofNullable(queueSize).orElse(DEFAULT_QUEUE_SIZE);
    this.reconnectionDelay = Optional.ofNullable(reconnectionDelay).orElse(DEFAULT_RECONNECT_DELAY);
    this.batchSize = Optional.ofNullable(batchSize).orElse(DEFAULT_BATCH_SIZE);
    this.batchTimeoutTime =
        Optional.ofNullable(batchTimeoutTime).orElse(DEFAULT_BATCH_TIMEOUT_TIME);
    this.pickle = Optional.ofNullable(pickle).orElse(DEFAULT_PICKLE);

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

    this.connectionRetryStrategy = new ConnectionRetryStrategy();
    this.connectionRetryManager = RetryManager.builder()
        .retryPolicy(RetryPolicy.builder()
            .maximumRetries(-1)
            .maximumRetryDelay(this.reconnectionDelay)
            .initialRetryDelay(100)
            .backoffMultiplier(3)
            .build())
        .retryStrategy(this.connectionRetryStrategy)
        .build();
  }

  /**
   * Initializes the internal book keeping resources for this instance. Must be called prior to
   * calling {@link #send(String, String, long)}.
   */
  public void init()
  {
    synchronized (lifecycleLock)
    {
      if (!run)
      {
        appender.init();

        executorService = new ScheduledThreadPoolExecutor(3,
            // NOT using Guava builder here because we would like to keep as many dependencies out
            // of this class as possible.
            new ThreadFactory()
            {
              private final AtomicInteger counter = new AtomicInteger();

              private final ThreadGroup group = (System.getSecurityManager() != null)
                  ? System.getSecurityManager().getThreadGroup() : Thread.currentThread()
                      .getThreadGroup();
              private final String nameTemplate = "graphite-" + id + "-worker-%d";

              @Override
              public Thread newThread(final Runnable r)
              {
                final Thread t = new Thread(group, r,
                    String.format(nameTemplate, counter.getAndIncrement()));

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

        connectionRetryStrategy.scheduleRetry(0);
      }
    }
  }

  /**
   * Destroys the internal book keeping resources for this instance.
   */
  public void destroy() throws InterruptedException
  {
    synchronized (lifecycleLock)
    {
      appender.destroy();

      run = false;
      if (executorService != null)
      {
        executorService.shutdownNow();
      }

      closeSocket(socket);
    }
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

        log.trace("[{}]: Added event [{}] to batch.", id, event);

        // If null, this is the first event in a new batch so schedule the timeout task.
        if (batchTimeoutTaskFuture == null)
        {
          batchTimeoutTaskFuture =
              executorService
                  .schedule(() ->
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

          log.trace("[{}]: Scheduled batch timeout for new batch.", id);
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
          log.trace("[{}]: Batch size reached, cancelling batch timeout and writing batch.", id);
          batchTimeoutTaskFuture.cancel(true);
          batchTimeoutTaskFuture = null;
        }
        else
        {
          log.trace("[{}]: Batch timeout reached, writing batch.", id);
        }

        writeBatch();
        aggregateEvents.clear();

        log.trace("[{}]: Wrote batch.", id);
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
        int failureCount = 0;
        Throwable lastRootCause = null;
        boolean written = false;

        while (!Thread.interrupted() && !written && run)
        {
          final Socket currentSocket = getSocket();

          if (currentSocket != null)
          {
            try
            {
              if (pickle)
              {
                writePickledBatch(currentSocket, aggregateEvents);
              }
              else
              {
                writeTextBatch(currentSocket, aggregateEvents);
              }
              written = true;
              // Successfully validated socket connection, we can now assert that the connection
              // was successfully initialized
              connectionRetryManager.onSuccess();
              failures = 0;
            }
            catch (final IOException e)
            {
              failures++;

              final Throwable rootCause = getRootCause(e);
              failureCount++;

              // New exception, not the same as the last exception we saw, or this failure is a
              // multiple of ten since the last success
              if (lastRootCause == null
                  || !rootCause.getClass().equals(lastRootCause.getClass())
                  || failureCount % 10 == 0)
              {
                log.error("[{}]: Error encoding metrics to socket on attempt [{}].",
                    id, failureCount, e);
              }

              lastRootCause = rootCause;

              closeSocket(currentSocket);
            }
          }
        }
      }
    }
  }

  /**
   * Writes a batch of metric events in text format.
   *
   * @param currentSocket
   *          the socket to write to
   * @param events
   *          the events to write
   *
   * @throws IOException
   *           if there is an error
   */
  private void writeTextBatch(final Socket currentSocket, final List<ReportEvent> events)
      throws IOException
  {
    final OutputStream out = currentSocket.getOutputStream();

    for (final ReportEvent event : events)
    {
      out.write(
          String
              .format(
                  "%s %s %s\n",
                  sanitize(event.getName()),
                  sanitize(event.getValue()),
                  Long.toString(event.getTimestamp()))
              .getBytes(UTF_8));
    }

    out.flush();
  }

  /**
   * Writes a batch of metric events in Pickle format.
   *
   * @param currentSocket
   *          the socket to write to
   * @param events
   *          the events to write
   *
   * @throws IOException
   *           if there is an error
   */
  private void writePickledBatch(final Socket currentSocket, final List<ReportEvent> events)
      throws IOException
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

      for (final ReportEvent event : events)
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
        baos.write(event.getName().getBytes(ISO_8859_1));
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
        baos.write(String.valueOf(event.getTimestamp()).getBytes(ISO_8859_1));
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
        baos.write(event.getValue().getBytes(ISO_8859_1));
        baos.write('\'');
        baos.write('\n');

        // Close metric value and timestamp tuple
        baos.write(TUPLE);

        // Close metric tuple
        baos.write(TUPLE);

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
    }
  }

  /**
   * Waits for the socket to be initialized and returns once the socket is connected and ready
   * (unless we are shutting down).
   *
   * @return the socket to use or {@code null} if the appender was stopped without a socket
   *         available
   */
  private Socket getSocket()
  {
    while (run && (socket == null || socket.isClosed()))
    {
      try
      {
        synchronized (socketMonitor)
        {
          if (run && (socket == null || socket.isClosed()))
          {
            socketMonitor.wait();
          }
        }
      }
      catch (final InterruptedException e)
      {
        log.info("[{}]: Interrupted while connecting.", id, e);
        Thread.interrupted();
      }
    }

    return socket;
  }

  private synchronized void tryInitSocket()
  {
    if (socket != null && !socket.isClosed())
    {
      // already connected
      return;
    }

    try
    {
      socket = socketFactory.createSocket();
      socket.connect(address, 2);

      // Setting this such that the watcher's read operation never times out until the socket
      // is actually dead / closed.
      socket.setSoTimeout(0);
      socket.setKeepAlive(true);

      // Fire the watcher task off to run in the background
      final Socket watchedSocket = socket;
      executorService.submit(() ->
      {
        try
        {
          while (watchedSocket.getInputStream().read() != -1)
          {
            // No-Op
          }

          if (run)
          {
            final String message = String.format("[%s]: Watcher detected socket close", id);
            connectionRetryManager.onFailure(new ConnectionClosedException(message));
            closeSocket(watchedSocket);
          }
        }
        catch (final IOException e)
        {
          if (run)
          {
            final String message =
                String.format("[%s]: Watcher detected socket close on IOException", id);
            connectionRetryManager.onFailure(new ConnectionClosedException(message, e));
            closeSocket(watchedSocket);
          }
        }
      });

      if (!run)
      {
        closeSocket(watchedSocket);
      }

      synchronized (socketMonitor)
      {
        socketMonitor.notifyAll();
      }
    }
    catch (final Exception e)
    {
      if (run)
      {
        final String message = String.format("[%s]: Connection failure to [%s:%s]",
            id, address.getAddress(), address.getPort());
        connectionRetryManager.onFailure(new ConnectionClosedException(message, e));
      }
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

  private Throwable getRootCause(final Throwable t)
  {
    if (t.getCause() == null)
    {
      return t;
    }
    else
    {
      return getRootCause(t.getCause());
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

    @Builder
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
    protected Logger getLogger()
    {
      return log;
    }
  }

  private class ConnectionRetryStrategy implements RetryStrategy
  {
    private Throwable lastRootCause = null;

    @Override
    public void scheduleRetry(final long delay)
    {
      log.trace("Schedule connect retry [{}]", delay);
      executorService.schedule(Graphite.this::tryInitSocket, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onFailure(final int retryCount, final Throwable cause)
    {
      if (run)
      {
        final Throwable rootCause = getRootCause(cause);

        // New exception, not the same as the last exception we saw, or this failure is a
        // multiple of ten since the last success
        if (lastRootCause == null
            || !rootCause.getClass().equals(lastRootCause.getClass())
            || retryCount % 10 == 0)
        {
          log.error(
              "[{}]: Error occurred while connecting to graphite on attempt [{}].",
              id, retryCount, cause);
        }

        lastRootCause = rootCause;

        closeSocket(socket);
      }
    }

    @Override
    public void onRetriesExhausted(final List<Throwable> causes)
    {
      log.error("Retries exhausted on reconnect???? " +
          "There is no max retry so this should not happen");
    }
  }

  private static class ConnectionClosedException extends Exception
  {
    private static final long serialVersionUID = 3424648054343088489L;

    public ConnectionClosedException()
    {
      super();
    }

    public ConnectionClosedException(final String message)
    {
      super(message);
    }

    public ConnectionClosedException(final String message, final Throwable cause)
    {
      super(message, cause);
    }

    public ConnectionClosedException(final Throwable cause)
    {
      super(cause);
    }
  }

  private static class ConnectionErrorException extends Exception
  {
    private static final long serialVersionUID = -1991764546589398466L;

    public ConnectionErrorException()
    {
      super();
    }

    public ConnectionErrorException(final String message)
    {
      super(message);
    }

    public ConnectionErrorException(final String message, final Throwable cause)
    {
      super(message, cause);
    }

    public ConnectionErrorException(final Throwable cause)
    {
      super(cause);
    }
  }

}
