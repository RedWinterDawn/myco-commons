package com.jive.myco.commons.lifecycle;

import static com.jayway.awaitility.Awaitility.*;
import static com.jive.myco.commons.concurrent.Pnky.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;

import org.fusesource.hawtdispatch.DispatchQueue;
import org.junit.Test;
import org.mockito.Matchers;

import com.google.common.collect.Lists;
import com.jive.myco.commons.concurrent.PnkyPromise;
import com.jive.myco.commons.hawtdispatch.DefaultDispatchQueueBuilder;

/**
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
@Slf4j
public class AbstractLifecycledTest
{
  private final DispatchQueue testQueue = spy(DefaultDispatchQueueBuilder.getDefaultBuilder()
      .build());

  @Test
  public void testStageSetAfterSuccess() throws Exception
  {
    final Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      @Override
      protected PnkyPromise<Void> initInternal()
      {
        return immediatelyComplete(null);
      }

      @Override
      protected PnkyPromise<Void> destroyInternal()
      {
        return immediatelyFailed(new IllegalStateException());
      }
    };

    testInstance.init().get(50, TimeUnit.MILLISECONDS);

    assertEquals(LifecycleStage.INITIALIZED, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
    verify(testQueue, atLeastOnce()).execute(Matchers.any(Runnable.class));
  }

  @Test
  public void testFailedInitializationViaCallback() throws Exception
  {
    final AtomicBoolean destroyInvoked = new AtomicBoolean();
    final Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      @Override
      protected PnkyPromise<Void> initInternal()
      {
        return immediatelyFailed(new IllegalArgumentException("foo"));
      }

      @Override
      protected PnkyPromise<Void> destroyInternal()
      {
        destroyInvoked.set(true);
        return immediatelyComplete(null);
      }
    };

    try
    {
      testInstance.init().get(50, TimeUnit.MILLISECONDS);
      fail();
    }
    catch (final ExecutionException e)
    {
      assertThat(e.getCause(), instanceOf(IllegalArgumentException.class));
    }

    assertEquals(LifecycleStage.DESTROYED, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
    assertTrue(destroyInvoked.get());
  }

  @Test
  public void testInitializationFailedViaRuntimeError() throws Exception
  {
    final AtomicBoolean destroyInvoked = new AtomicBoolean();
    final Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      @Override
      protected PnkyPromise<Void> initInternal()
      {
        throw new NumberFormatException();
      }

      @Override
      protected PnkyPromise<Void> destroyInternal()
      {
        destroyInvoked.set(true);
        return immediatelyComplete(null);
      }
    };

    try
    {
      testInstance.init().get(50, TimeUnit.MILLISECONDS);
      fail();
    }
    catch (final ExecutionException e)
    {
      assertThat(e.getCause(), instanceOf(NumberFormatException.class));
    }

    assertEquals(LifecycleStage.DESTROYED, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
    assertTrue(destroyInvoked.get());
  }

  @Test
  public void testCustomCleanupCalledAfterFailedInit() throws Exception
  {
    final AtomicBoolean destroyInvoked = new AtomicBoolean();
    final AtomicBoolean handlerInvoked = new AtomicBoolean();
    final Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {

      @Override
      protected PnkyPromise<Void> initInternal()
      {
        return immediatelyFailed(new NumberFormatException());
      }

      @Override
      protected PnkyPromise<Void> destroyInternal()
      {
        destroyInvoked.set(true);
        return immediatelyComplete(null);
      }

      @Override
      protected PnkyPromise<Void> handleInitFailure()
      {
        if (getLifecycleStage() == LifecycleStage.INITIALIZATION_FAILED)
        {
          handlerInvoked.set(true);
        }
        else
        {
          log.error("init failure invoked in wrong state: {}", getLifecycleStage());
        }
        return immediatelyComplete(null);
      }
    };

    try
    {
      testInstance.init().get(50, TimeUnit.MILLISECONDS);
      fail();
    }
    catch (final ExecutionException e)
    {
      assertThat(e.getCause(), instanceOf(NumberFormatException.class));
    }

    assertEquals(LifecycleStage.DESTROYED, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
    assertTrue(destroyInvoked.get());
    assertTrue(handlerInvoked.get());
  }

  @Test
  public void testFailedCustomCleanupViaCallback() throws Exception
  {
    final AtomicBoolean destroyInvoked = new AtomicBoolean();
    final AtomicBoolean handlerInvoked = new AtomicBoolean();
    final Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {

      @Override
      protected PnkyPromise<Void> initInternal()
      {
        return immediatelyFailed(new NumberFormatException());
      }

      @Override
      protected PnkyPromise<Void> destroyInternal()
      {
        destroyInvoked.set(true);
        return immediatelyComplete(null);
      }

      @Override
      protected PnkyPromise<Void> handleInitFailure()
      {
        if (getLifecycleStage() == LifecycleStage.INITIALIZATION_FAILED)
        {
          handlerInvoked.set(true);
        }
        else
        {
          log.error("init failure invoked in wrong state: {}", getLifecycleStage());
        }
        return immediatelyFailed(new NumberFormatException());
      }
    };

    try
    {
      testInstance.init().get(50, TimeUnit.MILLISECONDS);
      fail();
    }
    catch (final ExecutionException e)
    {
      assertThat(e.getCause(), instanceOf(NumberFormatException.class));
    }

    assertEquals(LifecycleStage.DESTROYED, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
    assertTrue(destroyInvoked.get());
    assertTrue(handlerInvoked.get());
  }

  @Test
  public void testFailedCustomCleanupViaRuntimeError() throws Exception
  {
    final AtomicBoolean destroyInvoked = new AtomicBoolean();
    final AtomicBoolean handlerInvoked = new AtomicBoolean();
    final Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {

      @Override
      protected PnkyPromise<Void> initInternal()
      {
        return immediatelyFailed(new NumberFormatException());
      }

      @Override
      protected PnkyPromise<Void> destroyInternal()
      {
        destroyInvoked.set(true);
        return immediatelyComplete(null);
      }

      @Override
      protected PnkyPromise<Void> handleInitFailure()
      {
        if (getLifecycleStage() == LifecycleStage.INITIALIZATION_FAILED)
        {
          handlerInvoked.set(true);
        }
        else
        {
          log.error("init failure invoked in wrong state: {}", getLifecycleStage());
        }
        throw new NumberFormatException();
      }
    };

    try
    {
      testInstance.init().get(200, TimeUnit.MILLISECONDS);
      fail();
    }
    catch (final ExecutionException e)
    {
      assertThat(e.getCause(), instanceOf(NumberFormatException.class));
    }

    assertEquals(LifecycleStage.DESTROYED, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
    assertTrue(destroyInvoked.get());
    assertTrue(handlerInvoked.get());
  }

  @Test
  public void testStageSetAfterDestroy() throws Exception
  {
    final Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        setLifecycleStage(LifecycleStage.INITIALIZED);
      }

      @Override
      protected PnkyPromise<Void> initInternal()
      {
        return immediatelyFailed(new IllegalStateException());
      }

      @Override
      protected PnkyPromise<Void> destroyInternal()
      {
        return immediatelyComplete(null);
      }
    };

    testInstance.destroy().get(50, TimeUnit.MILLISECONDS);

    assertEquals(LifecycleStage.DESTROYED, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
  }

  @Test
  public void testFailedDestroyViaCallback() throws Exception
  {
    final Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        setLifecycleStage(LifecycleStage.INITIALIZED);
      }

      @Override
      protected PnkyPromise<Void> initInternal()
      {
        return immediatelyFailed(new IllegalStateException());
      }

      @Override
      protected PnkyPromise<Void> destroyInternal()
      {
        return immediatelyFailed(new IllegalArgumentException("destroy"));
      }
    };

    try
    {
      testInstance.destroy().get(50, TimeUnit.MILLISECONDS);
      fail();
    }
    catch (final ExecutionException e)
    {
      assertThat(e.getCause(), instanceOf(IllegalArgumentException.class));
    }

    assertEquals(LifecycleStage.DESTROYING, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
  }

  @Test
  public void testDestroyFailedViaRuntimeError() throws Exception
  {
    final Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        setLifecycleStage(LifecycleStage.INITIALIZED);
      }

      @Override
      protected PnkyPromise<Void> initInternal()
      {
        return immediatelyFailed(new IllegalStateException());
      }

      @Override
      protected PnkyPromise<Void> destroyInternal()
      {
        throw new NumberFormatException();
      }
    };

    try
    {
      testInstance.destroy().get(50, TimeUnit.MILLISECONDS);
      fail();
    }
    catch (final ExecutionException e)
    {
      assertThat(e.getCause(), instanceOf(NumberFormatException.class));
    }

    assertEquals(LifecycleStage.DESTROYING, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
  }

  @Test
  public void testInitInWrongState() throws Exception
  {
    final Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        setLifecycleStage(LifecycleStage.INITIALIZING);
      }

      @Override
      protected PnkyPromise<Void> initInternal()
      {
        return immediatelyComplete(null);
      }

      @Override
      protected PnkyPromise<Void> destroyInternal()
      {
        return immediatelyComplete(null);
      }
    };

    try
    {
      testInstance.init().get(50, TimeUnit.MILLISECONDS);
      fail();
    }
    catch (final ExecutionException e)
    {
      assertThat(e.getCause(), instanceOf(IllegalStateException.class));
    }

    assertEquals(LifecycleStage.INITIALIZING, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
  }

  @Test
  public void testDestroyInWrongState() throws Exception
  {
    final Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        setLifecycleStage(LifecycleStage.UNINITIALIZED);
      }

      @Override
      protected PnkyPromise<Void> initInternal()
      {
        return immediatelyComplete(null);
      }

      @Override
      protected PnkyPromise<Void> destroyInternal()
      {
        return immediatelyComplete(null);
      }
    };

    try
    {
      testInstance.destroy().get(50, TimeUnit.MILLISECONDS);
      fail();
    }
    catch (final ExecutionException e)
    {
      assertThat(e.getCause(), instanceOf(IllegalStateException.class));
    }

    assertEquals(LifecycleStage.UNINITIALIZED, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
  }

  @Test
  public void testInitWhenAlreadyInited() throws Exception
  {
    final Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        setLifecycleStage(LifecycleStage.INITIALIZED);
      }

      @Override
      protected PnkyPromise<Void> initInternal()
      {
        return immediatelyFailed(new Exception());
      }

      @Override
      protected PnkyPromise<Void> destroyInternal()
      {
        return immediatelyFailed(new IllegalStateException());
      }
    };

    testInstance.init().get(50, TimeUnit.MILLISECONDS);

    assertEquals(LifecycleStage.INITIALIZED, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
  }

  @Test
  public void testDestroyWhenAlreadyDestroyed() throws Exception
  {
    final Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        setLifecycleStage(LifecycleStage.DESTROYED);
      }

      @Override
      protected PnkyPromise<Void> initInternal()
      {
        return immediatelyFailed(new Exception());
      }

      @Override
      protected PnkyPromise<Void> destroyInternal()
      {
        return immediatelyFailed(new Exception());
      }
    };

    testInstance.destroy().get(50, TimeUnit.MILLISECONDS);

    assertEquals(LifecycleStage.DESTROYED, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
  }

  @Test
  public void testCannotRestart() throws Exception
  {
    final Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        setLifecycleStage(LifecycleStage.DESTROYED);
      }

      @Override
      protected PnkyPromise<Void> initInternal()
      {
        return immediatelyFailed(new Exception());
      }

      @Override
      protected PnkyPromise<Void> destroyInternal()
      {
        return immediatelyFailed(new Exception());
      }
    };

    try
    {
      testInstance.init().get(50, TimeUnit.MILLISECONDS);
    }
    catch (final ExecutionException e)
    {
      assertThat(e.getCause(), instanceOf(IllegalStateException.class));
    }

    assertEquals(LifecycleStage.DESTROYED, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
  }

  @Test
  public void testCanBeRestartedIfRestartable() throws Exception
  {
    final Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        setLifecycleStage(LifecycleStage.DESTROYED);
      }

      @Override
      protected PnkyPromise<Void> initInternal()
      {
        return immediatelyComplete(null);
      }

      @Override
      protected PnkyPromise<Void> destroyInternal()
      {
        return immediatelyFailed(new Exception());
      }

      @Override
      protected boolean isRestartable()
      {
        return true;
      }
    };

    testInstance.init().get(50, TimeUnit.MILLISECONDS);

    assertEquals(LifecycleStage.INITIALIZED, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
  }

  @Test
  public void testListenerStages() throws Exception
  {
    final ListenableLifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      @Override
      protected PnkyPromise<Void> initInternal()
      {
        return immediatelyComplete(null);
      }

      @Override
      protected PnkyPromise<Void> destroyInternal()
      {
        return immediatelyComplete(null);
      }
    };

    final TestListener listener = new TestListener();
    testInstance.getLifecycleListenable().addListener(listener);

    await().until(() -> assertEquals(LifecycleStage.UNINITIALIZED, listener.state));

    testInstance.init().get(50, TimeUnit.MILLISECONDS);

    await().until(() -> assertEquals(LifecycleStage.INITIALIZED, listener.state));

    testInstance.destroy().get(50, TimeUnit.MILLISECONDS);

    await().until(() -> assertEquals(LifecycleStage.DESTROYED, listener.state));

    assertEquals(Lists.newArrayList(
        LifecycleStage.UNINITIALIZED,
        LifecycleStage.INITIALIZING,
        LifecycleStage.INITIALIZED,
        LifecycleStage.DESTROYING,
        LifecycleStage.DESTROYED), listener.transitions);
  }

  @Test
  public void testRestartableListenerStages() throws Exception
  {
    final ListenableLifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        setLifecycleStage(LifecycleStage.INITIALIZED);
      }

      @Override
      protected PnkyPromise<Void> initInternal()
      {
        return immediatelyComplete(null);
      }

      @Override
      protected PnkyPromise<Void> destroyInternal()
      {
        return immediatelyComplete(null);
      }

      @Override
      protected boolean isRestartable()
      {
        return true;
      }
    };

    final TestListener listener = new TestListener();
    testInstance.getLifecycleListenable().addListener(listener);

    await().until(() -> assertEquals(LifecycleStage.INITIALIZED, listener.state));

    testInstance.destroy().get(50, TimeUnit.MILLISECONDS);

    await().until(() -> assertEquals(LifecycleStage.DESTROYED, listener.state));

    testInstance.init().get(50, TimeUnit.MILLISECONDS);

    await().until(() -> assertEquals(LifecycleStage.INITIALIZED, listener.state));
  }

  @Test
  public void testListenerNotifiedWhenInitFails() throws Exception
  {
    final AtomicBoolean destroyInvoked = new AtomicBoolean();
    final ListenableLifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      @Override
      protected PnkyPromise<Void> initInternal()
      {
        throw new NumberFormatException();
      }

      @Override
      protected PnkyPromise<Void> destroyInternal()
      {
        destroyInvoked.set(true);
        return immediatelyComplete(null);
      }
    };

    final TestListener listener = new TestListener();
    testInstance.getLifecycleListenable().addListener(listener);

    await().until(() -> assertEquals(LifecycleStage.UNINITIALIZED, listener.state));

    try
    {
      testInstance.init().get(50, TimeUnit.MILLISECONDS);
      fail();
    }
    catch (final ExecutionException e)
    {
      assertThat(e.getCause(), instanceOf(NumberFormatException.class));
    }

    await().until(() -> assertEquals(LifecycleStage.DESTROYED, listener.state));

    assertEquals(Lists.newArrayList(
        LifecycleStage.UNINITIALIZED,
        LifecycleStage.INITIALIZING,
        LifecycleStage.INITIALIZATION_FAILED,
        LifecycleStage.DESTROYING,
        LifecycleStage.DESTROYED), listener.transitions);
  }

  private static class TestListener implements LifecycleListener
  {
    private final List<LifecycleStage> transitions = Lists.newArrayList();
    private LifecycleStage state = null;

    @Override
    public void stateChanged(final LifecycleStage newState)
    {
      transitions.add(newState);
      state = newState;
    }
  }
}
