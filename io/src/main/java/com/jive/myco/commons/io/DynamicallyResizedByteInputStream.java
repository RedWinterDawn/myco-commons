package com.jive.myco.commons.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * This {@code InputStream} is built on top of backing byte arrays that were filled by a paired
 * {@link DynamicallyResizedByteOutputStream}. Acquire instances of this class through an instance
 * of {@link DynamicallyResizedByteOutputStream}.
 *
 * As with typical stream implementations, this stream and its paired output stream are not thread
 * safe.
 *
 * @author Zach Morin
 * @author David Valeri
 *
 * @see DynamicallyResizedByteOutputStream
 */
public class DynamicallyResizedByteInputStream extends InputStream
{
  /**
   * The size in bytes of each additional increment allocated in the stream.
   */
  private final int incrementSize;

  /**
   * List of buffers to work with.
   */
  private final List<byte[]> bufferQueue;

  /**
   * The current buffer in use.
   *
   * @See {@link #bufferQueueIndex}
   * @See {@link #bufferIndex}
   */
  private byte[] buffer;

  /**
   * Index of the buffer in the data queue with which we are currently working.
   *
   * @See {@link #buffer}
   */
  private int bufferQueueIndex;

  /**
   * The zero based index in the current buffer.
   */
  private int bufferIndex;

  /**
   * The number of bytes remaining in the underlying buffers.
   */
  private int remaining;

  /**
   * The index of the buffer in the data queue with which we are currently working when
   * {@link #mark(int)} was last called.
   */
  private int markedBufferQueueIndex;

  /**
   * The zero based index in the current buffer when {@link #mark(int)} was last called.
   */
  private int markedBufferIndex;

  /**
   * The number of bytes remaining in the underlying buffers when {@link #mark(int)} was last
   * called.
   */
  private int markedRemaining;

  /**
   * The buffer current buffer in use when {@link #mark(int)} was last called.
   */
  private byte[] markedBuffer;

  /**
   * Flag indicating if the stream is closed.
   */
  private boolean closed;

  DynamicallyResizedByteInputStream(final int incrementSize, final List<byte[]> bufferQueue,
      final int size)
  {
    this.incrementSize = incrementSize;
    this.bufferQueue = bufferQueue;
    this.remaining = size;
    mark(Integer.MAX_VALUE);
  }

  @Override
  public int read() throws IOException
  {
    if (closed)
    {
      throw new IOException("Stream closed.");
    }

    int val = -1;

    if (buffer == null)
    {
      if (bufferQueue.size() > 0)
      {
        buffer = bufferQueue.get(bufferQueueIndex++);
      }
    }

    // If we are not in an empty stream
    if (buffer != null)
    {
      val = remaining-- <= 0 ? -1 : (buffer[bufferIndex++] & 0xff);

      if (bufferIndex >= buffer.length && remaining > 0)
      {
        buffer = bufferQueue.size() < bufferQueueIndex ? null : bufferQueue.get(bufferQueueIndex++);
        bufferIndex = 0;
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
    closed = true;
    return new DynamicallyResizedByteOutputStream(incrementSize, bufferQueue);
  }

  /**
   * Closes the stream, preventing further read operations.
   */
  @Override
  public void close()
  {
    closed = true;
  }

  @Override
  public int available()
  {
    return remaining;
  }

  @Override
  public boolean markSupported()
  {
    return true;
  }

  @Override
  public synchronized void mark(final int readlimit)
  {
    if (!closed)
    {
      markedBufferQueueIndex = bufferQueueIndex;
      markedBufferIndex = bufferIndex;
      markedRemaining = remaining;
      markedBuffer = buffer;
    }
  }

  @Override
  public synchronized void reset() throws IOException
  {
    bufferQueueIndex = markedBufferQueueIndex;
    bufferIndex = markedBufferIndex;
    remaining = markedRemaining;
    buffer = markedBuffer;
  }
}
