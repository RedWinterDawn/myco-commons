package com.jive.myco.commons.lifecycle.guava;

import static com.jayway.awaitility.Awaitility.*;
import static com.jive.myco.commons.concurrent.Pnky.*;
import static org.junit.Assert.*;

import org.fusesource.hawtdispatch.DispatchQueue;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Service;
import com.jive.myco.commons.concurrent.ImmediateExecutor;
import com.jive.myco.commons.concurrent.PnkyPromise;
import com.jive.myco.commons.lifecycle.AbstractLifecycled;
import com.jive.myco.commons.lifecycle.LifecycleStage;
import com.jive.myco.commons.lifecycle.ListenableLifecycled;

/**
 * Tests for {@link LifecycledToGuavaServiceWrapper}.
 *
 * @author David Valeri
 */
public class LifecycledToGuavaServiceWrapperTest extends AbstractTest
{
  @Test
  public void testListenerStagesSuccess() throws Exception
  {
    // Create the thing to test
    final ListenableLifecycled testLifecycled =
        new TestLifecycled(dispatchQueueBuilder.build(), false, false);
    final Service testService = new LifecycledToGuavaServiceWrapper(testLifecycled);

    // Set listeners

    final TestLifecycledListener lifecycledListener = new TestLifecycledListener();
    testLifecycled.getLifecycleListenable().addListener(lifecycledListener);

    final TestGuavaServiceListener serviceListener = new TestGuavaServiceListener();
    testService.addListener(serviceListener, ImmediateExecutor.INSTANCE);

    // Now work it

    await().until(() -> assertEquals(LifecycleStage.UNINITIALIZED, lifecycledListener.state));

    testService.startAsync();
    testService.awaitRunning();

    await().until(() -> assertEquals(LifecycleStage.INITIALIZED, lifecycledListener.state));

    testService.stopAsync();
    testService.awaitTerminated();

    await().until(() -> assertEquals(LifecycleStage.DESTROYED, lifecycledListener.state));

    // Validate

    assertEquals(
        Lists.newArrayList(
            LifecycleStage.UNINITIALIZED,
            LifecycleStage.INITIALIZING,
            LifecycleStage.INITIALIZED,
            LifecycleStage.DESTROYING,
            LifecycleStage.DESTROYED),
        lifecycledListener.transitions);

    assertEquals(
        Lists.newArrayList(
            Service.State.STARTING,
            Service.State.RUNNING,
            Service.State.STOPPING,
            Service.State.TERMINATED),
        serviceListener.transitions);
  }

  @Test
  public void testListenerStagesFailInit() throws Exception
  {
    // Create the thing to test
    final ListenableLifecycled testLifecycled =
        new TestLifecycled(dispatchQueueBuilder.build(), true, false);
    final Service testService = new LifecycledToGuavaServiceWrapper(testLifecycled);

    // Set listeners

    final TestLifecycledListener lifecycledListener = new TestLifecycledListener();
    testLifecycled.getLifecycleListenable().addListener(lifecycledListener);

    final TestGuavaServiceListener serviceListener = new TestGuavaServiceListener();
    testService.addListener(serviceListener, ImmediateExecutor.INSTANCE);

    // Now work it

    await().until(() -> assertEquals(LifecycleStage.UNINITIALIZED, lifecycledListener.state));

    testService.startAsync();

    try
    {
      testService.awaitRunning();
      fail();
    }
    catch (final IllegalStateException e)
    {
      // Expected
    }

    assertEquals(NumberFormatException.class, testService.failureCause().getClass());

    await().until(() -> assertEquals(LifecycleStage.DESTROYED, lifecycledListener.state));

    // Validate

    assertEquals(
        Lists.newArrayList(
            LifecycleStage.UNINITIALIZED,
            LifecycleStage.INITIALIZING,
            LifecycleStage.INITIALIZATION_FAILED,
            LifecycleStage.DESTROYING,
            LifecycleStage.DESTROYED),
        lifecycledListener.transitions);

    assertEquals(
        Lists.newArrayList(
            Service.State.STARTING,
            Service.State.FAILED),
        serviceListener.transitions);
  }

  @Test
  public void testListenerStagesFailDestroy() throws Exception
  {
    // Create the thing to test
    final ListenableLifecycled testLifecycled =
        new TestLifecycled(dispatchQueueBuilder.build(), false, true);
    final Service testService = new LifecycledToGuavaServiceWrapper(testLifecycled);

    // Set listeners

    final TestLifecycledListener lifecycledListener = new TestLifecycledListener();
    testLifecycled.getLifecycleListenable().addListener(lifecycledListener);

    final TestGuavaServiceListener serviceListener = new TestGuavaServiceListener();
    testService.addListener(serviceListener, ImmediateExecutor.INSTANCE);

    // Now work it

    await().until(() -> assertEquals(LifecycleStage.UNINITIALIZED, lifecycledListener.state));

    testService.startAsync();
    testService.awaitRunning();

    await().until(() -> assertEquals(LifecycleStage.INITIALIZED, lifecycledListener.state));

    testService.stopAsync();

    try
    {
      testService.awaitTerminated();
      fail();
    }
    catch (final IllegalStateException e)
    {
      // Expected
    }

    await().until(() -> assertEquals(LifecycleStage.DESTROYING, lifecycledListener.state));

    assertEquals(NumberFormatException.class, testService.failureCause().getClass());

    // Validate

    assertEquals(
        Lists.newArrayList(
            LifecycleStage.UNINITIALIZED,
            LifecycleStage.INITIALIZING,
            LifecycleStage.INITIALIZED,
            LifecycleStage.DESTROYING),
        lifecycledListener.transitions);

    assertEquals(
        Lists.newArrayList(
            Service.State.STARTING,
            Service.State.RUNNING,
            Service.State.STOPPING,
            Service.State.FAILED),
        serviceListener.transitions);
  }

  private static class TestLifecycled extends AbstractLifecycled
  {
    private final boolean failInit;
    private final boolean failDestroy;

    public TestLifecycled(final DispatchQueue lifecycleQueue, final boolean failInit,
        final boolean failDestroy)
    {
      super(lifecycleQueue);
      this.failInit = failInit;
      this.failDestroy = failDestroy;
    }

    @Override
    protected PnkyPromise<Void> initInternal()
    {
      if (failInit)
      {
        return immediatelyFailed(new NumberFormatException("foo"));
      }
      else
      {
        return immediatelyComplete(null);
      }
    }

    @Override
    protected PnkyPromise<Void> destroyInternal()
    {
      if (failDestroy)
      {
        return immediatelyFailed(new NumberFormatException("bar"));
      }
      else
      {
        return immediatelyComplete(null);
      }
    }
  };
}
