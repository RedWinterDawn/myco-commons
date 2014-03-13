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
 * @author zmorin
 * 
 */
public class DynamicallyResizedByteOutputStream extends OutputStream
{
  private final int incrementSize;
  private final DynamicallyResizedByteInputStream inputStream;
  @Getter(AccessLevel.PACKAGE)
  private final List<byte[]> dataQueue;

  private byte[] head;
  private int position;
  private int absolutePosition = 0;

  public DynamicallyResizedByteOutputStream(final int initialSize, final int incrementSize)
  {

    head = new byte[initialSize];
    this.incrementSize = incrementSize;

    dataQueue = Lists.newArrayList();
    dataQueue.add(head);

    position = 0;
    this.inputStream = new DynamicallyResizedByteInputStream(this);
  }

  @Override
  public void write(final int b) throws IOException
  {
    if (position == head.length)
    {
      final byte[] newHead = new byte[incrementSize];
      dataQueue.add(newHead);
      head = newHead;
      absolutePosition += position;
      position = 0;
    }
    head[position++] = (byte) b;
  }

  public int getSize()
  {
    return absolutePosition + position;
  }

  /**
   * This method returns the {@link InputStream} associated with the same resources that this
   * {@link OutputStream} is built on top of. When you call this method you are signaling that you
   * are done writing to this stream.
   */
  public InputStream toInputStream()
  {
    inputStream.setLength(getSize());
    return inputStream;
  }

  /**
   * This method resets the data queue to its original sized array and the various pointers. It
   * holds on to the reference so that we don't have to reallocate a massive array.
   */
  public void reset()
  {
    head = dataQueue.get(0);
    dataQueue.clear();
    dataQueue.add(head);
    position = 0;
    absolutePosition = 0;
  }
}
