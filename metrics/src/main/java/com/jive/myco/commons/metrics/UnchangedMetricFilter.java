package com.jive.myco.commons.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.Timer;

/**
 * A metric filter that filters out metrics with values that have not changed since the last time
 * the metric was encountered.
 *
 * @author David Valeri
 */
@RequiredArgsConstructor
public class UnchangedMetricFilter implements MetricFilter, MetricRegistryListener
{
  private static final long ONE_MINUTE = 60000L;

  private final Map<String, ValueHolder> lastValueCache = new ConcurrentHashMap<>();

  /**
   * Flag indicating if counters with unchanged values are transmitted with each report or if they
   * are filtered out of the report until they change again. Defaults to {@code false}.
   */
  private final boolean graphiteReporterFilterUnchangedCounters;

  /**
   * Flag indicating if gauges with unchanged values are transmitted with each report or if they are
   * filtered out of the report until they change again. Defaults to {@code false}.
   */
  private final boolean graphiteReporterFilterUnchangedGauges;

  @Override
  public boolean matches(final String name, final Metric metric)
  {
    if (graphiteReporterFilterUnchangedGauges && metric instanceof Gauge)
    {
      final Object newValue = ((Gauge<?>) metric).getValue();
      final ValueHolder lastValueHolder = lastValueCache.get(name);

      return matches(name, newValue, lastValueHolder);
    }
    else if (graphiteReporterFilterUnchangedCounters && metric instanceof Counter)
    {
      final Long newValue = ((Counter) metric).getCount();
      final ValueHolder lastValueHolder = lastValueCache.get(name);

      return matches(name, newValue, lastValueHolder);
    }
    // Not applicable, not our concern.
    else
    {
      return true;
    }
  }

  private boolean matches(final String name, final Object newValue, final ValueHolder lastValueHolder)
  {
    final long currentTimeMillis = System.currentTimeMillis();

    // Never seen it before or new value since last time it was encountered or it has
    // been over a minute since we reported it.
    if (lastValueHolder == null || lastValueHolder.getLastValue() == null
        || !lastValueHolder.getLastValue().equals(newValue)
        || lastValueHolder.getLastReported() - currentTimeMillis > ONE_MINUTE)
    {

      lastValueCache.put(name, new ValueHolder(newValue, currentTimeMillis));
      return true;
    }
    // Same value as the last encounter, skip it.
    else
    {
      return false;
    }
  }

  @Override
  public void onGaugeAdded(final String name, final Gauge<?> gauge)
  {
    // No-op
  }

  /**
   * Remove the metric from the last value cache.
   *
   * @param name
   *          the name of the removed metric
   */
  @Override
  public void onGaugeRemoved(final String name)
  {
    lastValueCache.remove(name);
  }

  @Override
  public void onCounterAdded(final String name, final Counter counter)
  {
    // No-op
  }

  /**
   * Remove the metric from the last value cache.
   *
   * @param name
   *          the name of the removed metric
   */
  @Override
  public void onCounterRemoved(final String name)
  {
    lastValueCache.remove(name);
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

  @RequiredArgsConstructor
  @Getter
  private static final class ValueHolder
  {
    private final Object lastValue;
    private final long lastReported;
  }
}
