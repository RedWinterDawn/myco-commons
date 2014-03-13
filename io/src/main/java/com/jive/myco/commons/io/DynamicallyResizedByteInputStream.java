package com.jive.myco.commons.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import lombok.AccessLevel;
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

  private byte[] head;
  private int position;
  private boolean endOfStream;
  private final DynamicallyResizedByteOutputStream outputStream;
  private int currentArray = 0;
  @Setter(AccessLevel.PACKAGE)
  private int length;

  DynamicallyResizedByteInputStream(final DynamicallyResizedByteOutputStream outputStream)
  {
    this.outputStream = outputStream;
    dataQueue = outputStream.getDataQueue();
  }

  @Override
  public int read() throws IOException
  {
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
  public OutputStream toOutputStream()
  {
    reset();
    outputStream.reset();
    return outputStream;
  }

  @Override
  public void reset()
  {
    head = null;
    endOfStream = false;
    length = -1;
    position = 0;
    currentArray = 0;
  }
}
