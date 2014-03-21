package com.jive.myco.commons.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * This {@code InputStream} is built on top of backing byte arrays that were filled by a paired
 * {@link DynamicallyResizedByteOutputStream}. Acquire instances of this class through an instance
 * of {@link DynamicallyResizedByteOutputStream}.
 *
 * As with typical stream implementations, this stream and its paired output stream are not thread
 * safe.
 *
 * @author zmorin
 *
 */
public class DynamicallyResizedByteInputStream extends InputStream
{
  private final List<byte[]> dataQueue;
  private final DynamicallyResizedByteOutputStream outputStream;

  private byte[] head;
  private int position;
  private boolean endOfStream;
  private int currentArray = 0;
  @Setter(AccessLevel.PACKAGE)
  @Getter
  private int length;
  private volatile boolean closed;

  DynamicallyResizedByteInputStream(@NonNull final DynamicallyResizedByteOutputStream outputStream)
  {
    this.outputStream = outputStream;
    dataQueue = outputStream.getDataQueue();
  }

  @Override
  public int read() throws IOException
  {
    if (closed)
    {
      throw new IOException("This stream was closed before read was complete.");
    }

    if (head == null)
    {
      if (dataQueue.size() > 0 && !endOfStream)
      {
        head = dataQueue.get(currentArray++);
      }
      else
      {
        // This is the case where someone tries to read from an empty input stream.
        endOfStream = true;
        head = new byte[0];
      }
    }

    final int val = (endOfStream) || length-- <= 0 ? -1 : (head[position++] & 0xff);
    if (position >= head.length)
    {
      head = dataQueue.size() < currentArray || length <= 0 ? null : dataQueue.get(currentArray++);
      position = 0;
      if (head == null)
      {
        endOfStream = true;
      }
    }
    return val;
  }

  /**
   * This returns a fresh/reset output stream. This also signals that you are done reading from this
   * stream as it resets the resources that this is built upon and readies the {@link OutputStream}
   * for reuse.
   */
  public DynamicallyResizedByteOutputStream toOutputStream()
  {
    recycle();
    outputStream.recycle();
    return outputStream;
  }

  /**
   * If you need to stop reading ASAP use this. On the next read this InputStream will throw an
   * {@link IOExeption}.
   */
  @Override
  public void close()
  {
    closed = true;
  }

  private void recycle()
  {
    head = null;
    endOfStream = false;
    length = -1;
    position = 0;
    currentArray = 0;
    closed = false;
  }
}
