package com.jive.myco.commons.hawtdispatch;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.fusesource.hawtdispatch.DispatchQueue;

import com.google.common.collect.Lists;

/**
 * Provides an ability to use a Hawt Dispatch {@link DispatchQueue} in areas where an
 * {@link ExecutorService} is required.
 * <p>
 * The implementation does not guarantee 100% parity with the {@link ExecutorService} interface but
 * simply tries to enable usage of a {@link DispatchQueue} where 3rd parties require an
 * {@link ExecutorService}, especially when they do not actually require the use of an
 * {@link ExecutorService}.
 * <p>
 * <strong>NOTE:</strong> Be careful when providing this to external resources. Ensure that no
 * blocking action is performed within this executor service and you must also ensure that the
 * consumer does not rely on the behavior of any of the shutdown related operations.
 *
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
@RequiredArgsConstructor
@Slf4j
public class DispatchQueueExecutorService extends AbstractExecutorService implements
    ExecutorService
{
  private final DispatchQueue dispatchQueue;

  @Override
  public void shutdown()
  {
    // no-op
    log.info("Ignoring attempt to shutdown executor service with dispatch queue [{}]",
        dispatchQueue.getLabel());
  }

  @Override
  public List<Runnable> shutdownNow()
  {
    return Lists.newArrayList();
  }

  @Override
  public boolean isShutdown()
  {
    return false;
  }

  @Override
  public boolean isTerminated()
  {
    return false;
  }

  @Override
  public boolean awaitTermination(final long timeout, final TimeUnit unit)
      throws InterruptedException
  {
    return false;
  }

  @Override
  public void execute(final Runnable command)
  {
    dispatchQueue.execute(command);
  }
}
