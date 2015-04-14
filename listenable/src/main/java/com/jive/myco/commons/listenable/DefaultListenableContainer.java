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

  @Override
  public void addListenerWithInitialAction(final T listener, final Consumer<? super T> action)
  {
    addListenerWithInitialAction(listener, MoreExecutors.directExecutor(), action);
  }

  @Override
  public void addListener(@NonNull final T listener, @NonNull final Executor executor)
  {
    listeners.put(listener, executor);
  }

  @Override
  public void addListenerWithInitialAction(final T listener, final Executor executor,
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
