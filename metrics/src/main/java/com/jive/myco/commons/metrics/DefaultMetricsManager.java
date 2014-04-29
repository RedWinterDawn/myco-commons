package com.jive.myco.commons.metrics;

import java.io.Closeable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Dispatcher;
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
import com.codahale.metrics.RatioGauge;
import com.codahale.metrics.RatioGauge.Ratio;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.google.common.base.Joiner;
import com.google.common.collect.MapMaker;
import com.jive.myco.commons.callbacks.Callback;
import com.jive.myco.commons.callbacks.SafeCallbackRunnable;
import com.jive.myco.commons.lifecycle.LifecycleStage;
import com.jive.myco.commons.lifecycle.Lifecycled;

/**
 * A manager for the Metrics library.
 *
 * @author David Valeri
 */
@Slf4j
public final class DefaultMetricsManager implements MetricsManager, Lifecycled
{
  private static final String DISPATCH_QUEUE_METRICS_ENTITY_PREFIX =
      "com.jive.myco.commons.metrics";

  private static final AtomicInteger INSTANCE_COUNT = new AtomicInteger();

  private MetricRegistry registry = new MetricRegistry();

  private final AtomicReference<LifecycleStage> lifecycleStage = new AtomicReference<>(
      LifecycleStage.UNINITIALIZED);

  private final List<Closeable> reporters = new LinkedList<>();

  /**
   * A map of {@link Metric}s back to the name of the metric in the registry. Used to make removal
   * of metrics easier as one does not need to recalculate the name of the metric in order to remove
   * it from the registry. Uses weak keys because there is a concurrency risk between the registry's
   * internal map and this map. The weak keys let things that have actually been removed from the
   * registry's internal map get removed from this map.
   */
  private final Map<Metric, String> metricNameMap = new MapMaker().weakKeys().makeMap();

  private DispatchQueue lifecycleQueue;

  private DefaultMetricsManagerContext baseContext;

  @Setter
  private MetricsManagerConfiguration metricsManagerConfiguration;

  @Setter
  private Dispatcher dispatcher;

  @Setter
  private String id;

  @Override
  public void init(final Callback<Void> callback)
  {
    if (lifecycleStage.compareAndSet(LifecycleStage.UNINITIALIZED, LifecycleStage.INITIALIZING))
    {
      if (dispatcher == null)
      {
        dispatcher = DispatcherConfig.getDefaultDispatcher();
      }

      if (id == null)
      {
        id = String.valueOf(INSTANCE_COUNT.getAndIncrement());
      }

      lifecycleQueue = dispatcher.createQueue(
          Joiner.on(".").join(DISPATCH_QUEUE_METRICS_ENTITY_PREFIX, id, "lifecycle"));

      lifecycleQueue.execute(new SafeCallbackRunnable<Void>(callback)
      {
        @Override
        protected void doRun() throws Exception
        {
          final SafeCallbackRunnable<Void> that = this;

          try
          {
            if (metricsManagerConfiguration == null)
            {
              throw new IllegalStateException("Metrics configuration must not be null.");
            }
            else
            {
              registry = new MetricRegistry();
              
              registry.register(MetricRegistry.name("jvm", "gc"), new GarbageCollectorMetricSet());
              registry.register(MetricRegistry.name("jvm", "memory"), new MemoryUsageGaugeSet());
              registry.register(MetricRegistry.name("jvm", "thread-states"), new ThreadStatesGaugeSet());
              registry.register(MetricRegistry.name("jvm", "fd", "usage"), new FileDescriptorRatioGauge());

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
                    .build();

                reporters.add(jmxReporter);

                jmxReporter.start();
              }

              baseContext = new DefaultMetricsManagerContext(null);

              lifecycleStage.set(LifecycleStage.INITIALIZED);

              onSuccess(null);
            }
          }
          catch (final RuntimeException e)
          {
            lifecycleStage.set(LifecycleStage.INITIALIZATION_FAILED);
            destroy(new Callback<Void>()
            {
              @Override
              public void onSuccess(final Void result)
              {
                that.onFailure(e);
              }

              @Override
              public void onFailure(final Throwable cause)
              {
                log.warn("Failed to cleanup manager after failure.", cause);
                that.onFailure(e);
              }
            });
          }
        }
      });
    }
    else if (lifecycleStage.get() == LifecycleStage.INITIALIZED)
    {
      callback.onSuccess(null);
    }
    else
    {
      callback.onFailure(new IllegalStateException(
          "Cannot initialize manager in the [" + lifecycleStage.get() + "] stage."));
    }
  }

  @Override
  public void destroy(final Callback<Void> callback)
  {
    if (lifecycleStage.compareAndSet(LifecycleStage.INITIALIZED, LifecycleStage.DESTROYING)
        || lifecycleStage.compareAndSet(LifecycleStage.INITIALIZATION_FAILED,
            LifecycleStage.DESTROYING)
        || lifecycleStage.compareAndSet(LifecycleStage.DESTROYING, LifecycleStage.DESTROYING))
    {
      lifecycleQueue.execute(new SafeCallbackRunnable<Void>(callback)
      {
        @Override
        protected void doRun()
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

          if (registry != null)
          {
            final MetricRegistry previousRegistry = registry;
            registry = null;
            previousRegistry.removeMatching(MetricFilter.ALL);
          }

          lifecycleStage.set(LifecycleStage.DESTROYED);
          onSuccess(null);
        }
      });
    }
    else if (lifecycleStage.get() == LifecycleStage.DESTROYED
        || lifecycleStage.get() == LifecycleStage.UNINITIALIZED)
    {
      callback.onSuccess(null);
    }
    else
    {
      callback.onFailure(new IllegalStateException(
          "Cannot destroy manager in the [" + lifecycleStage.get() + "] stage."));
    }
  }

  @Override
  public LifecycleStage getLifecycleStage()
  {
    return lifecycleStage.get();
  }

  @Override
  public void removeMetric(final Metric metric)
  {
    final String name = metricNameMap.get(metric);
    if (name != null)
    {
      registry.remove(name);
    }
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
    metricNameMap.put(counter, name);

    return counter;
  }

  protected Timer getTimer(final String prefix, final String segment,
      final String... additionalSegments)
  {
    final String name = getMetricName(prefix, segment, additionalSegments);
    final Timer timer = registry.timer(name);
    metricNameMap.put(timer, name);
    return timer;
  }

  protected Meter getMeter(final String prefix, final String segment,
      final String... additionalSegments)
  {
    final String name = getMetricName(prefix, segment, additionalSegments);
    final Meter meter = registry.meter(name);
    metricNameMap.put(meter, name);
    return meter;
  }

  protected Histogram getHistogram(final String prefix, final String segment,
      final String... additionalSegments)
  {
    final String name = getMetricName(prefix, segment, additionalSegments);
    final Histogram histogram = registry.histogram(name);
    metricNameMap.put(histogram, name);
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

    metricNameMap.put(gauge, name);

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

    metricNameMap.put(gauge, name);

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
