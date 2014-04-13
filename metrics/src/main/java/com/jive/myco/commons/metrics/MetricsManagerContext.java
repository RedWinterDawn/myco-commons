package com.jive.myco.commons.metrics;

import java.util.concurrent.Callable;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.RatioGauge;
import com.codahale.metrics.RatioGauge.Ratio;
import com.codahale.metrics.Timer;

/**
 * A context for creating metrics. All metrics created via this context contain the same name
 * prefix. Sub-contexts may be derived via {@link #segment(String, String...)}.
 *
 * @author David Valeri
 */
public interface MetricsManagerContext
{
  /**
   * Returns the base name of metrics created via this context. May be {@code null}.
   */
  String getBaseName();

  /**
   * Returns the metrics manager from which this context was derived.
   */
  MetricsManager getMetricsManager();
  
  /**
   * Returns a new context with {@code segment} and {@code additionalSegments} prefixed to all
   * metrics created from the returned context.
   *
   * @param segment
   *          the segment to add to the metric names for all metrics created by the new context
   * @param additionalSegments
   *          more optional segments to add to all metric names created by the new context
   */
  MetricsManagerContext segment(final String segment, final String... additionalSegments);

  /**
   * Removes a metric from the registry.
   *
   * @param metric
   *          the metric to remove from the registry
   */
  void removeMetric(final Metric metric);

  /**
   * Creates / returns a counter for the provided name.
   * 
   * @param segment
   *          the segment to add to the metric name for the metric
   * @param additionalSegments
   *          more optional segments to add to the metric name used to identify the context of the
   *          metric
   * 
   * @return a new counter or the previously created counter if one already exists
   */
  Counter getCounter(final String segment, final String... additionalSegments);

  /**
   * Creates / returns a timer for the provided name.
   * 
   * @param segment
   *          the segment to add to the metric name for the metric
   * @param additionalSegments
   *          more optional segments to add to the metric name used to identify the context of the
   *          metric
   * 
   * @return a new timer or the previously created timer if one already exists
   */
  Timer getTimer(final String segment, final String... additionalSegments);

  /**
   * Creates / returns a meter for the provided name.
   * 
   * @param segment
   *          the segment to add to the metric name for the metric
   * @param additionalSegments
   *          more optional segments to add to the metric name used to identify the context of the
   *          metric
   * 
   * @return a new meter or the previously created meter if one already exists
   */
  Meter getMeter(final String segment, final String... additionalSegments);

  /**
   * Creates / returns a histogram for the provided name.
   * 
   * @param segment
   *          the segment to add to the metric name for the metric
   * @param additionalSegments
   *          more optional segments to add to the metric name used to identify the context of the
   *          metric
   * 
   * @return a new histogram or the previously created histogram if one already exists
   */
  Histogram getHistogram(final String segment, final String... additionalSegments);

  /**
   * Adds a ratio gauge for the provided name and function, removing the current gauge if one is
   * already registered.
   * 
   * @param function
   *          the function that calculates the ratio
   * @param segment
   *          the segment to add to the metric name for the metric
   * @param additionalSegments
   *          more optional segments to add to the metric name used to identify the context of the
   *          metric
   * 
   * @return a new ratio gauge or the previously created ratio gauge if one already exists
   * 
   * @throws IllegalArgumentException
   *           if the metric name is already registered in the registry
   */
  RatioGauge addRatio(final Callable<Ratio> function, final String segment, final String... additionalSegments);
}
