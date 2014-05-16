package com.jive.myco.commons.lifecycle;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.fusesource.hawtdispatch.DispatchQueue;
import org.junit.Test;

import com.jive.myco.commons.callbacks.Callback;
import com.jive.myco.commons.callbacks.CallbackFuture;
import com.jive.myco.commons.hawtdispatch.SameThreadTestQueueBuilder;

/**
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public class AbstractLifecycledTest
{
  private DispatchQueue testQueue = SameThreadTestQueueBuilder.getTestQueue("lifecycle");

  @Test
  public void testStageSetAfterSuccess() throws Exception
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
        callback.onFailure(new IllegalStateException());
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

    assertEquals(LifecycleStage.INITIALIZATION_FAILED, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
  }

  @Test
  public void testInitializationFailedViaRuntimeError() throws Exception
  {
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
        callback.onFailure(new IllegalStateException());
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

    assertEquals(LifecycleStage.INITIALIZATION_FAILED, testInstance.getLifecycleStage());
    assertFalse(testQueue.isSuspended());
  }

  @Test
  public void testStageSetAfterDestroy() throws Exception
  {
    Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        lifecycleStage.set(LifecycleStage.INITIALIZED);
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
        lifecycleStage.set(LifecycleStage.INITIALIZED);
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
        lifecycleStage.set(LifecycleStage.INITIALIZED);
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
        lifecycleStage.set(LifecycleStage.INITIALIZING);
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
        lifecycleStage.set(LifecycleStage.UNINITIALIZED);
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
        lifecycleStage.set(LifecycleStage.INITIALIZED);
      }

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
  public void testDestroyWhenAlreadyDestroyed() throws Exception
  {
    Lifecycled testInstance = new AbstractLifecycled(testQueue)
    {
      {
        lifecycleStage.set(LifecycleStage.DESTROYED);
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
}
