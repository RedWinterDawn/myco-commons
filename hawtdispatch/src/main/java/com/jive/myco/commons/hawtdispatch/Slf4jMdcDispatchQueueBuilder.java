package com.jive.myco.commons.hawtdispatch;

import java.util.Map;

import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Dispatcher;
import org.fusesource.hawtdispatch.internal.DispatcherConfig;
import org.slf4j.MDC;

/**
 * An implementation that preserves the MDC between thread transitions.
 *
 * @author David Valeri
 */
public class Slf4jMdcDispatchQueueBuilder extends DefaultDispatchQueueBuilder
{
  private static Slf4jMdcDispatchQueueBuilder DEFAULT_BUILDER;

  public Slf4jMdcDispatchQueueBuilder(
      final String name,
      final Dispatcher dispatcher)
  {
    super(name, dispatcher);
  }

  @Override
  protected Runnable wrap(
      final DispatchQueue dispatchQueue,
      final Runnable runnable)
  {
    // The wrapped runnable from the parent class.
    final Runnable superRunnable = super.wrap(dispatchQueue, runnable);

    // Context from the caller as we are still in the calling thread.
    final Map<String, String> contextFromCaller = MDC.getCopyOfContextMap();

    return () ->
    {
      // Any context that was already on this thread. We don't want to inherit it, but we
      // also don't want to lose it if it was left for some reason.
      final Map<String, String> previousContext = MDC.getCopyOfContextMap();

      // Install the calling context
      MDC.clear();
      if (contextFromCaller != null)
      {
        MDC.setContextMap(contextFromCaller);
      }

      try
      {
        superRunnable.run();
      }
      finally
      {
        // Restore the previous context
        MDC.clear();
        if (previousContext != null)
        {
          MDC.setContextMap(previousContext);
        }
      }
    };
  }

  @Override
  protected DispatchQueueBuilder segment(final String name, final Dispatcher dispatcher)
  {
    return new Slf4jMdcDispatchQueueBuilder(name, dispatcher);
  }

  public static synchronized DispatchQueueBuilder getDefaultBuilder()
  {
    if (DEFAULT_BUILDER == null)
    {
      DEFAULT_BUILDER =
          new Slf4jMdcDispatchQueueBuilder("", DispatcherConfig.getDefaultDispatcher());
    }
    return DEFAULT_BUILDER;
  }
}
