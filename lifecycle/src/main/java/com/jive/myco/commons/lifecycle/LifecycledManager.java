package com.jive.myco.commons.lifecycle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import org.fusesource.hawtdispatch.DispatchQueue;

import com.google.common.collect.Lists;
import com.jive.myco.commons.concurrent.CombinedException;
import com.jive.myco.commons.concurrent.Pnky;
import com.jive.myco.commons.concurrent.PnkyPromise;
import com.jive.myco.commons.function.ExceptionalFunction;

/**
 * A manager for controlling the startup and shutdown of a number of lifecycled things.
 * Initialization may be performed using any of the supported {@link InitMode}s while destruction
 * always occurs in the reverse iteration order of the supplied {@link ListenableLifecycled}s.
 * Initialization is an all or nothing process while destruction of each lifecycled is guaranteed to
 * be attempted during destruction. Destruction completes successfully if all lifecycleds complete
 * their destruction successfully. Destruction completes exceptionally if any of the lifecycleds
 * completes destruction exceptionally; however, all lifecycleds are guaranteed to have their 
 * {@code destroy} function invoked. A {@link CombinedException} is produced when one or more 
 * consituent lifecycleds fails destruction.
 *
 * @author David Valeri
 */
@Slf4j
public class LifecycledManager extends AbstractLifecycled
{
  /**
   * The initialization mode used by a manager instance.
   */
  public static enum InitMode
  {
    /**
     * All of the {@link LifecycledManagerBuilder#managedLifecycleds(List) managed lifecycleds} are
     * {@link Lifecycled#init() init'd} immediately and are responsible for managing their
     * initialization ordering themselves.
     */
    CONCURRENT,

    /**
     * Each of the {@link LifecycledManagerBuilder#managedLifecycleds(List) managed lifecycleds} is
     * {@link Lifecycled#init() init'd} fully before the subsequent managed lifecycled is init'd.
     */
    CONSECUTIVE;
  }

  /**
   * An identifier assigned to this instance.
   */
  @Getter
  private final String id;

  /**
   * The {@link ListenableLifecycled}s that this manager controls.
   */
  private final List<ListenableLifecycled> managedLifecycleds;

  /**
   * The initialization mode that this manager uses.
   */
  private final InitMode initMode;

  @Builder
  private LifecycledManager(final String id, final DispatchQueue dispatchQueue,
      @NonNull final Collection<? extends ListenableLifecycled> managedLifecycleds,
      @NonNull final InitMode initMode)
  {
    super(id, dispatchQueue);

    this.id = id;
    this.managedLifecycleds = new ArrayList<>(managedLifecycleds);
    this.initMode = initMode;
  }

  @Override
  protected PnkyPromise<Void> initInternal()
  {
    final PnkyPromise<Void> result;
    switch (initMode)
    {
      case CONCURRENT:
        result = initConcurrent();
        break;
      case CONSECUTIVE:
        result = initConsecutive();
        break;
      default:
        result = Pnky.immediatelyFailed(
            new IllegalStateException(
                String.format("[%s]: Unknown init mode [%s].", id, initMode)));
    }

    return result;
  }

  @Override
  protected PnkyPromise<Void> destroyInternal()
  {
    log.debug("[{}]: Destroying consecutively.", id);

    PnkyPromise<Void> future = Pnky.immediatelyComplete(null);

    final List<Throwable> errors = new ArrayList<>(managedLifecycleds.size());

    for (final Lifecycled lifecycled : Lists.reverse(managedLifecycleds))
    {
      future = future.alwaysCompose((voyd, cause) ->
      {
        // Just keep going, it is the best we can do.
        return lifecycled.destroy()
            .thenRun(() ->
            {
              // NOTE: This is safe because it is single file, regardless of which thread the
              // future completes on.
              errors.add(null);
            })
            .onFailure((t) ->
            {
              log.error("[{}]: Failed to destroy a managed resource, continuing anyway.", id);
              // NOTE: This is safe because it is single file, regardless of which thread the
              // future completes on.
              errors.add(t);
            });
      });
    }

    return future
        .alwaysCompose((voyd, t) ->
        {
          // NOTE: t from the last destroy call is already in the list so no need to add it here.

          if (errors.stream()
              .filter((e) -> e != null)
              .findAny()
              .isPresent())
          {
            return Pnky.immediatelyFailed(new CombinedException(
                String.format(
                    "[{}]: Component failure during shutdown.", id),
                errors));
          }
          else
          {
            return Pnky.immediatelyComplete();
          }
        });
  }

  private PnkyPromise<Void> initConcurrent()
  {
    log.debug("[{}]: Initializing concurrently.", id);

    final List<PnkyPromise<Void>> initFutures = new ArrayList<>(managedLifecycleds.size());

    managedLifecycleds.forEach((lifecycled) -> initFutures.add(lifecycled.init()));

    final PnkyPromise<List<Void>> initFuture = Pnky.all(initFutures);

    return initFuture
        .thenTransform(ExceptionalFunction.toNull());
  }

  private PnkyPromise<Void> initConsecutive()
  {
    log.debug("[{}]: Initializing consecutively.", id);

    PnkyPromise<Void> future = Pnky.immediatelyComplete(null);

    for (final Lifecycled lifecycled : managedLifecycleds)
    {
      future = future.thenCompose((voyd) ->
      {
        return lifecycled.init();
      });
    }

    return future;
  }

  public static class LifecycledManagerBuilder
  {
    private InitMode initMode = InitMode.CONCURRENT;
  }
}
