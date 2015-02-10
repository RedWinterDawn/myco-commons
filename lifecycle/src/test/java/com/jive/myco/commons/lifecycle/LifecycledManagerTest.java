package com.jive.myco.commons.lifecycle;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import lombok.NonNull;
import lombok.experimental.Builder;

import org.fusesource.hawtdispatch.Dispatch;
import org.junit.Test;

import com.jive.myco.commons.concurrent.CombinedException;
import com.jive.myco.commons.concurrent.Pnky;
import com.jive.myco.commons.concurrent.PnkyPromise;

/**
 * Tests for {@link LifecycledManager}.
 *
 * @author David Valeri
 */
public class LifecycledManagerTest
{
  @Test
  public void testShutdownFailure() throws Exception
  {
    final LifecycledTestHarness lifecycled_0 =
        LifecycledTestHarness.builder()
            .id("0")
            .build();

    final LifecycledTestHarness lifecycled_1 =
        LifecycledTestHarness.builder()
            .id("1")
            .destroyFuture(
                (Pnky<Void>) Pnky.<Void> immediatelyFailed(new RuntimeException("exception 1")))
            .build();

    final LifecycledTestHarness lifecycled_2 =
        LifecycledTestHarness.builder()
            .id("2")
            .destroyFuture(
                (Pnky<Void>) Pnky.<Void> immediatelyFailed(new RuntimeException("exception 2")))
            .build();

    final LifecycledManager manager = LifecycledManager.builder()
        .dispatchQueue(Dispatch.createQueue())
        .managedLifecycleds(Arrays.asList(
            lifecycled_0,
            lifecycled_1,
            lifecycled_2))
        .build();

    manager.init().get();

    try
    {
      manager.destroy().get();
      fail();
    }
    catch (final ExecutionException e)
    {
      assertThat(e.getCause(), instanceOf(CombinedException.class));

      final CombinedException cause = (CombinedException) e.getCause();
      final List<? extends Throwable> causes = cause.getCauses();

      assertThat(causes.get(0), hasProperty("message", equalTo("exception 2")));
      assertThat(causes.get(1), hasProperty("message", equalTo("exception 1")));
      assertThat(causes.get(2), nullValue());
    }
  }

  private static final class LifecycledTestHarness extends AbstractLifecycled
  {
    private final Pnky<Void> initFuture;
    private final Pnky<Void> destroyFuture;

    @Builder
    private LifecycledTestHarness(
        @NonNull final String id,
        @NonNull final Pnky<Void> initFuture,
        @NonNull final Pnky<Void> destroyFuture)
    {
      super(Dispatch.createQueue(id));
      this.initFuture = initFuture;
      this.destroyFuture = destroyFuture;
    }

    @Override
    protected final PnkyPromise<Void> initInternal()
    {
      return initFuture;
    }

    @Override
    protected final PnkyPromise<Void> destroyInternal()
    {
      return destroyFuture;
    }

    public static final class LifecycledTestHarnessBuilder
    {
      private static final Pnky<Void> COMPLETE_FUTURE = Pnky.create();

      static
      {
        COMPLETE_FUTURE.resolve(null);
      }

      private Pnky<Void> initFuture = COMPLETE_FUTURE;
      private Pnky<Void> destroyFuture = COMPLETE_FUTURE;
    }
  }
}
