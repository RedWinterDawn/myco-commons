package com.jive.myco.commons.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

/**
 * Wrapper for an ExecutorService that execute the provided UncaughtExceptionHandler in any
 * exceptional case.
 * @author Binh Tran
 */
@RequiredArgsConstructor
public class CommonExecutorService implements ExecutorService
{

  private final ExecutorService delegate;
  protected final Thread.UncaughtExceptionHandler handler;

  public static ExecutorService wrap(
      ExecutorService executor, Thread.UncaughtExceptionHandler handler)
  {
    return new CommonExecutorService(executor, handler);
  }

  @Override
  public void shutdown()
  {
    this.delegate.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow()
  {
    return this.delegate.shutdownNow();
  }

  @Override
  public boolean isShutdown()
  {
    return this.delegate.isShutdown();
  }

  @Override
  public boolean isTerminated()
  {
    return this.delegate.isTerminated();
  }

  @Override
  public boolean awaitTermination(final long timeout, final TimeUnit unit)
      throws InterruptedException
  {
    return this.delegate.awaitTermination(timeout, unit);
  }

  @Override
  public <T> Future<T> submit(final Callable<T> task)
  {
    return this.delegate.submit(
        () -> {
          try
          {
            return  task.call();
          }
          catch (Throwable e)
          {
            handler.uncaughtException(Thread.currentThread(), e);
            throw e;
          }
        });
  }

  @Override
  public <T> Future<T> submit(final Runnable task, final T result)
  {
    return this.delegate.submit(() ->
    {
      try
      {
        task.run();
      }
      catch (Throwable e)
      {
        handler.uncaughtException(Thread.currentThread(), e);
      }
    }, result);
  }

  @Override
  public Future<?> submit(final Runnable task)
  {
    return delegate.submit(() ->
    {
      try
      {
        task.run();
      }
      catch (Throwable e)
      {
        handler.uncaughtException(Thread.currentThread(), e);
      }
    });
  }

  private <T> Collection<? extends Callable<T>> wrapCallableCollection(
      Collection<? extends Callable<T>> tasks)
  {
    return tasks.stream()
        .map(new Function<Callable<T>, Callable<T>>() {
          @Override
          public Callable<T> apply(final Callable<T> c)
          {
            return () -> {
              try
              {
                return c.call();
              }
              catch (Throwable e)
              {
                handler.uncaughtException(Thread.currentThread(), e);
                throw e;
              }
            };
          }
        }).collect(Collectors.toList());
  }

  @Override
  public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks)
      throws InterruptedException
  {
    return this.delegate.invokeAll(wrapCallableCollection(tasks));
  }

  @Override
  public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks,
      final long timeout,
      final TimeUnit unit) throws InterruptedException
  {
    return this.delegate.invokeAll(wrapCallableCollection(tasks), timeout, unit);
  }

  @Override
  public <T> T invokeAny(final Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException
  {
    return this.delegate.invokeAny(wrapCallableCollection(tasks));
  }

  @Override
  public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout,
      final TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException
  {
    return this.delegate.invokeAny(tasks, timeout, unit);
  }

  @Override
  public void execute(final Runnable command)
  {

  }
}
