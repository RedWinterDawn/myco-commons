package com.jive.myco.commons.concurrent;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.MoreExecutors;

/**
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public class PnkyTest
{
  private ExecutorService executor;

  @Before
  public void setup()
  {
    // TODO should use a test executor here that can validate that the executor was used.

    executor = Executors.newSingleThreadExecutor();

  }

  @After
  public void teadown()
  {
    executor.shutdownNow();
  }

  @Test
  public void testAlwaysPropagateBiConsumerSuccess() throws Exception
  {
    AtomicInteger badThings = new AtomicInteger();

    // Only on this test just to verify same thread behavior
    AtomicBoolean invoked = new AtomicBoolean();

    Pnky.supplyAsync(() -> 1, MoreExecutors.sameThreadExecutor())
        .alwaysAccept((result, error) ->
        {
          assertEquals(1, (int) result);
          assertNull(error);
        })
        .onFailure((error) -> badThings.incrementAndGet())
        .thenAccept((result) -> assertEquals(1, (int) result))
        .thenAccept((result) -> invoked.set(true));

    assertTrue(invoked.get());
    assertEquals(0, badThings.get());
  }

  @Test
  public void testAlwaysPropagateError() throws Exception
  {
    AtomicInteger badThings = new AtomicInteger();

    Pnky
        .supplyAsync(() ->
        {
          throw new NumberFormatException();
        }, MoreExecutors.sameThreadExecutor())
        .thenAccept((result) -> badThings.incrementAndGet())
        .alwaysAccept((result, error) ->
        {
          assertNull(result);
          assertThat(error, instanceOf(NumberFormatException.class));
        })
        .thenAccept((result) -> badThings.incrementAndGet());

    assertEquals(0, badThings.get());
  }

  @Test
  public void testAlwaysTransformSuccess() throws Exception
  {
    Pnky.supplyAsync(() -> 1, MoreExecutors.sameThreadExecutor())
        .alwaysTransform((result, error) ->
        {
          assertEquals(1, (int) result);
          assertNull(error);
          return result + 1;
        })
        .alwaysAccept((result, error) ->
        {
          assertEquals(2, (int) result);
          assertNull(error);
        });
  }

  @Test
  public void testAlwaysTransformFailure() throws Exception
  {
    AtomicInteger badThings = new AtomicInteger();

    Pnky
        .supplyAsync(() ->
        {
          throw new NumberFormatException();
        }, MoreExecutors.sameThreadExecutor())
        .alwaysTransform((result, error) ->
        {
          throw new IllegalStateException();
        })
        .thenAccept((result) -> badThings.incrementAndGet())
        .alwaysAccept((result, error) ->
        {
          assertNull(result);
          assertThat(error, instanceOf(IllegalStateException.class));
        })
        .thenAccept((result) -> badThings.incrementAndGet())
        .alwaysTransform((result, error) -> 2)
        .alwaysAccept((result, error) ->
        {
          assertNull(error);
          assertEquals(2, (int) result);
        });

    assertEquals(0, badThings.get());
  }

  @Test
  public void testAlwaysComposeOnSuccess() throws Exception
  {
    Pnky.supplyAsync(() -> 1, MoreExecutors.sameThreadExecutor())
        .alwaysCompose((result, error) ->
        {
          assertEquals(1, (int) result);
          assertNull(error);
          return Pnky.immediatelyComplete(result + 1);
        })
        .alwaysAccept((result, error) ->
        {
          assertEquals(2, (int) result);
          assertNull(error);
        });
  }

  @Test
  public void testAlwaysComposeFailure() throws Exception
  {
    AtomicInteger badThings = new AtomicInteger();

    Pnky
        .supplyAsync(() ->
        {
          throw new NumberFormatException();
        }, MoreExecutors.sameThreadExecutor())
        .alwaysCompose((result, error) ->
        {
          throw new IllegalStateException();
        })
        .thenAccept((result) -> badThings.incrementAndGet())
        .alwaysAccept((result, error) ->
        {
          assertNull(result);
          assertThat(error, instanceOf(IllegalStateException.class));
        })
        .thenAccept((result) -> badThings.incrementAndGet())
        .alwaysCompose((result, error) -> Pnky.immediatelyComplete(2))
        .alwaysAccept((result, error) ->
        {
          assertNull(error);
          assertEquals(2, (int) result);
        })
        .alwaysCompose((result, error) -> Pnky.immediatelyFailed(
            new IllegalArgumentException()))
        .alwaysAccept((result, error) ->
        {
          assertNull(result);
          assertThat(error, instanceOf(IllegalArgumentException.class));
        });

    assertEquals(0, badThings.get());
  }

  @Test
  public void testPropagateIndividually() throws Exception
  {
    AtomicInteger badThings = new AtomicInteger();

    Pnky
        .supplyAsync(() ->
        {
          throw new NumberFormatException();
        }, MoreExecutors.sameThreadExecutor())
        .thenAccept((result) -> badThings.incrementAndGet())
        .onFailure((error) -> assertThat(error, instanceOf(NumberFormatException.class)))
        .withFallback((error) -> 1)
        .thenAccept((result) -> assertEquals(1, (int) result))
        .onFailure((error) -> badThings.incrementAndGet())
        .thenAccept((result) ->
        {
          throw new IllegalStateException();
        })
        .withFallback((error) ->
        {
          assertThat(error, instanceOf(IllegalStateException.class));
          return 5;
        })
        .thenAccept((result) -> assertEquals(5, result))
        .onFailure((error) -> badThings.incrementAndGet());

    assertEquals(0, badThings.get());
  }

  @Test
  public void testAcceptExceptionally() throws Exception
  {
    final PnkyPromise<Integer> future = Pnky.immediatelyComplete(-1);

    final AtomicInteger value = new AtomicInteger();

    future.thenAccept(value::set).get();

    assertEquals(-1, value.get());

    value.set(0);

    future.thenAccept(value::set).get();

    assertEquals(-1, value.get());

    value.set(0);

    future.thenAccept(value::set, executor).get();

    assertEquals(-1, value.get());

    value.set(0);

    try
    {
      future
          .thenAccept((result) ->
          {
            throw new TestException();
          })
          .get();

      fail();
    }
    catch (final ExecutionException e)
    {
      assertEquals(TestException.class, e.getCause().getClass());
    }

    try
    {
      future
          .thenAccept((result) ->
          {
            throw new TestException();
          }, ForkJoinPool.commonPool())
          .get();

      fail();
    }
    catch (final ExecutionException e)
    {
      assertEquals(TestException.class, e.getCause().getClass());
    }

    try
    {
      future
          .thenAccept((result) ->
          {
            throw new TestException();
          }, executor)
          .get();

      fail();
    }
    catch (final ExecutionException e)
    {
      assertEquals(TestException.class, e.getCause().getClass());
    }
  }

  @Test
  public void testAccept() throws Exception
  {
    final PnkyPromise<Integer> future = Pnky.immediatelyComplete(-1);

    final AtomicInteger value = new AtomicInteger();

    future
        .thenAccept(value::set)
        .get();

    assertEquals(-1, value.get());

    value.set(0);

    future
        .thenAccept(value::set)
        .get();

    assertEquals(-1, value.get());

    value.set(0);

    future
        .thenAccept(value::set, executor)
        .get();

    assertEquals(-1, value.get());
  }

}
