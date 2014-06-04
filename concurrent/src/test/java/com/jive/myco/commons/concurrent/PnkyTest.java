package com.jive.myco.commons.concurrent;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.google.common.util.concurrent.MoreExecutors;

/**
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public class PnkyTest
{
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
          return Pnky.immediateFuture(result + 1);
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
        .alwaysCompose((result, error) -> Pnky.immediateFuture(2))
        .alwaysAccept((result, error) ->
        {
          assertNull(error);
          assertEquals(2, (int) result);
        })
        .alwaysCompose((result, error) -> Pnky.immediateFailedFuture(
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
}
