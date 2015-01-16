package com.jive.myco.commons.hawtdispatch;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.AccessLevel;
import lombok.Setter;

import com.google.common.util.concurrent.AbstractFuture;

class ScheduledSettableFuture<V> extends AbstractFuture<V> implements ScheduledFuture<V>
{

  @Setter(AccessLevel.PACKAGE)
  private Instant executionTime;

  public ScheduledSettableFuture(long delay, TimeUnit unit)
  {
    this.executionTime = Instant.now().plusMillis(TimeUnit.MILLISECONDS.convert(delay, unit));
  }

  @Override
  public long getDelay(TimeUnit unit)
  {
    return unit.convert(Instant.now().until(executionTime, ChronoUnit.MILLIS),
        TimeUnit.MILLISECONDS);
  }

  @Override
  public int compareTo(Delayed o)
  {
    return Long.compare(getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
  }
  
  @Override
  public boolean set(V value)
  {
    return super.set(value);
  }
  
  @Override
  public boolean setException(Throwable ex)
  {
    return super.setException(ex);
  }

  public static <V> ScheduledSettableFuture<V> create(long delay, TimeUnit unit)
  {
    return new ScheduledSettableFuture<>(delay, unit);
  }

}
