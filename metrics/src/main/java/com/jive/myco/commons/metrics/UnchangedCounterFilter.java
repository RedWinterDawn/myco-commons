package com.jive.myco.commons.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.Timer;

/**
 * A metric filter that filters out {@link Counter} metrics with values that have not changed since
 * the last time the metric was encountered.
 *
 * @author David Valeri
 */
public class UnchangedCounterFilter implements MetricFilter, MetricRegistryListener
{

  private final Map<String, Long> counterLastCountCache = new ConcurrentHashMap<>();

  @Override
  public boolean matches(final String name, final Metric metric)
  {
    if (metric instanceof Counter)
    {
      final Long newValue = ((Counter) metric).getCount();
      final Long lastValue = counterLastCountCache.get(name);

      // Never seen it before or new value since last time it was encountered
      if (lastValue == null || lastValue != newValue)
      {
        counterLastCountCache.put(name, newValue);
        return true;
      }
      // Same value as the last encounter, skip it.
      else
      {
        return false;
      }
    }
    // Not a counter, not our concern.
    else
    {
      return true;
    }
  }

  @Override
  public void onGaugeAdded(final String name, final Gauge<?> gauge)
  {
    // No-op
  }

  @Override
  public void onGaugeRemoved(final String name)
  {
    // No-op
  }

  @Override
  public void onCounterAdded(final String name, final Counter counter)
  {
    // No-op
  }

  /**
   * Remove the metric from the counter last value cache.
   *
   * @param name
   *          the name of the removed metric
   */
  @Override
  public void onCounterRemoved(final String name)
  {
    counterLastCountCache.remove(name);
  }

  @Override
  public void onHistogramAdded(final String name, final Histogram histogram)
  {
    // No-op

  }

  @Override
  public void onHistogramRemoved(final String name)
  {
    // No-op
  }

  @Override
  public void onMeterAdded(final String name, final Meter meter)
  {
    // No-op
  }

  @Override
  public void onMeterRemoved(final String name)
  {
    // No-op
  }

  @Override
  public void onTimerAdded(final String name, final Timer timer)
  {
    // No-op
  }

  @Override
  public void onTimerRemoved(final String name)
  {
    // No-op
  }
}
