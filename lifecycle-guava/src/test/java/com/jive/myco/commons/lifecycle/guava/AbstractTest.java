package com.jive.myco.commons.lifecycle.guava;

import java.util.List;

import org.junit.Before;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.Service.State;
import com.jive.myco.commons.hawtdispatch.DefaultDispatchQueueBuilder;
import com.jive.myco.commons.hawtdispatch.DispatchQueueBuilder;
import com.jive.myco.commons.lifecycle.LifecycleListener;
import com.jive.myco.commons.lifecycle.LifecycleStage;

/**
 * Base class for tests.
 *
 * @author David Valeri
 */
public abstract class AbstractTest
{
  protected DispatchQueueBuilder dispatchQueueBuilder;

  @Before
  public void setup()
  {
    dispatchQueueBuilder = DefaultDispatchQueueBuilder.getDefaultBuilder();
  }

  protected static class TestLifecycledListener implements LifecycleListener
  {
    protected final List<LifecycleStage> transitions = Lists.newArrayList();
    protected LifecycleStage state = null;

    @Override
    public void stateChanged(final LifecycleStage newState)
    {
      transitions.add(newState);
      state = newState;
    }
  }

  protected static class TestGuavaServiceListener extends Service.Listener
  {
    protected final List<Service.State> transitions = Lists.newArrayList();
    protected Service.State state = null;

    @Override
    public void starting()
    {
      state = Service.State.STARTING;
      transitions.add(state);
    }

    @Override
    public void running()
    {
      state = Service.State.RUNNING;
      transitions.add(state);
    }

    @Override
    public void stopping(final State from)
    {
      state = Service.State.STOPPING;
      transitions.add(state);
    }

    @Override
    public void terminated(final State from)
    {
      state = Service.State.TERMINATED;
      transitions.add(state);
    }

    @Override
    public void failed(final State from, final Throwable failure)
    {
      state = Service.State.FAILED;
      transitions.add(state);
    }
  }

}
