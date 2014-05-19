package com.jive.myco.commons.metrics;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.fusesource.hawtdispatch.Dispatcher;
import org.junit.Test;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.RatioGauge;
import com.codahale.metrics.RatioGauge.Ratio;
import com.codahale.metrics.Timer;
import com.jive.myco.commons.callbacks.CallbackFuture;
import com.jive.myco.commons.lifecycle.LifecycleStage;

public class DefaultMetricsManagerTest
{
  @Test
  public void testInitFailure() throws Exception
  {
    MetricsManagerConfiguration config = mock(MetricsManagerConfiguration.class);
    when(config.isSlf4jReporterEnabled()).thenThrow(new IllegalStateException());
    final MetricsManager manager = createMetricsManager(null, null, config);

    final CallbackFuture<Void> callback = new CallbackFuture<>();
    manager.init(callback);

    try
    {
      callback.get(50, TimeUnit.MILLISECONDS);
      fail();
    }
    catch (final ExecutionException e)
    {
      assertTrue(e.getCause() instanceof IllegalStateException);
      assertEquals(LifecycleStage.DESTROYED, manager.getLifecycleStage());
    }
  }

  @Test
  public void testLifecycle() throws Exception
  {
    final MetricsManager manager =
        createMetricsManager(null, null, MetricsManagerConfiguration.builder().build());

    try
    {
      manager.segment().getCounter("blah");
      fail();
    }
    catch (final Exception e)
    {
      // Expected
    }

    CallbackFuture<Void> callback = new CallbackFuture<>();
    manager.init(callback);
    callback.get(50, TimeUnit.MILLISECONDS);

    assertEquals(LifecycleStage.INITIALIZED, manager.getLifecycleStage());

    manager.segment().getCounter("blah");

    callback = new CallbackFuture<>();
    manager.destroy(callback);
    callback.get(50, TimeUnit.MILLISECONDS);

    assertEquals(LifecycleStage.DESTROYED, manager.getLifecycleStage());

    try
    {
      manager.segment().getCounter("blah");
      fail();
    }
    catch (final Exception e)
    {
      // Expected
    }
  }

  @Test
  public void testAll() throws Exception
  {
    final MetricsManager manager =
        createMetricsManager(null, null, MetricsManagerConfiguration.builder().build());

    CallbackFuture<Void> callback = new CallbackFuture<>();
    manager.init(callback);
    callback.get(50, TimeUnit.MILLISECONDS);

    assertEquals(LifecycleStage.INITIALIZED, manager.getLifecycleStage());

    final MetricsManagerContext baseContext = manager.segment();
    final MetricsManagerContext subContext = manager.segment("sub");
    final MetricsManagerContext subContext2 = manager.segment("sub", "2", "3", "4");
    final MetricsManagerContext subContext2Alternate = subContext.segment("2", "3", "4");
    assertNull(baseContext.getBaseName());

    // Test creating unique metrics in one context

    final Meter baseMeter = baseContext.getMeter("meter");
    final Meter baseMeter2 = baseContext.getMeter("meter", "2");
    assertNotSame(baseMeter, baseMeter2);

    final Timer baseTimer = baseContext.getTimer("timer");
    final Timer baseTimer2 = baseContext.getTimer("timer", "2");
    assertNotSame(baseTimer, baseTimer2);

    final Histogram baseHistogram = baseContext.getHistogram("histogram");
    final Histogram baseHistogram2 = baseContext.getHistogram("histogram", "2");
    assertNotSame(baseHistogram, baseHistogram2);

    final Counter baseCounter = baseContext.getCounter("counter");
    final Counter baseCounter2 = baseContext.getCounter("counter", "2");
    assertNotSame(baseCounter, baseCounter2);

    final RatioGauge baseRatio = baseContext.addRatio(
        new Callable<Ratio>()
        {
          @Override
          public Ratio call() throws Exception
          {
            return Ratio.of(1d, 1d);
          }
        },
        "ratio");

    final RatioGauge baseRatio2 = baseContext.addRatio(
        new Callable<Ratio>()
        {
          @Override
          public Ratio call() throws Exception
          {
            return Ratio.of(1d, 1d);
          }
        }, "ratio", "2");

    assertNotSame(baseRatio, baseRatio2);

    final Gauge<Integer> baseGauge = baseContext.addGauge(
        new Callable<Integer>()
        {
          @Override
          public Integer call() throws Exception
          {
            return 1;
          }
        }, "gauge");

    final Gauge<Integer> baseGauge2 = baseContext.addGauge(
        new Callable<Integer>()
        {
          @Override
          public Integer call() throws Exception
          {
            return 1;
          }
        }, "gauge2");

    assertNotSame(baseGauge, baseGauge2);

    // Test get or create in one context
    assertSame(baseMeter, baseContext.getMeter("meter"));
    assertSame(baseMeter2, baseContext.getMeter("meter", "2"));
    assertSame(baseTimer, baseContext.getTimer("timer"));
    assertSame(baseTimer2, baseContext.getTimer("timer", "2"));
    assertSame(baseHistogram, baseContext.getHistogram("histogram"));
    assertSame(baseHistogram2, baseContext.getHistogram("histogram", "2"));
    assertSame(baseCounter, baseContext.getCounter("counter"));
    assertSame(baseCounter2, baseContext.getCounter("counter", "2"));

    assertNotSame(baseRatio, baseContext.addRatio(
        new Callable<Ratio>()
        {
          @Override
          public Ratio call() throws Exception
          {
            return Ratio.of(1d, 1d);
          }
        },
        "ratio"));

    assertNotSame(baseGauge, baseContext.addGauge(
        new Callable<Integer>()
        {
          @Override
          public Integer call() throws Exception
          {
            return 1;
          }
        },
        "gauge"));

    // Test duplicate name in different contexts

    final Meter subMeter = subContext.getMeter("meter");
    assertNotSame(baseMeter, subMeter);

    final Timer subTimer = subContext.getTimer("timer");
    assertNotSame(baseTimer, subTimer);

    final Histogram subHistogram = subContext.getHistogram("histogram");
    assertNotSame(baseHistogram, subHistogram);

    final Counter subCounter = subContext.getCounter("counter");
    assertNotSame(baseCounter, subCounter);

    final RatioGauge subRatio = subContext.addRatio(
        new Callable<Ratio>()
        {
          @Override
          public Ratio call() throws Exception
          {
            return Ratio.of(1d, 1d);
          }
        },
        "ratio");
    assertNotSame(baseRatio, subRatio);

    final Gauge<Integer> subGauge = subContext.addGauge(
        new Callable<Integer>()
        {
          @Override
          public Integer call() throws Exception
          {
            return 1;
          }
        }, "gauge");
    assertNotSame(baseGauge, subGauge);

    // Test duplicate name in different contexts with same prefix
    assertEquals(subContext2.getBaseName(), subContext2Alternate.getBaseName());
    assertSame(subContext2.getMeter("blah"), subContext2Alternate.getMeter("blah"));

    // Test delete metric
    subContext.getMetricsManager().removeMetric(baseMeter);
    subContext.removeMetric(baseMeter2);

    assertNotSame(baseMeter, baseContext.getMeter("meter"));
    assertNotSame(baseMeter2, baseContext.getMeter("meter", "2"));

    callback = new CallbackFuture<>();
    manager.destroy(callback);
    callback.get(50, TimeUnit.MILLISECONDS);

    assertEquals(LifecycleStage.DESTROYED, manager.getLifecycleStage());
  }

  @Test
  public void testRatio() throws Exception
  {
    final MetricsManager manager =
        createMetricsManager(null, null, MetricsManagerConfiguration.builder().build());

    final CallbackFuture<Void> callback = new CallbackFuture<>();
    manager.init(callback);
    callback.get(50, TimeUnit.MILLISECONDS);

    assertEquals(LifecycleStage.INITIALIZED, manager.getLifecycleStage());

    final MetricsManagerContext baseContext = manager.segment();

    final RatioGauge baseRatio = baseContext.addRatio(
        new Callable<Ratio>()
        {
          @Override
          public Ratio call() throws Exception
          {
            throw new RuntimeException();
          }
        }, "ratio");

    final Gauge<Integer> baseGauge = baseContext.addGauge(
        new Callable<Integer>()
        {
          @Override
          public Integer call() throws Exception
          {
            throw new RuntimeException();
          }
        }, "gauge");


    assertEquals(Double.valueOf(Double.NaN), baseRatio.getValue());
    assertNull(baseGauge.getValue());
  }

  private MetricsManager createMetricsManager(final String id, final Dispatcher dispatcher,
      final MetricsManagerConfiguration metricsManagerConfiguration)
  {
    final DefaultMetricsManager metricsManager = new DefaultMetricsManager();

    metricsManager.setId(id);
    metricsManager.setDispatcher(dispatcher);
    metricsManager.setMetricsManagerConfiguration(metricsManagerConfiguration);

    return metricsManager;
  }
}
