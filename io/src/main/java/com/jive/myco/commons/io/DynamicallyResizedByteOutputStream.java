package com.jive.myco.commons.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;

import com.google.common.collect.Lists;

/**
 * This {@code OutputStream} is backed by a byte array that is initially sized in its constructor.
 * As the stream is filled, additional byte arrays are added to accommodate the additional data.
 * These additional byte arrays solve the problem of having to double the size of the initial byte
 * array and memory copying as in {@link ByteArrayOutputStream} when you only need a fraction of the
 * space.
 * 
 * This class also provides a reference to an {@code InputStream} that is built on top of the same
 * backing arrays so that you don't have to go around copying bytes like some kind of un-optimized
 * barbarian.
 * 
 * As with typical stream implementations, this stream and its paired output stream are not thread
 * safe.
 * 
 * Typical usage pattern is as follows:
 * 
 * <pre>
 * 
 * DynamicallyResizedByteOutputStream outputStream =
 *     new DynamicallyResizedByteOutputStream(1024 * 4, 1024);
 * 
 * outputStream.write(...);
 * 
 * // Recycle for reading
 * InputStream inputStream = outputStream.getInputStream();
 * 
 * inputStream.read(...);
 * 
 * // Recycle for writing
 * outputStream = inputStream.getOutputStream();
 * 
 * ...
 * </pre>
 * 
 * @author Zach Morin
 * @author David Valer
 * 
 */
public class DynamicallyResizedByteOutputStream extends OutputStream
{
  /**
   * The size in bytes of each additional increment allocated in the stream.
   */
  private final int incrementSize;

  /**
   * List of buffers to work with.
   */
  @Getter(AccessLevel.PACKAGE)
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
   * Total, zero based, index of the entire stream.
   */
  private int absoluteIndex;

  /**
   * Flag indicating if the stream is closed.
   */
  private boolean closed;


  public DynamicallyResizedByteOutputStream(final int initialSize, final int incrementSize)
  {
    this.incrementSize = incrementSize;
    bufferQueue = Lists.newArrayList(new byte[initialSize]);
    reset();
  }
  
  DynamicallyResizedByteOutputStream(final int incrementSize, final List<byte[]> bufferQueue)
  {
    this.incrementSize = incrementSize;
    this.bufferQueue = bufferQueue;
    reset();
  }

  @Override
  public void write(final int b) throws IOException
  {
    checkClosed();

    if (bufferIndex == buffer.length)
    {
      if (bufferQueueIndex == bufferQueue.size() - 1)
      {
        final byte[] newBuffer = new byte[incrementSize];
        bufferQueue.add(newBuffer);
        buffer = newBuffer;
        bufferQueueIndex++;
      }
      else
      {
        buffer = bufferQueue.get(++bufferQueueIndex);
      }

      absoluteIndex += bufferIndex;
      bufferIndex = 0;
    }

    buffer[bufferIndex++] = (byte) b;
  }

  /**
   * Closes the stream and prevents further writes.
   */
  @Override
  public void close() throws IOException
  {
    closed = true;
  }

  public int getSize()
  {
    return absoluteIndex + bufferIndex;
  }

  /**
   * This method returns the {@link InputStream} associated with the same resources that this
   * {@link OutputStream} is built on top of. When you call this method you are signaling that you
   * are done writing to this stream.
   * <p/>
   * This stream is closed once this method returns.
   */
  public DynamicallyResizedByteInputStream toInputStream()
  {
    closed = true;
    return new DynamicallyResizedByteInputStream(incrementSize, bufferQueue, getSize());
  }

  /**
   * Resets the internal pointers and readies the stream for writing. Call this method if you wish
   * to reset the output stream to an empty state and start filling it again without calling
   * {@link #toInputStream()}.
   * 
   * @throws IllegalStateException
   *           if the stream is closed or {@link #toInputStream()} has been called.
   */
  public void recycle()
  {
    if (closed)
    {
      throw new IllegalStateException("Stream closed.");
    }

    reset();
  }

  private void reset()
  {
    buffer = bufferQueue.get(0);
    bufferIndex = 0;
    absoluteIndex = 0;
    bufferQueueIndex = 0;
  }

  private void checkClosed() throws IOException
  {
    if (closed)
    {
      throw new IOException("Stream closed.");
    }
  }
}
