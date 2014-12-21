package com.jive.myco.commons.hawtdispatch;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.fusesource.hawtdispatch.DispatchPriority;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Dispatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultDispatchQueueBuilderTest
{
  @Mock
  private Dispatcher dispatcher;

  @Mock
  private DispatchQueue queue;

  @Mock
  private DispatchQueue globalQueue;

  private DispatchQueueBuilder builder;

  @Before
  public void setup()
  {
    builder = new DefaultDispatchQueueBuilder("top", dispatcher);
    when(dispatcher.createQueue(anyString())).thenReturn(queue);
    when(dispatcher.getGlobalQueue(any(DispatchPriority.class))).thenReturn(globalQueue);
  }

  @Test
  public void testSegment()
  {
    final DispatchQueueBuilder newBuilder = builder.segment("second").segment("third");
    assertEquals("top:second:third", newBuilder.getName());
    newBuilder.build();
    verify(dispatcher).createQueue("top:second:third");
  }

  @Test
  public void testBuild()
  {
    builder.build();
    verify(dispatcher).createQueue("top");
    verify(queue).setTargetQueue(eq(globalQueue));
  }

  @Test
  public void testBuildDispatchPriority()
  {
    builder.segment("foo", "bar").build(DispatchPriority.HIGH);
    verify(dispatcher).createQueue("top:foo:bar");
    verify(queue).setTargetQueue(eq(globalQueue));
  }

  @Test
  public void testGetName()
  {
    assertEquals("top", builder.getName());
    assertEquals("top:bottom", builder.segment("bottom").getName());
  }

  @Test
  public void testGetDispatcher() throws Exception
  {
    assertEquals(dispatcher, builder.getDispatcher());
    assertEquals(dispatcher, builder.segment("one", "two").segment("three").getDispatcher());
  }

}
