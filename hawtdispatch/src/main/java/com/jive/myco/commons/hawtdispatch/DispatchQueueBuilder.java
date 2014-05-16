package com.jive.myco.commons.hawtdispatch;

import org.fusesource.hawtdispatch.DispatchPriority;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Dispatcher;

/**
 * A utility class to create hierarchical dispatch queues without having to know who your daddy is.
 *
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public interface DispatchQueueBuilder
{
  /**
   * Get the current name that will be used to create queues
   *
   * @return the current queue name
   */
  String getName();

  /**
   * Get the dispatcher used for this builder
   */
  Dispatcher getDispatcher();

  /**
   * Create a new {@code DispatchQueueBuilder} based off of this one with a new segment added to the
   * name for the dispatch queue.
   *
   * @param segment
   *          the segment to add to the name for the queue
   * @param additionalSegments
   *          more segments to add one by one if needed
   * @return a new {@code DispatchQueueBuilder} with the {@code segment} appended to the name
   */
  DispatchQueueBuilder segment(String segment, String... additionalSegments);

  /**
   * Create a new {@link DispatchQueue} with the current builder's name at the default priority
   * level.
   *
   * @return a new {@code DispatchQueue}
   */
  DispatchQueue build();

  /**
   * Create a new {@link DispatchQueue} with the current builder's name at the given priority level
   *
   * @param priority
   *          the priority to use for the queue
   * @return a new {@code DispatchQueue}
   */
  DispatchQueue build(DispatchPriority priority);

}
