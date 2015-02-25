package com.jive.myco.commons.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

import com.google.common.util.concurrent.MoreExecutors;

/**
 * Implements an executor that executes the provided runnable immediately in the same thread it is
 * called from. This is similar to Guava's {@link MoreExecutors#sameThreadExecutor()} except that
 * there is only ever one instance of an {@link ImmediateExecutor} instead of creating a new
 * instance every time the method is called. There is also no locking involved at all within this
 * instance and it does not implement an {@link ExecutorService}, just an {@link Executor}.
 *
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public enum ImmediateExecutor implements Executor
{
  INSTANCE;

  @Override
  public void execute(@Nonnull final Runnable runnable)
  {
    runnable.run();
  }

  /**
   * Get at the singleton {@link ImmediateExecutor} instance in a more natural fashion.
   */
  public static ImmediateExecutor getInstance()
  {
    return INSTANCE;
  }
}
