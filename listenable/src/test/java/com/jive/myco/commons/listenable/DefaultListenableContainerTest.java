package com.jive.myco.commons.listenable;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public class DefaultListenableContainerTest
{
  private DefaultListenableContainer<Listener> listenable;

  @Before
  public void setup() throws Exception
  {
    listenable = new DefaultListenableContainer<>();
  }

  private Executor getMockExecutor()
  {
    final Executor mock = mock(Executor.class);
    doAnswer(new Answer<Void>()
    {
      @Override
      public Void answer(final InvocationOnMock invocation) throws Throwable
      {
        final Runnable runner = (Runnable) invocation.getArguments()[0];
        runner.run();
        return null;
      }
    }).when(mock).execute(any(Runnable.class));

    return mock;
  }

  @Test
  public void testRunOnCorrectExecutor() throws Exception
  {
    final Listener listener1 = new Listener();
    final Executor executor1 = getMockExecutor();
    final Listener listener2 = new Listener();
    final Executor executor2 = getMockExecutor();
    listenable.addListener(listener1, executor1);
    listenable.addListener(listener2, executor2);

    listenable.forEach(new ListenerInvoker());

    assertEquals(1, listener1.timesRun);
    assertEquals(1, listener2.timesRun);
    verify(executor1, times(1)).execute(any(Runnable.class));
    verify(executor2, times(1)).execute(any(Runnable.class));
  }

  @Test
  public void testOnlyAddedOnceButChangesExecutor() throws Exception
  {
    final Listener listener1 = new Listener();
    final Executor executor1 = getMockExecutor();

    listenable.addListener(listener1);
    listenable.addListener(listener1, executor1);
    listenable.addListener(listener1, executor1);
    listenable.addListener(listener1, executor1);

    listenable.forEach(new ListenerInvoker());

    assertEquals(1, listener1.timesRun);
    verify(executor1, times(1)).execute(any(Runnable.class));

    listenable.addListenerWithInitialAction(listener1, new ListenerInvoker());
    listenable.addListenerWithInitialAction(listener1, executor1, new ListenerInvoker());

    assertEquals(1, listener1.timesRun);
    verify(executor1, times(1)).execute(any(Runnable.class));

    listenable.forEach(new ListenerInvoker());

    assertEquals(2, listener1.timesRun);
    verify(executor1, times(2)).execute(any(Runnable.class));
  }

  @Test
  public void testRemoveListener() throws Exception
  {
    final Listener listener1 = new Listener();
    final Executor executor1 = getMockExecutor();
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
    final List<Listener> listeners = new ArrayList<>();
    for (int i = 0; i < 1000; i++)
    {
      final Listener listener = new Listener();
      listeners.add(listener);
      listenable.addListener(listener);
    }

    listenable.forEach(new ListenerInvoker());

    for (final Listener listener : listeners)
    {
      assertEquals(1, listener.timesRun);
    }
  }

  @Test
  public void testAddWithActionSameThreadExecutorDefault() throws Exception
  {
    final List<Listener> listeners = new ArrayList<>();
    for (int i = 0; i < 1000; i++)
    {
      final Listener listener = new Listener();
      listeners.add(listener);
      listenable.addListenerWithInitialAction(listener, new ListenerInvoker());
      assertEquals(1, listener.timesRun);
    }

    listenable.forEach(new ListenerInvoker());

    for (final Listener listener : listeners)
    {
      assertEquals(2, listener.timesRun);
    }
  }

  @Test
  public void testAddWithActionCorrectExecutor() throws Exception
  {
    final Listener listener1 = new Listener();
    final Executor executor1 = getMockExecutor();

    final Listener listener2 = new Listener();
    final Executor executor2 = getMockExecutor();

    listenable.addListenerWithInitialAction(listener1, executor1, new ListenerInvoker());
    listenable.addListenerWithInitialAction(listener2, executor2, new ListenerInvoker());

    assertEquals(1, listener1.timesRun);
    assertEquals(1, listener2.timesRun);

    verify(executor1, times(1)).execute(any(Runnable.class));
    verify(executor2, times(1)).execute(any(Runnable.class));

    listenable.forEach(new ListenerInvoker());

    assertEquals(2, listener1.timesRun);
    assertEquals(2, listener2.timesRun);

    verify(executor1, times(2)).execute(any(Runnable.class));
    verify(executor2, times(2)).execute(any(Runnable.class));
  }

  @Test
  public void testListenerThrowsException() throws Exception
  {
    final Listener listener1 = new Listener()
    {
      @Override
      public void invoke()
      {
        super.invoke();
        throw new RuntimeException();
      }
    };

    final Listener listener2 = new Listener()
    {
      @Override
      public void invoke()
      {
        super.invoke();
        throw new RuntimeException();
      }
    };

    listenable.addListener(listener1);
    listenable.addListenerWithInitialAction(listener2, new ListenerInvoker());

    assertEquals(0, listener1.timesRun);
    assertEquals(1, listener2.timesRun);

    listenable.forEach(new ListenerInvoker());

    assertEquals(1, listener1.timesRun);
    assertEquals(2, listener2.timesRun);
  }

  private static class Listener
  {
    private int timesRun;

    public void invoke()
    {
      timesRun++;
    }
  }

  private static class ListenerInvoker implements Consumer<Listener>
  {
    @Override
    public void accept(final Listener input)
    {
      input.invoke();
    }
  }
}
