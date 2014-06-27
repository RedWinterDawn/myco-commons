package com.jive.myco.commons.listenable;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
@Slf4j
public class DefaultListenableContainer<T> implements ListenableContainer<T>
{
  private final Map<T, Executor> listeners = Maps.newConcurrentMap();

  /**
   * Adds a new listener. The listener is invoked on the calling thread of
   * {@link #forEach(Consumer)}. Calling this method with a listener that has already been added
   * will update the listener's associated executor.
   *
   * @param listener
   *          the listener to add
   */
  @Override
  public void addListener(final T listener)
  {
    addListener(listener, MoreExecutors.sameThreadExecutor());
  }

  /**
   * Adds a new listener, passing the listener to {@code action} after successfully adding it on the
   * calling thread. The listener is invoked on the calling thread of {@link #forEach(Consumer)}.
   * Calling this method with a listener that has already been added will update the listener's
   * associated executor but will not invoke the provided action.
   *
   * @param listener
   *          the listener to add
   * @param action
   *          the action to perform on the listener
   */
  public void addListener(final T listener, final Consumer<? super T> action)
  {
    addListener(listener, MoreExecutors.sameThreadExecutor(), action);
  }

  /**
   * Adds a new listener. The listener is invoked on the supplied {@code executor} when triggered
   * via {@link #forEach(Consumer)}. Calling this method with a listener that has already been added
   * will update the listener's associated executor.
   *
   * @param listener
   *          the listener to add
   * @param executor
   *          executor to run the listener in
   */
  @Override
  public void addListener(@NonNull final T listener, @NonNull final Executor executor)
  {
    listeners.put(listener, executor);
  }

  /**
   * Adds a new listener, passing the listener to {@code action} after successfully adding it on the
   * supplied {@code executor}. The listener is invoked on the supplied {@code executor} when
   * triggered via {@link #forEach(Consumer)}. Calling this method with a listener that has already
   * been added will update the listener's associated executor but will not invoke the provided
   * action.
   *
   * @param listener
   *          the listener to add
   * @param executor
   *          executor to run the listener in
   */
  public void addListener(final T listener, final Executor executor,
      final Consumer<? super T> action)
  {
    if (listeners.put(listener, executor) == null)
    {
      doAction(listener, executor, action);
    }
  }

  @Override
  public void removeListener(final Object listener)
  {
    listeners.remove(listener);
  }

  @Override
  public void forEach(final Consumer<? super T> action)
  {
    for (final Map.Entry<T, Executor> entry : listeners.entrySet())
    {
      final T listener = entry.getKey();
      doAction(listener, entry.getValue(), action);
    }
  }

  @Override
  public void clear()
  {
    listeners.clear();
  }

  private void doAction(final T listener, final Executor executor, final Consumer<? super T> action)
  {
    executor.execute(() ->
    {
      try
      {
        action.accept(listener);
      }
      catch (final Exception e)
      {
        log.error("Listener [{}] had an error", listener, e);
      }
    });
  }
}
