package com.jive.myco.commons.hawtdispatch;

import java.util.concurrent.TimeUnit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import org.fusesource.hawtdispatch.DispatchPriority;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Dispatcher;
import org.fusesource.hawtdispatch.Metrics;
import org.fusesource.hawtdispatch.Task;
import org.fusesource.hawtdispatch.internal.DispatcherConfig;

import com.google.common.base.Joiner;

/**
 * Default implementation of a {@link DispatchQueueBuilder}
 *
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
@AllArgsConstructor
public class DefaultDispatchQueueBuilder implements DispatchQueueBuilder
{
  private static DispatchQueueBuilder DEFAULT_BUILDER;

  private static final String SEPARATOR = ":";
  private static final Joiner JOINER = Joiner.on(SEPARATOR);

  @Getter
  @NonNull
  private final String name;

  @Getter
  @NonNull
  private final Dispatcher dispatcher;

  @Override
  public DispatchQueueBuilder segment(
      @NonNull final String segment,
      final String... additionalSegments)
  {
    return segment(joinSegments(name, segment, additionalSegments), dispatcher);
  }

  @Override
  public DispatchQueue build()
  {
    final DispatchQueue delegateQueue = build(DispatchPriority.DEFAULT);

    return new DispatchQueue()
    {
      @Override
      public void setTargetQueue(final DispatchQueue queue)
      {
        delegateQueue.setTargetQueue(delegateQueue);
      }

      @Override
      public DispatchQueue getTargetQueue()
      {
        return delegateQueue.getTargetQueue();
      }

      @Override
      public void suspend()
      {
        delegateQueue.suspend();
      }

      @Override
      public void resume()
      {
        delegateQueue.resume();
      }

      @Override
      public boolean isSuspended()
      {
        return delegateQueue.isSuspended();
      }

      @Override
      public QueueType getQueueType()
      {
        return delegateQueue.getQueueType();
      }

      @Override
      public DispatchQueue createQueue(final String label)
      {
        return delegateQueue.createQueue(label);
      }

      @Override
      public void execute(final Runnable runnable)
      {
        delegateQueue.execute(wrap(delegateQueue, runnable));
      }

      @Override
      public void execute(final Task task)
      {
        delegateQueue.execute(wrap(delegateQueue, task));
      }

      @Override
      public void executeAfter(final long delay, final TimeUnit unit, final Runnable runnable)
      {
        delegateQueue.executeAfter(delay, unit, wrap(delegateQueue, runnable));
      }

      @Override
      public void executeAfter(final long delay, final TimeUnit unit, final Task task)
      {
        delegateQueue.executeAfter(delay, unit, wrap(delegateQueue, task));
      }

      @Override
      public String getLabel()
      {
        return delegateQueue.getLabel();
      }

      @Override
      public void setLabel(final String label)
      {
        delegateQueue.setLabel(label);
      }

      @Override
      public boolean isExecuting()
      {
        return delegateQueue.isExecuting();
      }

      @Override
      public void assertExecuting()
      {
        delegateQueue.assertExecuting();
      }

      @Override
      public void profile(final boolean on)
      {
        delegateQueue.profile(on);
      }

      @Override
      public boolean profile()
      {
        return delegateQueue.profile();
      }

      @Override
      public Metrics metrics()
      {
        return delegateQueue.metrics();
      }
    };
  }

  @Override
  public DispatchQueue build(final DispatchPriority priority)
  {
    final DispatchQueue queue = dispatcher.createQueue(name);
    queue.setTargetQueue(dispatcher.getGlobalQueue(priority));
    return queue;
  }

  protected void beforeExecute()
  {
    // No-Op
  }

  protected void afterExecute()
  {
    // No-Op
  };

  protected Runnable wrap(
      final DispatchQueue dispatchQueue,
      final Runnable runnable)
  {
    return () ->
    {
      DefaultDispatchQueueBuilder.this.beforeExecute();
      try
      {
        runnable.run();
      }
      finally
      {
        DefaultDispatchQueueBuilder.this.afterExecute();
      }
    };
  }

  protected DispatchQueueBuilder segment(final String name, final Dispatcher dispatcher)
  {
    return new DefaultDispatchQueueBuilder(name, dispatcher);
  }

  private static String joinSegments(@NonNull final String first, @NonNull final String second,
      final String... rest)
  {
    return JOINER.join(first, second, (Object[]) rest);
  }

  public static synchronized DispatchQueueBuilder getDefaultBuilder()
  {
    if (DEFAULT_BUILDER == null)
    {
      DEFAULT_BUILDER =
          new DefaultDispatchQueueBuilder("", DispatcherConfig.getDefaultDispatcher());
    }
    return DEFAULT_BUILDER;
  }
}
