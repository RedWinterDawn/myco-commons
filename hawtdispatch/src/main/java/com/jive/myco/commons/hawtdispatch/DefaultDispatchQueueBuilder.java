package com.jive.myco.commons.hawtdispatch;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import org.fusesource.hawtdispatch.DispatchPriority;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Dispatcher;
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
  public DispatchQueueBuilder segment(@NonNull String segment, String... additionalSegments)
  {
    return new DefaultDispatchQueueBuilder(
        joinSegments(name, segment, additionalSegments), dispatcher);
  }

  @Override
  public DispatchQueue build()
  {
    return build(DispatchPriority.DEFAULT);
  }

  @Override
  public DispatchQueue build(DispatchPriority priority)
  {
    DispatchQueue queue = dispatcher.createQueue(name);
    queue.setTargetQueue(dispatcher.getGlobalQueue(priority));
    return queue;
  }

  static String joinSegments(@NonNull String first, @NonNull String second, String... rest)
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
