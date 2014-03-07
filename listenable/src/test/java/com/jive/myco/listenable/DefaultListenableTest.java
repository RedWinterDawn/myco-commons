package com.jive.myco.listenable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.base.Function;

/**
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public class DefaultListenableTest
{
  private DefaultListenableContainer<Listener> listenable;

  @Before
  public void setup() throws Exception
  {
    listenable = new DefaultListenableContainer<>();
  }

  private Executor getMockExecutor()
  {
    Executor mock = mock(Executor.class);
    doAnswer(new Answer()
    {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable
      {
        Runnable runner = (Runnable) invocation.getArguments()[0];
        runner.run();
        return null;
      }
    }).when(mock).execute(any(Runnable.class));

    return mock;
  }

  @Test
  public void testRunOnCorrectExecutor() throws Exception
  {
    Listener listener1 = new Listener();
    Executor executor1 = getMockExecutor();
    Listener listener2 = new Listener();
    Executor executor2 = getMockExecutor();
    listenable.addListener(listener1, executor1);
    listenable.addListener(listener2, executor2);

    listenable.forEach(new ListenerInvoker());

    assertEquals(1, listener1.timesRun);
    assertEquals(1, listener2.timesRun);
    verify(executor1, times(1)).execute(any(Runnable.class));
    verify(executor2, times(1)).execute(any(Runnable.class));
  }

  @Test
  public void testOnlyAddedOnce() throws Exception
  {
    Listener listener1 = new Listener();
    Executor executor1 = getMockExecutor();
    listenable.addListener(listener1, executor1);
    listenable.addListener(listener1, executor1);
    listenable.addListener(listener1, executor1);

    listenable.forEach(new ListenerInvoker());

    assertEquals(1, listener1.timesRun);
    verify(executor1, times(1)).execute(any(Runnable.class));
  }

  @Test
  public void testRemoveListener() throws Exception
  {
    Listener listener1 = new Listener();
    Executor executor1 = getMockExecutor();
    listenable.addListener(listener1, executor1);
    listenable.removeListener(listener1);

    listenable.forEach(new ListenerInvoker());

    assertEquals(0, listener1.timesRun);
    verify(executor1, never()).execute(any(Runnable.class));
  }

  @Test
  public void testSameThreadExecutorDefault() throws Exception
  {
    // if no executor provided should use same thread (all should be done after forEach)
    List<Listener> listeners = new ArrayList<>();
    for (int i = 0; i < 1000; i++)
    {
      Listener listener = new Listener();
      listeners.add(listener);
      listenable.addListener(listener);
    }

    listenable.forEach(new ListenerInvoker());

    for (Listener listener : listeners)
    {
      assertEquals(1, listener.timesRun);
    }
  }

  private static class Listener
  {
    private int timesRun;

    public void invoke()
    {
      timesRun++;
    }
  }

  private static class ListenerInvoker implements Function<Listener, Void>
  {
    @Override
    public Void apply(Listener input)
    {
      input.invoke();
      return null;
    }
  }
}
