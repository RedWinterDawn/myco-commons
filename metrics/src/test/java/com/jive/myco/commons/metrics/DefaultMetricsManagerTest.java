package com.jive.myco.commons.metrics;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import org.fusesource.hawtdispatch.internal.DispatcherConfig;
import org.junit.Test;
import org.python.core.PyFile;
import org.python.core.PyList;
import org.python.core.PyTuple;
import org.python.modules.cPickle;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.RatioGauge;
import com.codahale.metrics.RatioGauge.Ratio;
import com.codahale.metrics.Timer;
import com.google.common.collect.Lists;
import com.jive.myco.commons.hawtdispatch.DefaultDispatchQueueBuilder;
import com.jive.myco.commons.lifecycle.LifecycleStage;

public class DefaultMetricsManagerTest
{
  @Test
  public void testLifecycle() throws Exception
  {
    final MetricsManager manager =
        createMetricsManager(MetricsManagerConfiguration.builder().build());

    try
    {
      manager.segment().getCounter("blah");
      fail();
    }
    catch (final Exception e)
    {
      // Expected
    }

    manager.init().get(100, TimeUnit.MILLISECONDS);

    assertEquals(LifecycleStage.INITIALIZED, manager.getLifecycleStage());

    manager.segment().getCounter("blah");

    manager.destroy().get(100, TimeUnit.MILLISECONDS);

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
        createMetricsManager(MetricsManagerConfiguration.builder().build());

    manager.init().get(100, TimeUnit.MILLISECONDS);

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

    manager.destroy().get(50, TimeUnit.MILLISECONDS);

    assertEquals(LifecycleStage.DESTROYED, manager.getLifecycleStage());
  }

  @Test
  public void testRatio() throws Exception
  {
    final MetricsManager manager =
        createMetricsManager(MetricsManagerConfiguration.builder().build());

    manager.init().get(50, TimeUnit.MILLISECONDS);

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

  @Test
  public void testGraphiteFilterUnchangedCounters() throws Exception
  {
    final SocketFactory socketFactory = mock(SocketFactory.class);
    final Socket socket = mock(Socket.class);
    final InputStream inputStream = mock(InputStream.class);
    final OutputStream outputStream = mock(OutputStream.class);

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final InetSocketAddress inetSocketAddress = new InetSocketAddress("localhost", 12354);

    when(socketFactory.createSocket()).thenReturn(socket);

    when(socket.getInputStream()).thenReturn(inputStream);
    when(socket.getOutputStream()).thenReturn(outputStream);

    doAnswer(
        (invocation) ->
        {
          baos.write((Integer) invocation.getArguments()[0]);
          return null;
        })
        .when(outputStream).write(anyByte());

    doAnswer(
        (invocation) ->
        {
          baos.write((byte[]) invocation.getArguments()[0]);
          return null;
        })
        .when(outputStream).write(any(byte[].class));

    final CountDownLatch readLatch = new CountDownLatch(1);
    when(inputStream.read()).thenAnswer((invocation) ->
    {
      readLatch.await();
      return -1;
    });

    final MetricsManager manager = new DefaultMetricsManager(
        "test",
        new DefaultDispatchQueueBuilder("test", DispatcherConfig.getDefaultDispatcher()),
        new MetricRegistry(),
        MetricsManagerConfiguration.builder()
            .graphiteReporterAddress(inetSocketAddress)
            .graphiteReporterSocketFactory(socketFactory)
            .graphiteReporterEnabled(true)
            .graphiteReporterPickle(true)
            .graphiteReporterFilterUnchangedCounters(true)
            .build());

    manager.init().get();

    final Counter changing = manager.segment().getCounter("changing");
    Counter unchanging = manager.segment().getCounter("unchanging");

    Thread.sleep(2500);

    changing.inc();

    Thread.sleep(2500);

    changing.inc();

    Thread.sleep(2500);

    manager.removeMetric(unchanging);

    Thread.sleep(2500);

    unchanging = manager.segment().getCounter("unchanging");

    Thread.sleep(2500);

    manager.destroy().get();

    // Validate the output
    final byte[] bytes = baos.toByteArray();
    final List<PyList> tuples = new LinkedList<>();
    final ByteBuffer buffer = ByteBuffer.wrap(bytes);
    int batchStartingOffset = 0;

    while (batchStartingOffset < bytes.length)
    {
      buffer.position(batchStartingOffset);
      final int batchPayloadLength = buffer.getInt();
      final int batchPayloadStartingOffset = batchStartingOffset + 4;
      final int batchTotalLength = batchPayloadLength + 4;

      tuples.add((PyList) cPickle.load(
          new PyFile(
              new ByteArrayInputStream(bytes, batchPayloadStartingOffset, batchPayloadLength))));

      batchStartingOffset += batchTotalLength;
    }

    // Changing
    // Step 0: Looking for changing with value of 0
    // Step 1: Looking for changing to not appear
    // Step 2: Looking for changing metric to go to 1
    // Step 3: Looking for changing metric to not appear
    // Step 4: Looking for changing metric to go to 2
    // Step 5: Looking for changing metric to not appear
    // Step 6: Winner

    final ArrayList<String> changingList = Lists.newArrayList();

    int count = 0;
    String text = "com.jive.metrics.changing.count";
    for (final PyList py : tuples)
    {
      for (final Object item : py)
      {
        final PyTuple outerTuple = (PyTuple) item;
        final String result = (String) outerTuple.get(0);
        if (result.equals(text))
        {
          final PyTuple innerTuple = (PyTuple) outerTuple.get(1);
          final String num = (String) innerTuple.get(1);
          changingList.add(num);
          count--;
        }
      }
      count++;
    }

    assertEquals(changingList, Arrays.asList("0", "1", "2"));
    assertTrue(count == (tuples.size() - changingList.size()));

    // Unchanging
    // Step 0: Looking for unchanging with value of 0
    // Step 1: Looking for unchanging to not appear
    // Step 2: Looking for unchanging with value of 0
    // Step 3: Looking for unchanging to not appear
    // Step 4: Winner

    final ArrayList<String> unchangingList = Lists.newArrayList();

    count = 0;
    text = "com.jive.metrics.unchanging.count";
    for (final PyList py : tuples)
    {
      for (final Object item : py)
      {
        final PyTuple outerTuple = (PyTuple) item;
        final String result = (String) outerTuple.get(0);
        if (result.equals(text))
        {
          final PyTuple innerTuple = (PyTuple) outerTuple.get(1);
          final String num = (String) innerTuple.get(1);
          unchangingList.add(num);
          count--;
        }
      }
      count++;
    }

    assertEquals(unchangingList, Arrays.asList("0", "0"));
    assertTrue(count == (tuples.size() - unchangingList.size()));

  }

  private MetricsManager createMetricsManager(
      final MetricsManagerConfiguration metricsManagerConfiguration)
  {

    return new DefaultMetricsManager(
        "test",
        new DefaultDispatchQueueBuilder("test", DispatcherConfig.getDefaultDispatcher()),
        new MetricRegistry(),
        metricsManagerConfiguration);
  }
}
