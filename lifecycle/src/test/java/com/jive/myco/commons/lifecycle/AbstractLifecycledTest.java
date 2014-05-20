package com.jive.myco.commons.lifecycle;

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

import com.jive.myco.commons.callbacks.Callback;
import com.jive.myco.commons.callbacks.CallbackFuture;
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
      protected void initInternal(Callback<Void> callback)
      {
        callback.onSuccess(null);
      }

      @Override
      protected void destroyInternal(Callback<Void> callback)
      {
        callback.onFailure(new IllegalStateException());
      }
    };

    CallbackFuture<Void> callback = new CallbackFuture<>();
    testInstance.init(callback);
    callback.get(50, TimeUnit.MILLISECONDS);

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
      protected void initInternal(Callback<Void> callback)
      {
        callback.onFailure(new IllegalArgumentException("foo"));
      }

      @Override
      protected void destroyInternal(Callback<Void> callback)
      {
        destroyInvoked.set(true);
        callback.onSuccess(null);
      }
    };

    CallbackFuture<Void> callback = new CallbackFuture<>();
    testInstance.init(callback);
    try
    {
      callback.get(50, TimeUnit.MILLISECONDS);
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
      protected void initInternal(Callback<Void> callback)
      {
        throw new NumberFormatException();
      }

      @Override
      protected void destroyInternal(Callback<Void> callback)
      {
        destroyInvoked.set(true);
        callback.onSuccess(null);
      }
    };

    CallbackFuture<Void> callback = new CallbackFuture<>();
    testInstance.init(callback);
    try
    {
      callback.get(50, TimeUnit.MILLISECONDS);
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
      protected void initInternal(Callback<Void> callback)
      {
        callback.onFailure(new NumberFormatException());
      }

      @Override
      protected void destroyInternal(Callback<Void> callback)
      {
        destroyInvoked.set(true);
        callback.onSuccess(null);
      }

      @Override
      protected void handleInitFailure(Callback<Void> callback)
      {
        if (lifecycleStage == LifecycleStage.INITIALIZATION_FAILED)
        {
          handlerInvoked.set(true);
        }
        else
        {
          log.error("init failure invoked in wrong state: {}", lifecycleStage);
        }
        callback.onSuccess(null);
      }
    };

    CallbackFuture<Void> callback = new CallbackFuture<>();
    testInstance.init(callback);
    try
    {
      callback.get(50, TimeUnit.MILLISECONDS);
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
      protected void initInternal(Callback<Void> callback)
      {
        callback.onFailure(new NumberFormatException());
      }

      @Override
      protected void destroyInternal(Callback<Void> callback)
      {
        destroyInvoked.set(true);
        callback.onSuccess(null);
      }

      @Override
      protected void handleInitFailure(Callback<Void> callback)
      {
        if (lifecycleStage == LifecycleStage.INITIALIZATION_FAILED)
        {
          handlerInvoked.set(true);
        }
        else
        {
          log.error("init failure invoked in wrong state: {}", lifecycleStage);
        }
        callback.onFailure(new NumberFormatException());
      }
    };

    CallbackFuture<Void> callback = new CallbackFuture<>();
    testInstance.init(callback);
    try
    {
      callback.get(50, TimeUnit.MILLISECONDS);
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
      protected void initInternal(Callback<Void> callback)
      {
        callback.onFailure(new NumberFormatException());
      }

      @Override
      protected void destroyInternal(Callback<Void> callback)
      {
        destroyInvoked.set(true);
        callback.onSuccess(null);
      }

      @Override
      protected void handleInitFailure(Callback<Void> callback)
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

    CallbackFuture<Void> callback = new CallbackFuture<>();
    testInstance.init(callback);
    try
    {
      callback.get(50, TimeUnit.MILLISECONDS);
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
      protected void initInternal(Callback<Void> callback)
      {
        callback.onFailure(new IllegalStateException());
      }

      @Override
      protected void destroyInternal(Callback<Void> callback)
      {
        callback.onSuccess(null);
      }
    };

    CallbackFuture<Void> callback = new CallbackFuture<>();
    testInstance.destroy(callback);
    callback.get(50, TimeUnit.MILLISECONDS);

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
      protected void initInternal(Callback<Void> callback)
      {
        callback.onFailure(new IllegalStateException());
      }

      @Override
      protected void destroyInternal(Callback<Void> callback)
      {
        callback.onFailure(new IllegalArgumentException("destroy"));
      }
    };

    CallbackFuture<Void> callback = new CallbackFuture<>();
    testInstance.destroy(callback);
    try
    {
      callback.get(50, TimeUnit.MILLISECONDS);
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
      protected void initInternal(Callback<Void> callback)
      {
        callback.onFailure(new IllegalStateException());
      }

      @Override
      protected void destroyInternal(Callback<Void> callback)
      {
        throw new NumberFormatException();
      }
    };

    CallbackFuture<Void> callback = new CallbackFuture<>();
    testInstance.destroy(callback);
    try
    {
      callback.get(50, TimeUnit.MILLISECONDS);
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
      protected void initInternal(Callback<Void> callback)
      {
        callback.onSuccess(null);
      }

      @Override
      protected void destroyInternal(Callback<Void> callback)
      {
        callback.onSuccess(null);
      }
    };

    CallbackFuture<Void> callback = new CallbackFuture<>();
    testInstance.init(callback);
    try
    {
      callback.get(50, TimeUnit.MILLISECONDS);
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
      protected void initInternal(Callback<Void> callback)
      {
        callback.onSuccess(null);
      }

      @Override
      protected void destroyInternal(Callback<Void> callback)
      {
        callback.onSuccess(null);
      }
    };

    CallbackFuture<Void> callback = new CallbackFuture<>();
    testInstance.destroy(callback);
    try
    {
      callback.get(50, TimeUnit.MILLISECONDS);
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
      protected void initInternal(Callback<Void> callback)
      {
        callback.onFailure(new Exception());
      }

      @Override
      protected void destroyInternal(Callback<Void> callback)
      {
        callback.onFailure(new IllegalStateException());
      }
    };

    CallbackFuture<Void> callback = new CallbackFuture<>();
    testInstance.init(callback);
    callback.get(50, TimeUnit.MILLISECONDS);

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
      protected void initInternal(Callback<Void> callback)
      {
        callback.onFailure(new Exception());
      }

      @Override
      protected void destroyInternal(Callback<Void> callback)
      {
        callback.onFailure(new Exception());
      }
    };

    CallbackFuture<Void> callback = new CallbackFuture<>();
    testInstance.destroy(callback);
    callback.get(50, TimeUnit.MILLISECONDS);

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
      protected void initInternal(Callback<Void> callback)
      {
        callback.onFailure(new Exception());
      }

      @Override
      protected void destroyInternal(Callback<Void> callback)
      {
        callback.onFailure(new Exception());
      }
    };

    CallbackFuture<Void> callback = new CallbackFuture<>();
    testInstance.init(callback);
    try
    {
      callback.get(50, TimeUnit.MILLISECONDS);
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
      protected void initInternal(Callback<Void> callback)
      {
        callback.onSuccess(null);
      }

      @Override
      protected void destroyInternal(Callback<Void> callback)
      {
        callback.onFailure(new Exception());
      }

      @Override
      protected boolean isRestartable()
      {
        return true;
      }
    };

    CallbackFuture<Void> callback = new CallbackFuture<>();
    testInstance.init(callback);
    callback.get(50, TimeUnit.MILLISECONDS);

    assertEquals(LifecycleStage.INITIALIZED, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
  }
}
