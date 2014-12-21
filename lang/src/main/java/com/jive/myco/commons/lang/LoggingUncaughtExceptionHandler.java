package com.jive.myco.commons.lang;

import lombok.extern.slf4j.Slf4j;

/**
 * An {@link Thread.UncaughtExceptionHandler} implementation that attempts to log to an Slf4j logger
 * rather than {@link System#err}. This implementation falls back to the default behavior in the
 * event that there is an error while logging via Slf4j.
 *
 * @author David Valeri
 */
@Slf4j
public class LoggingUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler
{
  @Override
  public void uncaughtException(final Thread thread, final Throwable t)
  {
    try
    {
      // Try to log it somewhere desirable.
      log.error("Exception in Thread [{}].", thread.getName(), t);
    }
    catch (final Throwable t2)
    {
      // Attempt to log the double exception. Sometimes exceptions blow up when trying
      // to render stack traces, I'm looking at you Guice, or the logging framework chokes.
      try
      {
        log.error("Error handling uncaught exception.", t2);
      }
      catch (final Throwable t3)
      {
        // IGNORE we could do this all day so we have to give up somewhere.
      }

      // Fall back to the original behavior as a last resort.
      System.err.print("Exception in thread \"" + thread.getName() + "\" ");
      t.printStackTrace(System.err);
    }
  }
}
