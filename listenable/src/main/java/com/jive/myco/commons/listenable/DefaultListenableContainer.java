package com.jive.myco.commons.listenable;

import java.util.Map;
import java.util.concurrent.Executor;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import com.google.common.base.Function;
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
  public void addListener(T listener)
  {
    addListener(listener, MoreExecutors.sameThreadExecutor());
  }

  @Override
  public void addListener(@NonNull T listener, @NonNull Executor executor)
  {
    listeners.put(listener, executor);
  }

  @Override
  public void removeListener(T listener)
  {
    listeners.remove(listener);
  }

  @Override
  public void forEach(final Function<T, Void> action)
  {
    for (final Map.Entry<T, Executor> entry : listeners.entrySet())
    {
      final T listener = entry.getKey();
      entry.getValue().execute(new Runnable()
      {
        @Override
        public void run()
        {
          try
          {
            action.apply(listener);
          }
          catch (Exception e)
          {
            log.error("Listener [{}] had an error", listener, e);
          }
        }
      });
    }
  }

  @Override
  public void clear()
  {
    listeners.clear();
  }
}
