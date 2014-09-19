package com.jive.myco.commons.metrics;

import java.net.InetSocketAddress;

import org.junit.Ignore;
import org.junit.Test;

/**
 * A simple smoke test for {@link Graphite}.
 *
 * @author David Valeri
 */
@Ignore("Ignored as this is intended for manual debugging and validation against Graphite.")
public class GraphiteIT
{
  @Test
  public void testSendMetricsPickle() throws Exception
  {
    final InetSocketAddress inetSocketAddress = new InetSocketAddress("10.132.0.81", 2004);

    final Graphite graphite = Graphite.builder()
        .id("test")
        .address(inetSocketAddress)
        .pickle(true)
        .build();

    try
    {
      graphite.init();

      for (int i = 0; i < 10; i++)
      {
        graphite.send(getClass().getName() + ".test", "" + i, System.currentTimeMillis() / 1000);
        Thread.sleep(1000);
      }
    }
    finally
    {
      graphite.destroy();
    }
  }

  @Test
  public void testSendMetricsText() throws Exception
  {
    final InetSocketAddress inetSocketAddress = new InetSocketAddress("10.132.0.81", 2003);

    final Graphite graphite = Graphite.builder()
        .id("test")
        .address(inetSocketAddress)
        .pickle(false)
        .build();

    try
    {
      graphite.init();

      for (int i = 0; i < 10; i++)
      {
        graphite.send(getClass().getName() + ".test", "" + i, System.currentTimeMillis() / 1000);
        Thread.sleep(1000);
      }
    }
    finally
    {
      graphite.destroy();
    }
  }
}
