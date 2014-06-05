package com.jive.myco.commons.lifecycle;

import static com.jive.myco.commons.concurrent.Pnky.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;

import org.fusesource.hawtdispatch.DispatchQueue;
import org.junit.Test;
import org.mockito.Matchers;

import com.jive.myco.commons.concurrent.PnkyPromise;
import com.jive.myco.commons.hawtdispatch.SameThreadTestQueueBuilder;

/**
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
@Slf4j
public class AbstractLifecycledTest
{
  private DispatchQueue testQueue = SameThreadTestQueueBuilder.getTestQueue("lifecycle");

  @Test
  public void testStageSetAfterSuccess() throws Exception
  {
    simpleInitTest();
    verify(testQueue).execute(Matchers.any(Runnable.class));
  }

  @Test
  public void testDontQueueIfAlreadyOnQueue() throws Exception
  {
    when(testQueue.isExecuting()).thenReturn(true);
    simpleInitTest();
    verify(testQueue, never()).execute(Matchers.any(Runnable.class));
  }

  private void simpleInitTest() throws Exception
  {
    Lifecycled testInstance = new AbstractLifecycled(testQueue)
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
  }

  @Test
  public void testFailedInitializationViaCallback() throws Exception
  {
    final AtomicBoolean destroyInvoked = new AtomicBoolean();
    Lifecycled testInstance = new AbstractLifecycled(testQueue)
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
    catch (ExecutionException e)
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
    Lifecycled testInstance = new AbstractLifecycled(testQueue)
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
    catch (ExecutionException e)
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
    Lifecycled testInstance = new AbstractLifecycled(testQueue)
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
        if (lifecycleStage == LifecycleStage.INITIALIZATION_FAILED)
        {
          handlerInvoked.set(true);
        }
        else
        {
          log.error("init failure invoked in wrong state: {}", lifecycleStage);
        }
        return immediatelyComplete(null);
      }
    };

    try
    {
      testInstance.init().get(50, TimeUnit.MILLISECONDS);
      fail();
    }
    catch (ExecutionException e)
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
    Lifecycled testInstance = new AbstractLifecycled(testQueue)
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
        if (lifecycleStage == LifecycleStage.INITIALIZATION_FAILED)
        {
          handlerInvoked.set(true);
        }
        else
        {
          log.error("init failure invoked in wrong state: {}", lifecycleStage);
        }
        return immediatelyFailed(new NumberFormatException());
      }
    };

    try
    {
      testInstance.init().get(50, TimeUnit.MILLISECONDS);
      fail();
    }
    catch (ExecutionException e)
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
    Lifecycled testInstance = new AbstractLifecycled(testQueue)
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
        if (lifecycleStage == LifecycleStage.INITIALIZATION_FAILED)
        {
          handlerInvoked.set(true);
        }
        else
        {
          log.error("init failure invoked in wrong state: {}", lifecycleStage);
        }
        throw new NumberFormatException();
      }
    };

    try
    {
      testInstance.init().get(50, TimeUnit.MILLISECONDS);
      fail();
    }
    catch (ExecutionException e)
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
    Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        lifecycleStage = LifecycleStage.INITIALIZED;
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
    Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        lifecycleStage = LifecycleStage.INITIALIZED;
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
    catch (ExecutionException e)
    {
      assertThat(e.getCause(), instanceOf(IllegalArgumentException.class));
    }

    assertEquals(LifecycleStage.DESTROYING, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
  }

  @Test
  public void testDestroyFailedViaRuntimeError() throws Exception
  {
    Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        lifecycleStage = LifecycleStage.INITIALIZED;
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
    catch (ExecutionException e)
    {
      assertThat(e.getCause(), instanceOf(NumberFormatException.class));
    }

    assertEquals(LifecycleStage.DESTROYING, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
  }

  @Test
  public void testInitInWrongState() throws Exception
  {
    Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        lifecycleStage = LifecycleStage.INITIALIZING;
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
    catch (ExecutionException e)
    {
      assertThat(e.getCause(), instanceOf(IllegalStateException.class));
    }

    assertEquals(LifecycleStage.INITIALIZING, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
  }

  @Test
  public void testDestroyInWrongState() throws Exception
  {
    Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        lifecycleStage = LifecycleStage.UNINITIALIZED;
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
    catch (ExecutionException e)
    {
      assertThat(e.getCause(), instanceOf(IllegalStateException.class));
    }

    assertEquals(LifecycleStage.UNINITIALIZED, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
  }

  @Test
  public void testInitWhenAlreadyInited() throws Exception
  {
    Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        lifecycleStage = LifecycleStage.INITIALIZED;
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
    Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        lifecycleStage = LifecycleStage.DESTROYED;
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
    Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        lifecycleStage = LifecycleStage.DESTROYED;
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
    catch (ExecutionException e)
    {
      assertThat(e.getCause(), instanceOf(IllegalStateException.class));
    }

    assertEquals(LifecycleStage.DESTROYED, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
  }

  @Test
  public void testCanBeRestartedIfRestartable() throws Exception
  {
    Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        lifecycleStage = LifecycleStage.DESTROYED;
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
}
