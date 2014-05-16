package com.jive.myco.commons.hawtdispatch;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.fusesource.hawtdispatch.DispatchPriority;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Dispatcher;
import org.fusesource.hawtdispatch.internal.HawtDispatchQueue;
import org.fusesource.hawtdispatch.internal.HawtDispatcher;
import org.mockito.ArgumentCaptor;

import com.google.common.base.Joiner;

/**
 * @author Brandon Pedersen <bpedersen@getjive.com>
 */
public class SameThreadTestQueueBuilder extends DefaultDispatchQueueBuilder
{

  public SameThreadTestQueueBuilder()
  {
    this("test");
  }

  public SameThreadTestQueueBuilder(String id)
  {
    super(id, mock(Dispatcher.class));

    when(getDispatcher().getCurrentQueue()).thenAnswer(
        invocation -> HawtDispatcher.CURRENT_QUEUE.get());

    when(getDispatcher().getGlobalQueue())
        .thenAnswer(invocation -> build(DispatchPriority.DEFAULT));

    final ArgumentCaptor<DispatchPriority> captor = ArgumentCaptor.forClass(DispatchPriority.class);
    when(getDispatcher().getGlobalQueue(captor.capture())).thenAnswer(
        invocation -> build(captor.getValue()));
  }

  private SameThreadTestQueueBuilder(String id, Dispatcher dispatcher)
  {
    super(id, dispatcher);
  }

  @Override
  public DispatchQueueBuilder segment(String segment, String... additionalSegments)
  {
    return new SameThreadTestQueueBuilder(Joiner.on(":").join(getName(), segment,
        (Object[]) additionalSegments), getDispatcher());
  }

  @Override
  public DispatchQueue build(DispatchPriority priority)
  {
    return getTestQueue(getName());
  }

  public static DispatchQueue getTestQueue(String name)
  {
    final HawtDispatchQueue testQueue = mock(HawtDispatchQueue.class);
    final AtomicInteger suspended = new AtomicInteger();
    doAnswer(invocation ->
    {
      boolean set = false;
      if (HawtDispatcher.CURRENT_QUEUE.get() != testQueue)
      {
        set = true;
        HawtDispatcher.CURRENT_QUEUE.set(testQueue);
      }

      when(testQueue.isExecuting()).thenReturn(true);

      Runnable runnable = (Runnable) invocation.getArguments()[0];
      runnable.run();

      when(testQueue.isExecuting()).thenReturn(false);

      if (set)
      {
        HawtDispatcher.CURRENT_QUEUE.set(null);
      }
      return null;
    }).when(testQueue).execute(any(Runnable.class));
    when(testQueue.getLabel()).thenReturn(name);
    doAnswer(invocation -> suspended.incrementAndGet()).when(testQueue).suspend();
    doAnswer(invocation -> suspended.decrementAndGet()).when(testQueue).resume();
    when(testQueue.isSuspended()).thenReturn(suspended.get() != 0);
    return testQueue;
  }

}
