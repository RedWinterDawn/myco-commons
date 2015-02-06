package com.jive.myco.commons.lang;

import static com.jayway.awaitility.Awaitility.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

/**
 * Tests for {@link LoggingUncaughtExceptionHandler}.
 *
 * @author David Valeri
 */
public class LoggingUncaughtExceptionHandlerTest
{
  private static final String TEST_THREAD_NAME_PREFIX = "TEST THREAD";
  private static final Runnable EXCEPTION_THROWING_RUNNABLE = () ->
  {
    throw new RuntimeException("THIS IS A TEST EXCEPTION");
  };
  private Appender<ILoggingEvent> mockAppender;

  @SuppressWarnings("unchecked")
  @Before
  public void setup()
  {
    mockAppender = mock(Appender.class);
    when(mockAppender.getName()).thenReturn("MOCK");

    // Reconfigure logging to use a mock appender.
    final ch.qos.logback.classic.Logger root =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(
            ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
    root.addAppender(mockAppender);

    // Reset this so tests don't bleed into each other.
    Thread.setDefaultUncaughtExceptionHandler(null);
  }

  @Test
  public void testUncaughtExceptionHandlerOnThread() throws Exception
  {
    final Thread thread = createTestThread();
    thread.setUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler());
    thread.start();
    thread.join(1000L);

    verifyLogEvent();
  }

  @Test
  public void testUncaughtExceptionHandlerOnExecutorThreadFactory() throws Exception
  {
    final AtomicBoolean executed = new AtomicBoolean();

    final ExecutorService executorService = Executors.newSingleThreadExecutor(
        createThreadFactory(true));
    executorService.execute(EXCEPTION_THROWING_RUNNABLE);
    executorService
        .execute(() ->
        {
          executed.set(true);
        });

    executorService.shutdown();
    executorService.awaitTermination(1000L, TimeUnit.MILLISECONDS);

    // The task completes and the executor service completes shutdown before it is guaranteed
    // to have triggered the handler. Therefore, we need to wait a little for the event to
    // show up in the logs.
    await().until(() ->
    {
      verifyLogEvent();
      assertTrue(executed.get());
    });
  }

  @Test
  public void testDefaultUncaughtExceptionHandlerOnThread() throws Exception
  {
    Thread.setDefaultUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler());

    final Thread thread = createTestThread();
    thread.start();
    thread.join(1000L);

    verifyLogEvent();
  }

  @Test
  public void testDefaultUncaughtExceptionHandlerOnExecutor() throws Exception
  {
    Thread.setDefaultUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler());

    final AtomicBoolean executed = new AtomicBoolean();

    final ExecutorService executorService = Executors.newSingleThreadExecutor(
        createThreadFactory(false));
    executorService.execute(EXCEPTION_THROWING_RUNNABLE);
    executorService
        .execute(() ->
        {
          executed.set(true);
        });

    executorService.shutdown();
    executorService.awaitTermination(1000L, TimeUnit.MILLISECONDS);

    // The task completes and the executor service completes shutdown before it is guaranteed
    // to have triggered the handler. Therefore, we need to wait a little for the event to
    // show up in the logs.
    await().until(() ->
    {
      verifyLogEvent();
      assertTrue(executed.get());
    });
  }

  @Test
  public void testUncaughtExceptionHandlerFallbackBehavior() throws Exception
  {
    final Thread thread = new Thread(
        () ->
        {
          throw new RuntimeException()
          {
            private static final long serialVersionUID = 1L;

            @Override
            public String getMessage() {
              throw new RuntimeException("ERROR IN EXCEPTION");
            }
          };
        },
        TEST_THREAD_NAME_PREFIX);

    thread.setUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler());
    thread.start();
    thread.join(1000L);

    verifyLogEvent("Error handling uncaught exception");
  }

  private ThreadFactory createThreadFactory(final boolean setHandler)
  {
    final AtomicInteger counter = new AtomicInteger();
    return (r) ->
    {
      final Thread t = new Thread(r, TEST_THREAD_NAME_PREFIX + counter.incrementAndGet());
      t.setDaemon(true);
      if (setHandler)
      {
        t.setUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler());
      }
      return t;
    };
  }

  private void verifyLogEvent()
  {
    verifyLogEvent(TEST_THREAD_NAME_PREFIX);
  }

  private void verifyLogEvent(final String content)
  {
    verify(mockAppender).doAppend(
        argThat(
            new ArgumentMatcher<ILoggingEvent>()
            {
              @Override
              public boolean matches(final Object argument)
              {
                return ((ILoggingEvent) argument).getFormattedMessage().contains(content);
              }
            }));
  }

  private Thread createTestThread()
  {
    return new Thread(
        EXCEPTION_THROWING_RUNNABLE,
        TEST_THREAD_NAME_PREFIX);
  }
}
