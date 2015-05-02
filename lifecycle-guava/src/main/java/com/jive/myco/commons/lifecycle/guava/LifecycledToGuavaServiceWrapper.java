package com.jive.myco.commons.lifecycle.guava;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import com.jive.myco.commons.lifecycle.LifecycleListener;
import com.jive.myco.commons.lifecycle.LifecycleStage;
import com.jive.myco.commons.lifecycle.Lifecycled;
import com.jive.myco.commons.lifecycle.ListenableLifecycled;

/**
 * A wrapper for a {@link Lifecycled} to turn it into a Guava {@link Service}.
 *
 * @author David Valeri
 */
@Slf4j
@RequiredArgsConstructor
public class LifecycledToGuavaServiceWrapper extends AbstractService
{
  @NonNull
  private final ListenableLifecycled lifecycled;

  @Override
  protected void doStart()
  {
    lifecycled.getLifecycleListenable().addListener(new LifecycleListener()
    {
      private boolean failed;

      @Override
      public void stateChanged(final LifecycleStage newState)
      {
        switch (newState)
        {
          case UNINITIALIZED:
            // No need to alert on this, we start here
            break;
          case INITIALIZING:
            // No hook for this, handled by super class
            break;
          case INITIALIZATION_FAILED:
            failed = true;
            break;
          case INITIALIZED:
            notifyStarted();
            break;
          case DESTROYING:
            // No hook for this, handled by super class
            break;
          case DESTROYED:
            if (!failed)
            {
              notifyStopped();
            }
            break;
          default:
            log.error("Unknown lifecycle stage [{}].", newState);
        }
      }
    });

    lifecycled
        .init()
        .onFailure(this::notifyFailed);
  }

  @Override
  protected void doStop()
  {
    lifecycled
        .destroy()
        .onFailure(this::notifyFailed);
  }
}
