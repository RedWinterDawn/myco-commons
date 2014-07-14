package com.jive.myco.commons.metrics;

import static com.jive.myco.commons.concurrent.Pnky.*;

import java.io.Closeable;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.fusesource.hawtdispatch.internal.DispatcherConfig;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.RatioGauge;
import com.codahale.metrics.RatioGauge.Ratio;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.common.base.Joiner;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;
import com.jive.myco.commons.concurrent.Pnky;
import com.jive.myco.commons.concurrent.PnkyPromise;
import com.jive.myco.commons.hawtdispatch.DefaultDispatchQueueBuilder;
import com.jive.myco.commons.hawtdispatch.DispatchQueueBuilder;
import com.jive.myco.commons.lifecycle.AbstractLifecycled;
import com.jive.myco.commons.lifecycle.LifecycleStage;
import com.jive.myco.commons.lifecycle.Lifecycled;

/**
 * A manager for the Metrics library.
 *
 * @author David Valeri
 */
@Slf4j
public final class DefaultMetricsManager extends AbstractLifecycled implements MetricsManager,
    Lifecycled
{
  private static final AtomicInteger INSTANCE_COUNT = new AtomicInteger();

  private final List<Closeable> reporters = new LinkedList<>();

  /**
   * A set of {@link Metric}s still valid for this manager. Uses weak keys because there is a
   * concurrency risk between the registry's internal map and this map. The weak keys let things
   * that have actually been removed from the registry's internal map get removed from this map.
   */
  private final Set<Metric> metrics = Sets.newSetFromMap(new MapMaker().weakKeys().makeMap());

  private final String id;

  private final MetricRegistry registry;

  private final DefaultMetricsManagerContext baseContext;

  @NonNull
  private final MetricsManagerConfiguration metricsManagerConfiguration;

  private MetricFilter graphiteMetricFilter;

  private Graphite graphite;

  public DefaultMetricsManager()
  {
    this(
        String.valueOf(INSTANCE_COUNT.getAndIncrement()),
        new DefaultDispatchQueueBuilder(
            "com.jive.myco.commons",
            DispatcherConfig.getDefaultDispatcher()),
        new MetricRegistry(),
        MetricsManagerConfiguration.builder().build());
  }

  public DefaultMetricsManager(@NonNull final String id,
      final DispatchQueueBuilder dispatchQueueBuilder,
      @NonNull final MetricRegistry metricRegistry,
      @NonNull final MetricsManagerConfiguration metricsManagerConfiguration)
  {
    super(dispatchQueueBuilder.segment("metrics", id, "lifecycle").build());
    this.id = id;
    this.registry = metricRegistry;
    this.metricsManagerConfiguration = metricsManagerConfiguration;
    baseContext = new DefaultMetricsManagerContext(null);
  }

  @Override
  protected PnkyPromise<Void> initInternal()
  {
    registry.register(MetricRegistry.name("jvm", "gc"), new GarbageCollectorMetricSet());

    registry.register(MetricRegistry.name("jvm", "memory"), new MemoryUsageGaugeSet());

    registry.register(MetricRegistry.name("jvm", "buffer-pool"),
        new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));

    registry.register(MetricRegistry.name("jvm", "thread-states"),
        new ThreadStatesGaugeSet());

    registry.register(MetricRegistry.name("jvm", "fd", "usage"),
        new FileDescriptorRatioGauge());

    if (metricsManagerConfiguration.isSlf4jReporterEnabled())
    {
      final Slf4jReporter reporter = Slf4jReporter.forRegistry(registry)
          .outputTo(LoggerFactory.getLogger("com.jive.jotter.broker"))
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .build();

      reporters.add(reporter);

      reporter.start(metricsManagerConfiguration.getSlf4jReporterPeriod(),
          TimeUnit.MILLISECONDS);
    }

    if (metricsManagerConfiguration.isJmxReporterEnabled())
    {
      final JmxReporter jmxReporter = JmxReporter.forRegistry(registry)
          .inDomain(metricsManagerConfiguration.getJmxReporterDomain())
          .registerWith(ManagementFactory.getPlatformMBeanServer())
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .build();

      reporters.add(jmxReporter);

      jmxReporter.start();
    }

    if (metricsManagerConfiguration.isGraphiteReporterEnabled())
    {
      graphite = Graphite.builder()
          .id(id)
          .address(metricsManagerConfiguration.getGraphiteReporterAddress())
          .queueSize(metricsManagerConfiguration.getGraphiteReporterQueueSize())
          .socketFactory(metricsManagerConfiguration.getGraphiteReporterSocketFactory())
          .build();

      // Technically synchronized; however, there is no chance of it being contended at
      // this time since we just created it.
      graphite.init();

      if (metricsManagerConfiguration.isGraphiteReporterFilterUnchangedCounters())
      {
        graphiteMetricFilter = new UnchangedCounterFilter();
        registry.addListener((MetricRegistryListener) graphiteMetricFilter);
      }
      else
      {
        graphiteMetricFilter = MetricFilter.ALL;
      }

      final GraphiteReporter graphiteReporter = GraphiteReporter.forRegistry(registry)
          .prefixedWith(metricsManagerConfiguration.getGraphiteReporterPrefix())
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .filter(graphiteMetricFilter)
          .build(graphite);

      reporters.add(graphiteReporter);

      graphiteReporter.start(metricsManagerConfiguration.getGraphiteReporterPeriod(),
          TimeUnit.MILLISECONDS);
    }

    return immediatelyComplete(null);
  }

  @Override
  protected PnkyPromise<Void> destroyInternal()
  {
    for (final Closeable reporter : reporters)
    {
      try
      {
        reporter.close();
      }
      catch (final Exception e)
      {
        log.warn("Error closing reporter of type [{}].", reporter.getClass());
      }
    }

    if (graphiteMetricFilter != null && graphiteMetricFilter instanceof MetricRegistryListener)
    {
      registry.removeListener((MetricRegistryListener) graphiteMetricFilter);
    }

    final Pnky<Void> future = Pnky.create();

    if (graphite != null)
    {
      // Hack to destroy the Graphite instance, which is a blocking operation.
      final Thread t = new Thread(() ->
      {
        try
        {
          graphite.destroy();
          future.resolve(null);
        }
        catch (final Exception e)
        {
          future.reject(e);
        }
      });

      t.start();
    }
    else
    {
      future.resolve(null);
    }

    return future
        .thenAccept((result) ->
        {
          registry.removeMatching((name, metric) -> metrics.contains(metric));
          metrics.clear();
        });
  }

  @Override
  public void removeMetric(final Metric metricToRemove)
  {
    registry.removeMatching((name, metric) -> metric == metricToRemove);
  }

  @Override
  public MetricsManagerContext segment()
  {
    return baseContext;
  }

  @Override
  public MetricsManagerContext segment(@NonNull final String segment,
      final String... additionalSegments)
  {
    return segment(baseContext, segment, additionalSegments);
  }

  protected DefaultMetricsManagerContext segment(
      @NonNull final DefaultMetricsManagerContext context,
      @NonNull final String segment, final String... additionalSegments)
  {
    final LinkedList<String> parts = new LinkedList<>();

    final String prefix = context.getBaseName();
    if (prefix != null)
    {
      parts.add(prefix);
    }

    parts.add(segment);

    if (additionalSegments != null)
    {
      parts.addAll(Arrays.asList(additionalSegments));
    }

    return new DefaultMetricsManagerContext(Joiner.on(".").join(parts));
  }

  protected Counter getCounter(final String prefix, final String segment,
      final String... additionalSegments)
  {
    final String name = getMetricName(prefix, segment, additionalSegments);
    final Counter counter = registry.counter(name);
    metrics.add(counter);
    checkState(name, counter);
    return counter;
  }

  protected Timer getTimer(final String prefix, final String segment,
      final String... additionalSegments)
  {
    final String name = getMetricName(prefix, segment, additionalSegments);
    final Timer timer = registry.timer(name);
    metrics.add(timer);
    checkState(name, timer);
    return timer;
  }

  protected Meter getMeter(final String prefix, final String segment,
      final String... additionalSegments)
  {
    final String name = getMetricName(prefix, segment, additionalSegments);
    final Meter meter = registry.meter(name);
    metrics.add(meter);
    checkState(name, meter);
    return meter;
  }

  protected Histogram getHistogram(final String prefix, final String segment,
      final String... additionalSegments)
  {
    final String name = getMetricName(prefix, segment, additionalSegments);
    final Histogram histogram = registry.histogram(name);
    metrics.add(histogram);
    checkState(name, histogram);
    return histogram;
  }

  protected RatioGauge addRatio(@NonNull final Callable<Ratio> function, final String prefix,
      final String segment, final String... additionalSegments)
  {
    final String name = getMetricName(prefix, segment, additionalSegments);

    final RatioGauge gauge = new RatioGauge()
    {
      @Override
      public Ratio getRatio()
      {
        try
        {
          return function.call();
        }
        catch (final Exception e)
        {
          log.error("Error calculating ratio [{}].", name, e);
          return RatioGauge.Ratio.of(0d, 0d); // Yields NaN
        }
      }
    };

    registry.remove(name);
    registry.register(name, gauge);

    metrics.add(gauge);

    checkState(name, gauge);

    return gauge;
  }

  protected <T> Gauge<T> addGauge(@NonNull final Callable<T> function, final String prefix,
      final String segment, final String... additionalSegments)
  {
    final String name = getMetricName(prefix, segment, additionalSegments);

    final Gauge<T> gauge = new Gauge<T>()
    {
      @Override
      public T getValue()
      {
        try
        {
          return function.call();
        }
        catch (final Exception e)
        {
          log.error("Error calculating gauge [{}].", name, e);
          return null;
        }
      }
    };

    registry.remove(name);
    registry.register(name, gauge);

    metrics.add(gauge);

    checkState(name, gauge);

    return gauge;
  }

  private String getMetricName(final String prefix, @NonNull final String segment,
      final String... additionalSegments)
  {
    int size = 1;

    if (additionalSegments != null)
    {
      size += additionalSegments.length;
    }

    final String[] parts = new String[size];
    parts[0] = segment;

    if (additionalSegments != null)
    {
      System.arraycopy(additionalSegments, 0, parts, 1, additionalSegments.length);
    }

    // This method is OK w/ prefix being null. It will just ignore it.
    return MetricRegistry.name(prefix, parts);
  }

  private void checkState(final String name, final Metric metric)
  {
    if (lifecycleStage != LifecycleStage.INITIALIZED)
    {
      registry.remove(name);
      metrics.remove(metric);
      throw new IllegalStateException("Manager destroyed");
    }
  }

  @ToString(of = { "prefix" })
  @RequiredArgsConstructor
  private final class DefaultMetricsManagerContext implements MetricsManagerContext
  {
    private final String prefix;

    @Override
    public MetricsManager getMetricsManager()
    {
      return DefaultMetricsManager.this;
    }

    @Override
    public String getBaseName()
    {
      return prefix;
    }

    @Override
    public void removeMetric(final Metric metric)
    {
      DefaultMetricsManager.this.removeMetric(metric);
    }

    @Override
    public MetricsManagerContext segment(final String segment, final String... additionalSegments)
    {
      return DefaultMetricsManager.this.segment(this, segment, additionalSegments);
    }

    @Override
    public Timer getTimer(final String segment, final String... additionalSegments)
    {
      return DefaultMetricsManager.this.getTimer(prefix, segment, additionalSegments);
    }

    @Override
    public Meter getMeter(final String segment, final String... additionalSegments)
    {
      return DefaultMetricsManager.this.getMeter(prefix, segment, additionalSegments);
    }

    @Override
    public Histogram getHistogram(final String segment, final String... additionalSegments)
    {
      return DefaultMetricsManager.this.getHistogram(prefix, segment, additionalSegments);
    }

    @Override
    public Counter getCounter(final String segment, final String... additionalSegments)
    {
      return DefaultMetricsManager.this.getCounter(prefix, segment, additionalSegments);
    }

    @Override
    public RatioGauge addRatio(final Callable<Ratio> function, final String segment,
        final String... additionalSegments)
    {
      return DefaultMetricsManager.this.addRatio(function, prefix, segment, additionalSegments);
    }

    @Override
    public <T> Gauge<T> addGauge(final Callable<T> function, final String segment,
        final String... additionalSegments)
    {
      return DefaultMetricsManager.this.addGauge(function, prefix, segment, additionalSegments);
    }
  };
}
