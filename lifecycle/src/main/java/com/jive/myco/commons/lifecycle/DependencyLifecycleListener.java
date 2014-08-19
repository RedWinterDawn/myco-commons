package com.jive.myco.commons.lifecycle;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.fusesource.hawtdispatch.DispatchQueue;

import com.jive.myco.commons.concurrent.Pnky;

/**
 * A listener for a required upstream dependency. Used to trigger the completion of initialization
 * and destruction on destruction of the upstream dependency.
 *
 * @author David Valeri
 */
@RequiredArgsConstructor
@Slf4j
public class DependencyLifecycleListener implements LifecycleListener
{
  /**
   * ID used for logging purposes, usually the ID of the {@link #dependant}
   */
  private final String id;

  /**
   * The service that depends on the service that this listener is added to
   */
  @NonNull
  private final Lifecycled dependant;

  /**
   * The lifecycle queue of the {@link #dependant}
   */
  @NonNull
  private final DispatchQueue lifecycleQueue;

  /**
   * The name of the dependency to which this listener is added
   */
  @NonNull
  private final String dependencyName;

  @Getter
  private final Pnky<Void> future = Pnky.create();

  @Override
  public void stateChanged(final LifecycleStage newState)
  {
    if (newState == LifecycleStage.INITIALIZED)
    {
      if (!future.isDone())
      {
        log.debug("[{}]: {} started.", id, dependencyName);
        future.resolve(null);
      }
    }
    // One of our dependencies went into a failed or destroying state
    else if (newState == LifecycleStage.INITIALIZATION_FAILED
        || newState.hasAchieved(LifecycleStage.DESTROYING))
    {
      // Reject the future, if it wasn't already handled.
      future.reject(new IllegalStateException(
          String.format(
              "[%s]: %s did not satisfy lifecycle requirements during initialization.",
              id, dependencyName)));

      // Do the next bit on the lifecycle queue since it needs to check our current state before
      // making a decision.
      Pnky.runAsync(() ->
      {
        // We are still initializing or we are initialized, which means we are going to fail too
        // since our dependency failed.
        if (dependant.getLifecycleStage() == LifecycleStage.INITIALIZING
            || dependant.getLifecycleStage() == LifecycleStage.INITIALIZED)
        {
          log.error(
              "[{}]: {} failed startup or was destroyed ([{}]) while this "
                  + "manager was initializing or initialized ([{}]).  Triggering destruction.",
              id, dependencyName, newState, dependant.getLifecycleStage());

          dependant.destroy()
              .alwaysAccept((result, cause) ->
              {
                if (cause == null)
                {
                  log.info(
                      "[{}]: Destroyed by upstream dependency failure.", id);
                }
                else
                {
                  log.error(
                      "[{}]: Error handling destroy triggered by upstream dependency.",
                      id, cause);
                }
              });
        }
      }, lifecycleQueue);
    }
  }
}
