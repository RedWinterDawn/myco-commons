package com.jive.myco.commons.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * An {@code InputStream} backed by a {@link ByteBuffer}.
 * 
 * Date: 1/20/14
 * 
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 * @author David Valeri
 */
@RequiredArgsConstructor
public class ByteBufferInputStream extends InputStream
{
  @NonNull
  private final ByteBuffer buffer;

  @Override
  public int available() throws IOException
  {
    return buffer.remaining();
  }

  @Override
  public int read() throws IOException
  {
    if (!buffer.hasRemaining())
    {
      return -1;
    }

    try
    {
      return buffer.get() & 0xff;
    }
    catch (final Exception e)
    {
      throw new IOException(e);
    }
  }

  @Override
  public int read(final byte[] b, final int off, int len) throws IOException
  {
    if (b == null)
    {
      throw new NullPointerException();
    }
    else if (off < 0 || len < 0 || len > b.length - off)
    {
      throw new IndexOutOfBoundsException();
    }
    else if (len == 0)
    {
      return 0;
    }

    final int available = available();
    if (available == 0)
    {
      return -1;
    }
    else
    {
      len = Math.min(available, len);
      buffer.get(b, off, len);
      return len;
    }
  }
}
