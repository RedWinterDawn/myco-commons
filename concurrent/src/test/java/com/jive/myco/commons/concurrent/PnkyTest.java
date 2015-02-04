package com.jive.myco.commons.concurrent;

import static com.jayway.awaitility.Awaitility.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Cleanup;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
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
    final AtomicInteger badThings = new AtomicInteger();

    // Only on this test just to verify same thread behavior
    final AtomicBoolean invoked = new AtomicBoolean();

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
    final AtomicInteger badThings = new AtomicInteger();

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
    final AtomicInteger badThings = new AtomicInteger();

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
    final AtomicInteger badThings = new AtomicInteger();

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
    final AtomicInteger badThings = new AtomicInteger();

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

  @Test
  public void testAllWaitsForAll() throws Exception
  {
    final Pnky<Integer> toFinish = Pnky.create();
    final List<PnkyPromise<Integer>> promises = Arrays.asList(
        Pnky.<Integer> immediatelyFailed(new NumberFormatException()), toFinish);

    final AtomicReference<Throwable> throwable = new AtomicReference<>();
    Pnky.all(promises).alwaysAccept((result, error) ->
    {
      assertNull(result);
      throwable.set(error);
    });

    assertNull(throwable.get());

    toFinish.resolve(1);

    assertThat(throwable.get(), instanceOf(CombinedException.class));
    assertThat(((CombinedException) throwable.get()).getCauses().get(0),
        instanceOf(NumberFormatException.class));
  }

  @Test
  public void testAllWithEmptySetOfPromises() throws Exception
  {
    final CountDownLatch invoked = new CountDownLatch(1);
    Pnky.all(Lists.newArrayList()).alwaysAccept((result, error) -> invoked.countDown());

    assertTrue(invoked.await(10, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testAllInOrder() throws Exception
  {
    final Pnky<Integer> first = Pnky.create();
    final Pnky<Integer> second = Pnky.create();
    final CountDownLatch finished = new CountDownLatch(1);
    final AtomicReference<List<Integer>> results = new AtomicReference<>();
    Pnky.all(Arrays.asList(first, second))
        .thenAccept((result) ->
        {
          results.set(result);
          finished.countDown();
        });

    second.resolve(2);

    assertNull(results.get());

    first.resolve(1);

    assertTrue(finished.await(10, TimeUnit.MILLISECONDS));

    assertNotNull(results.get());
    assertEquals(2, results.get().size());
    assertEquals(1, (int) results.get().get(0));
    assertEquals(2, (int) results.get().get(1));
  }

  @Test
  public void testAllFailingFast() throws Exception
  {
    final AtomicReference<Throwable> result = new AtomicReference<>();
    Pnky.allFailingFast(
        Arrays.asList(Pnky.immediatelyFailed(new NumberFormatException()), Pnky.create()))
        .onFailure(result::set);

    assertThat(result.get(), instanceOf(NumberFormatException.class));
  }

  @Test
  public void testWrapListenableFutureSuccess() throws Exception
  {
    @Cleanup("shutdownNow")
    final
    ListeningExecutorService executorService =
        MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

    final AtomicBoolean started = new AtomicBoolean();
    final CountDownLatch latch = new CountDownLatch(1);

    final ListenableFuture<Boolean> future = executorService.submit(() ->
    {
      started.set(true);
      return latch.await(5, TimeUnit.SECONDS);
    });

    final PnkyPromise<Boolean> pnky = Pnky.from(future);

    assertFalse(pnky.isDone());

    await().untilTrue(started);

    latch.countDown();

    await().until(pnky::isDone);
    assertTrue(pnky.get());
  }

  @Test
  public void testWrapListenableFutureFailure() throws Exception
  {
    @Cleanup("shutdownNow")
    final
    ListeningExecutorService executorService =
        MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

    final AtomicBoolean started = new AtomicBoolean();
    final CountDownLatch latch = new CountDownLatch(1);

    final ListenableFuture<Boolean> future = executorService.submit((Callable<Boolean>) () ->
    {
      started.set(true);
      if (latch.await(5, TimeUnit.SECONDS))
      {
        throw new NumberFormatException();
      }
      else
      {
        throw new Exception();
      }
    });

    final PnkyPromise<Boolean> pnky = Pnky.from(future);

    assertFalse(pnky.isDone());

    await().untilTrue(started);

    latch.countDown();

    await().until(pnky::isDone);

    try
    {
      pnky.get(5, TimeUnit.SECONDS);
      fail();
    }
    catch (final ExecutionException e)
    {
      assertThat(e.getCause(), instanceOf(NumberFormatException.class));
    }
  }

  @Test
  public void testAlwaysRunSuccess() throws Exception
  {
    final AtomicInteger badThings = new AtomicInteger();
    final AtomicBoolean invoked = new AtomicBoolean();
    final AtomicBoolean invokedDownstream = new AtomicBoolean();

    Pnky.supplyAsync(() -> 1, MoreExecutors.sameThreadExecutor())
        .alwaysRun(() -> invoked.set(true))
        .onFailure((error) -> badThings.incrementAndGet())
        .thenAccept((result) ->
        {
          assertEquals(1, (int) result);
          invokedDownstream.set(true);
        });

    assertTrue(invoked.get());
    assertTrue(invokedDownstream.get());
    assertEquals(0, badThings.get());
  }

  @Test
  public void testAlwaysRunFailure() throws Exception
  {
    final AtomicInteger badThings = new AtomicInteger();
    final AtomicBoolean invoked = new AtomicBoolean();
    final AtomicBoolean invokedDownstream = new AtomicBoolean();

    Pnky
        .composeAsync(
            () -> Pnky.immediatelyFailed(new RuntimeException("Initial exception")),
            MoreExecutors.sameThreadExecutor())
        .alwaysRun(() -> invoked.set(true))
        .thenAccept((result) -> badThings.incrementAndGet())
        .onFailure((error) ->
        {
          invokedDownstream.set(true);
          assertEquals("Initial exception", error.getMessage());
        });

    assertTrue(invoked.get());
    assertTrue(invokedDownstream.get());
    assertEquals(0, badThings.get());
  }

  @Test
  public void testAlwaysRunFailureReplacesSuccess() throws Exception
  {
    final AtomicInteger badThings = new AtomicInteger();
    final AtomicBoolean invoked = new AtomicBoolean();
    final AtomicBoolean invokedDownstream = new AtomicBoolean();

    Pnky.supplyAsync(() -> 1, MoreExecutors.sameThreadExecutor())
        .alwaysRun(() ->
        {
          invoked.set(true);
          throw new RuntimeException("exception");
        })
        .onFailure((error) ->
        {
          invokedDownstream.set(true);
          assertEquals("exception", error.getMessage());
        })
        .thenAccept((result) -> badThings.incrementAndGet());

    assertTrue(invoked.get());
    assertTrue(invokedDownstream.get());
    assertEquals(0, badThings.get());
  }

  @Test
  public void testAlwaysRunFailureReplacesFailure() throws Exception
  {
    final AtomicInteger badThings = new AtomicInteger();
    final AtomicBoolean invoked = new AtomicBoolean();
    final AtomicBoolean invokedDownstream = new AtomicBoolean();

    Pnky
        .composeAsync(
            () -> Pnky.immediatelyFailed(new RuntimeException("Initial exception")),
            MoreExecutors.sameThreadExecutor())
        .alwaysRun(() ->
        {
          invoked.set(true);
          throw new RuntimeException("Second exception");
        })
        .thenAccept((result) -> badThings.incrementAndGet())
        .onFailure((error) ->
        {
          invokedDownstream.set(true);
          assertEquals("Second exception", error.getMessage());
        });

    assertTrue(invoked.get());
    assertTrue(invokedDownstream.get());
    assertEquals(0, badThings.get());
  }

  @Test
  public void testThenRunSuccess() throws Exception
  {
    final AtomicInteger badThings = new AtomicInteger();
    final AtomicBoolean invoked = new AtomicBoolean();
    final AtomicBoolean invokedDownstream = new AtomicBoolean();

    Pnky.supplyAsync(() -> 1, MoreExecutors.sameThreadExecutor())
        .thenRun(() -> invoked.set(true))
        .onFailure((error) -> badThings.incrementAndGet())
        .thenAccept((result) ->
        {
          assertEquals(1, (int) result);
          invokedDownstream.set(true);
        });

    assertTrue(invoked.get());
    assertTrue(invokedDownstream.get());
    assertEquals(0, badThings.get());
  }

  @Test
  public void testThenRunFailure() throws Exception
  {
    final AtomicInteger badThings = new AtomicInteger();
    final AtomicBoolean invokedDownstream = new AtomicBoolean();

    Pnky
        .composeAsync(
            () -> Pnky.immediatelyFailed(new RuntimeException("Initial exception")),
            MoreExecutors.sameThreadExecutor())
        .thenRun(() -> badThings.incrementAndGet())
        .onFailure((error) ->
        {
          invokedDownstream.set(true);
          assertEquals("Initial exception", error.getMessage());
        });

    assertTrue(invokedDownstream.get());
    assertEquals(0, badThings.get());
  }

  @Test
  public void testThenRunFailureReplacesSuccess() throws Exception
  {
    final AtomicInteger badThings = new AtomicInteger();
    final AtomicBoolean invoked = new AtomicBoolean();
    final AtomicBoolean invokedDownstream = new AtomicBoolean();

    Pnky.supplyAsync(() -> 1, MoreExecutors.sameThreadExecutor())
        .thenRun(() ->
        {
          invoked.set(true);
          throw new RuntimeException("exception");
        })
        .onFailure((error) ->
        {
          invokedDownstream.set(true);
          assertEquals("exception", error.getMessage());
        })
        .thenAccept((result) -> badThings.incrementAndGet());

    assertTrue(invoked.get());
    assertTrue(invokedDownstream.get());
    assertEquals(0, badThings.get());
  }
}
