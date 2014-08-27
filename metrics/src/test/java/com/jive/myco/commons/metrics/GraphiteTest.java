package com.jive.myco.commons.metrics;

import static com.jayway.awaitility.Awaitility.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.SocketFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.python.core.PyFile;
import org.python.core.PyList;
import org.python.core.PyTuple;
import org.python.modules.cPickle;

/**
 * Test for {@link Graphite}.
 *
 * @author David Valeri
 */
@RunWith(MockitoJUnitRunner.class)
public class GraphiteTest
{
  private final InetSocketAddress inetSocketAddress = new InetSocketAddress("localhost", 12354);

  @Mock
  private SocketFactory socketFactory;

  @Mock
  private Socket socket;

  @Mock
  private OutputStream outputStream;

  @Mock
  private InputStream inputStream;

  @Test
  public void testBatchTimeout() throws Exception
  {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final Graphite graphite = createStandardGraphite(baos, true);

    try
    {
      graphite.init();

      graphite.send("blah", "1.0", 12345L);
      graphite.send("foo", "2.0", 123456L);
      graphite.send("bar", "3.0", 1234567L);

      await().until(() ->
      {
        try
        {
          verify(outputStream, times(1)).flush();
        }
        catch (final Exception e)
        {
          throw new RuntimeException(e);
        }
      });

      verify(socket, atLeast(1)).connect(inetSocketAddress, 2);

      final byte[] bytes = baos.toByteArray();

      final PyList payload =
          (PyList) cPickle.load(new PyFile(new ByteArrayInputStream(bytes, 4, bytes.length - 4)));

      assertEquals(3, payload.size());

      final PyTuple metric = (PyTuple) payload.get(0);
      final PyTuple metric2 = (PyTuple) payload.get(1);
      final PyTuple metric3 = (PyTuple) payload.get(2);

      assertEquals("blah", metric.get(0));
      assertEquals(BigInteger.valueOf(12345L), ((PyTuple) metric.get(1)).get(0));
      assertEquals("1.0", ((PyTuple) metric.get(1)).get(1));

      assertEquals("foo", metric2.get(0));
      assertEquals(BigInteger.valueOf(123456L), ((PyTuple) metric2.get(1)).get(0));
      assertEquals("2.0", ((PyTuple) metric2.get(1)).get(1));

      assertEquals("bar", metric3.get(0));
      assertEquals(BigInteger.valueOf(1234567L), ((PyTuple) metric3.get(1)).get(0));
      assertEquals("3.0", ((PyTuple) metric3.get(1)).get(1));
    }
    finally
    {
      if (graphite != null)
      {
        graphite.destroy();
      }
    }
  }

  @Test
  public void testBatchFull() throws Exception
  {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final Graphite graphite = createStandardGraphite(baos, true);

    try
    {
      graphite.init();

      for (int i = 0; i < 75; i++)
      {
        graphite.send("blah", "1.0", 12345L);
      }

      await().until(() ->
      {
        try
        {
          verify(outputStream, times(1)).flush();
        }
        catch (final Exception e)
        {
          throw new RuntimeException(e);
        }
      });

      verify(socket, atLeast(1)).connect(inetSocketAddress, 2);

      final byte[] bytes = baos.toByteArray();
      baos.reset();

      await().until(() ->
      {
        try
        {
          verify(outputStream, times(2)).flush();
        }
        catch (final Exception e)
        {
          throw new RuntimeException(e);
        }
      });

      final byte[] bytes2 = baos.toByteArray();
      baos.reset();

      final PyList payload =
          (PyList) cPickle.load(new PyFile(new ByteArrayInputStream(bytes, 4, bytes.length - 4)));

      final PyList payload2 =
          (PyList) cPickle.load(new PyFile(new ByteArrayInputStream(bytes2, 4, bytes2.length - 4)));

      assertEquals(50, payload.size());
      assertEquals(25, payload2.size());
    }
    finally
    {
      if (graphite != null)
      {
        graphite.destroy();
      }
    }
  }

  @Test
  public void testWriteBatchRetry() throws Exception
  {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();

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

    final AtomicInteger writeByteArrayCounter = new AtomicInteger();

    doAnswer(
        (invocation) ->
        {
          // Throw an exception the first 100 times.
          if (writeByteArrayCounter.getAndIncrement() < 100)
          {
            baos.reset();

            if (writeByteArrayCounter.get() % 5 == 0)
            {
              throw new IOException("test exception", new NumberFormatException());
            }
            else
            {
              throw new IOException("test exception");
            }

          }
          else
          {
            baos.write((byte[]) invocation.getArguments()[0]);
            return null;
          }
        })
        .when(outputStream).write(any(byte[].class));

    final CountDownLatch readLatch = new CountDownLatch(1);
    when(inputStream.read()).thenAnswer((invocation) ->
    {
      readLatch.await();
      return -1;
    });

    doAnswer(
        (invocation) ->
        {
          readLatch.countDown();
          return null;
        }).when(socket).close();

    final Graphite graphite = Graphite.builder()
        .id("test")
        .address(inetSocketAddress)
        .socketFactory(socketFactory)
        .pickle(true)
        .build();

    try
    {
      graphite.init();

      graphite.send("blah", "1.0", 12345L);
      graphite.send("foo", "2.0", 123456L);
      graphite.send("bar", "3.0", 1234567L);

      await().until(() ->
      {
        try
        {
          verify(outputStream, times(1)).flush();
        }
        catch (final Exception e)
        {
          throw new RuntimeException(e);
        }
      });

      verify(socket, atLeast(1)).connect(inetSocketAddress, 2);

      final byte[] bytes = baos.toByteArray();

      final PyList payload =
          (PyList) cPickle.load(new PyFile(new ByteArrayInputStream(bytes, 4, bytes.length - 4)));

      assertEquals(3, payload.size());

      final PyTuple metric = (PyTuple) payload.get(0);
      final PyTuple metric2 = (PyTuple) payload.get(1);
      final PyTuple metric3 = (PyTuple) payload.get(2);

      assertEquals("blah", metric.get(0));
      assertEquals(BigInteger.valueOf(12345L), ((PyTuple) metric.get(1)).get(0));
      assertEquals("1.0", ((PyTuple) metric.get(1)).get(1));

      assertEquals("foo", metric2.get(0));
      assertEquals(BigInteger.valueOf(123456L), ((PyTuple) metric2.get(1)).get(0));
      assertEquals("2.0", ((PyTuple) metric2.get(1)).get(1));

      assertEquals("bar", metric3.get(0));
      assertEquals(BigInteger.valueOf(1234567L), ((PyTuple) metric3.get(1)).get(0));
      assertEquals("3.0", ((PyTuple) metric3.get(1)).get(1));
    }
    finally
    {
      if (graphite != null)
      {
        graphite.destroy();
      }
    }
  }

  @Test
  public void testConnectRetry() throws Exception
  {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();

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

    final AtomicInteger writeByteArrayCounter = new AtomicInteger();

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

    doAnswer(
        (invocation) ->
        {
          // Throw an exception the first 2 times.
        if (writeByteArrayCounter.getAndIncrement() < 1)
        {
          baos.reset();

          if (writeByteArrayCounter.get() % 5 == 0)
          {
            throw new IOException("test exception", new NumberFormatException());
          }
          else
          {
            throw new IOException("test exception");
          }
        }

          return null;
        })
        .when(socket).connect(inetSocketAddress, 2);

    doAnswer(
        (invocation) ->
        {
          readLatch.countDown();
          return null;
        }).when(socket).close();

    final Graphite graphite = Graphite.builder()
        .id("test")
        .address(inetSocketAddress)
        .socketFactory(socketFactory)
        .pickle(true)
        .build();

    try
    {
      graphite.init();

      graphite.send("blah", "1.0", 12345L);
      graphite.send("foo", "2.0", 123456L);
      graphite.send("bar", "3.0", 1234567L);

      await().until(() ->
      {
        try
        {
          verify(outputStream, times(1)).flush();
        }
        catch (final Exception e)
        {
          throw new RuntimeException(e);
        }
      });

      verify(socket, atLeast(1)).connect(inetSocketAddress, 2);

      final byte[] bytes = baos.toByteArray();

      final PyList payload =
          (PyList) cPickle.load(new PyFile(new ByteArrayInputStream(bytes, 4, bytes.length - 4)));

      assertEquals(3, payload.size());

      final PyTuple metric = (PyTuple) payload.get(0);
      final PyTuple metric2 = (PyTuple) payload.get(1);
      final PyTuple metric3 = (PyTuple) payload.get(2);

      assertEquals("blah", metric.get(0));
      assertEquals(BigInteger.valueOf(12345L), ((PyTuple) metric.get(1)).get(0));
      assertEquals("1.0", ((PyTuple) metric.get(1)).get(1));

      assertEquals("foo", metric2.get(0));
      assertEquals(BigInteger.valueOf(123456L), ((PyTuple) metric2.get(1)).get(0));
      assertEquals("2.0", ((PyTuple) metric2.get(1)).get(1));

      assertEquals("bar", metric3.get(0));
      assertEquals(BigInteger.valueOf(1234567L), ((PyTuple) metric3.get(1)).get(0));
      assertEquals("3.0", ((PyTuple) metric3.get(1)).get(1));
    }
    finally
    {
      if (graphite != null)
      {
        graphite.destroy();
      }
    }
  }

  @Test
  public void testQueueFull() throws Exception
  {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    when(socketFactory.createSocket()).thenReturn(socket);

    when(socket.getInputStream()).thenReturn(inputStream);
    when(socket.getOutputStream()).thenReturn(outputStream);

    final CountDownLatch writeLatch = new CountDownLatch(1);

    doAnswer(
        (invocation) ->
        {
          writeLatch.await();
          baos.write((Integer) invocation.getArguments()[0]);
          return null;
        })
        .when(outputStream).write(anyByte());

    doAnswer(
        (invocation) ->
        {
          writeLatch.await();
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

    doAnswer(
        (invocation) ->
        {
          readLatch.countDown();
          return null;
        }).when(socket).close();

    final Graphite graphite = Graphite.builder()
        .id("test")
        .address(inetSocketAddress)
        .socketFactory(socketFactory)
        .queueSize(50)
        .batchTimeoutTime(5000L)
        .pickle(true)
        .build();

    try
    {
      graphite.init();

      // Exceed queue size to ensure that items get rejected.
      for (int i = 0; i < 150; i++)
      {
        graphite.send("blah", "1.0", 12345L);
        // Slow down you fool, you fill the queue before it can even offload the metrics to
        // the batch.
        Thread.sleep(5L);
      }

      writeLatch.countDown();

      await().until(() ->
      {
        try
        {
          verify(outputStream, times(2)).flush();
        }
        catch (final Exception e)
        {
          throw new RuntimeException(e);
        }
      });

      verify(socket, atLeast(1)).connect(inetSocketAddress, 2);

      final byte[] bytes = baos.toByteArray();
      final ByteBuffer buffer = ByteBuffer.wrap(bytes);
      final int length = buffer.getInt();

      final PyList payload =
          (PyList) cPickle
              .load(new PyFile(new ByteArrayInputStream(bytes, 4, length - 1 + 4)));

      assertEquals(50, payload.size());

      buffer.position(length + 4);
      final int length2 = buffer.getInt();
      final PyList payload2 =
          (PyList) cPickle.load(new PyFile(new ByteArrayInputStream(bytes, length + 4 + 4,
              length + 4 + length2 - 1 + 4)));

      assertEquals(50, payload2.size());

    }
    finally
    {
      if (graphite != null)
      {
        graphite.destroy();
      }
    }
  }

  @Test
  public void testTextEncoding() throws Exception
  {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final Graphite graphite = createStandardGraphite(baos);

    try
    {
      graphite.init();

      graphite.send("blah", "1.0", 12345L);
      graphite.send("foo", "2.0", 123456L);
      graphite.send("bar", "3.0", 1234567L);

      await().until(() ->
      {
        try
        {
          verify(outputStream, times(1)).flush();
        }
        catch (final Exception e)
        {
          throw new RuntimeException(e);
        }
      });

      verify(socket, atLeast(1)).connect(inetSocketAddress, 2);

      final byte[] bytes = baos.toByteArray();

      final String[] metrics = new String(bytes, "UTF-8").split("\n");

      assertEquals(3, metrics.length);

      final String[] metric = metrics[0].split(" ");
      final String[] metric2 = metrics[1].split(" ");
      final String[] metric3 = metrics[2].split(" ");

      assertEquals("blah", metric[0]);
      assertEquals("1.0", metric[1]);
      assertEquals("12345", metric[2]);

      assertEquals("foo", metric2[0]);
      assertEquals("2.0", metric2[1]);
      assertEquals("123456", metric2[2]);

      assertEquals("bar", metric3[0]);
      assertEquals("3.0", metric3[1]);
      assertEquals("1234567", metric3[2]);
    }
    finally
    {
      if (graphite != null)
      {
        graphite.destroy();
      }
    }
  }

  private Graphite createStandardGraphite(final ByteArrayOutputStream baos) throws IOException,
      InterruptedException
  {
    return createStandardGraphite(baos, null);
  }

  private Graphite createStandardGraphite(final ByteArrayOutputStream baos, final Boolean pickle)
      throws IOException,
      InterruptedException
  {
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

    doAnswer(
        (invocation) ->
        {
          readLatch.countDown();
          return null;
        }).when(socket).close();

    final Graphite graphite = Graphite.builder()
        .id("test")
        .address(inetSocketAddress)
        .socketFactory(socketFactory)
        .pickle(pickle)
        .build();
    return graphite;
  }
}
