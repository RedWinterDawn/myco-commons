package com.jive.myco.commons.lifecycle.guava;

import static com.jayway.awaitility.Awaitility.*;
import static org.junit.Assert.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import com.jive.myco.commons.concurrent.ImmediateExecutor;
import com.jive.myco.commons.lifecycle.LifecycleStage;
import com.jive.myco.commons.lifecycle.ListenableLifecycled;

/**
 * Tests for {@link GuavaServiceToLifecycledWrapper}.
 *
 * @author David Valeri
 */
public class GuavaServiceToLifecycledTest extends AbstractTest
{
  @Test
  public void testListenerStagesSuccess() throws Exception
  {
    // Create the thing to test
    final Service testService = new TestService(false, false);
    final ListenableLifecycled testLifecycled =
        new GuavaServiceToLifecycledWrapper(testService, dispatchQueueBuilder);

    // Set listeners

    final TestLifecycledListener lifecycledListener = new TestLifecycledListener();
    testLifecycled.getLifecycleListenable().addListener(lifecycledListener);

    final TestGuavaServiceListener serviceListener = new TestGuavaServiceListener();
    testService.addListener(serviceListener, ImmediateExecutor.INSTANCE);

    // Now work it

    await().until(() -> assertEquals(LifecycleStage.UNINITIALIZED, lifecycledListener.state));

    testLifecycled.init().get(50, TimeUnit.MILLISECONDS);

    await().until(() -> assertEquals(LifecycleStage.INITIALIZED, lifecycledListener.state));

    testLifecycled.destroy().get(50, TimeUnit.MILLISECONDS);

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
    final Service testService = new TestService(true, false);
    final ListenableLifecycled testLifecycled =
        new GuavaServiceToLifecycledWrapper(testService, dispatchQueueBuilder);

    // Set listeners

    final TestLifecycledListener lifecycledListener = new TestLifecycledListener();
    testLifecycled.getLifecycleListenable().addListener(lifecycledListener);

    final TestGuavaServiceListener serviceListener = new TestGuavaServiceListener();
    testService.addListener(serviceListener, ImmediateExecutor.INSTANCE);

    // Now work it

    await().until(() -> assertEquals(LifecycleStage.UNINITIALIZED, lifecycledListener.state));

    try
    {
      testLifecycled.init().get();
    }
    catch (final ExecutionException e)
    {
      assertEquals(NumberFormatException.class, e.getCause().getClass());
    }

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
    final Service testService = new TestService(false, true);
    final ListenableLifecycled testLifecycled =
        new GuavaServiceToLifecycledWrapper(testService, dispatchQueueBuilder);

    // Set listeners

    final TestLifecycledListener lifecycledListener = new TestLifecycledListener();
    testLifecycled.getLifecycleListenable().addListener(lifecycledListener);

    final TestGuavaServiceListener serviceListener = new TestGuavaServiceListener();
    testService.addListener(serviceListener, ImmediateExecutor.INSTANCE);

    // Now work it

    await().until(() -> assertEquals(LifecycleStage.UNINITIALIZED, lifecycledListener.state));

    testLifecycled.init().get(50, TimeUnit.MILLISECONDS);

    await().until(() -> assertEquals(LifecycleStage.INITIALIZED, lifecycledListener.state));

    try
    {
      testLifecycled.destroy().get();
    }
    catch (final ExecutionException e)
    {
      assertEquals(NumberFormatException.class, e.getCause().getClass());
    }

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
            Service.State.FAILED),
        serviceListener.transitions);
  }

  @RequiredArgsConstructor
  private static final class TestService extends AbstractService
  {
    private final boolean failStart;
    private final boolean failStop;

    @Override
    protected void doStart()
    {
      if (failStart)
      {
        notifyFailed(new NumberFormatException("foo"));
      }
      else
      {
        notifyStarted();
      }
    }

    @Override
    protected void doStop()
    {
      if (failStop)
      {
        notifyFailed(new NumberFormatException("bar"));
      }
      else
      {
        notifyStopped();
      }
    }
  }
}
