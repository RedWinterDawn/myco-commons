package com.jive.myco.commons.io;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Test;

public class DynamicallyResizedByteInputOutputStreamTest
{

  private static final int INITIAL_SIZE = 50;
  private static final int INCREMENT_SIZE = 10;
  private DynamicallyResizedByteOutputStream outputStream;
  private List<byte[]> backingStore;

  private final String fiftyBytes = "This is supposed to be 50 bytes long ... and it is";

  @Before
  public void setUp() throws Exception
  {
    outputStream = new DynamicallyResizedByteOutputStream(INITIAL_SIZE, INCREMENT_SIZE);
    backingStore = outputStream.getBufferQueue();
  }

  @Test
  public void testOutputResize() throws Exception
  {
    assertEquals(1, backingStore.size());
    outputStream.write(fiftyBytes.getBytes());
    // Fill up the initial array and check that we haven't allocated any new space until we need
    // it.
    assertEquals(1, backingStore.size());
    outputStream.write("1".getBytes());
    assertEquals(2, backingStore.size());
    outputStream.write("234567890".getBytes());
    assertEquals(2, backingStore.size());
    outputStream.write("1".getBytes());
    assertEquals(3, backingStore.size());
  }

  @Test
  public void testWriteSmallThenRead() throws Exception
  {
    assertEquals(1, backingStore.size());
    final String writenToBuffer = "Test bytes";
    outputStream.write(writenToBuffer.getBytes());

    final InputStream inputStream = outputStream.toInputStream();
    final String val = convertStreamToString(inputStream);

    assertEquals(writenToBuffer, val);
  }

  @Test
  public void testWriteFullThenRead() throws Exception
  {
    assertEquals(1, backingStore.size());
    outputStream.write(fiftyBytes.getBytes());

    final InputStream inputStream = outputStream.toInputStream();
    final String val = convertStreamToString(inputStream);

    assertEquals(fiftyBytes, val);
  }

  @Test
  public void testWriteOverThenRead() throws Exception
  {
    final String writtenToBuffer =
        "This is supposed to be 50 bytes long ... and it is unless it isn't";
    assertEquals(1, backingStore.size());
    outputStream.write(writtenToBuffer.getBytes());
    final InputStream inputStream = outputStream.toInputStream();
    final String val = convertStreamToString(inputStream);

    assertEquals(writtenToBuffer, val);
  }

  @Test
  public void testWriteReadWriteReadDataIntegrity() throws Exception
  {
    final String writtenToBuffer =
        "This is supposed to be 50 bytes long ... and it is unless it isn't";
    assertEquals(1, backingStore.size());
    outputStream.write(writtenToBuffer.getBytes());
    DynamicallyResizedByteInputStream inputStream =
        outputStream.toInputStream();

    String val = convertStreamToString(inputStream);

    assertEquals(writtenToBuffer, val);

    outputStream = inputStream.toOutputStream();
    assertEquals(3, backingStore.size());
    final String rewritten = "This is much shorter than the last one";
    outputStream.write(rewritten.getBytes());
    inputStream = outputStream.toInputStream();

    val = convertStreamToString(inputStream);
    assertEquals(rewritten, val);

    outputStream = inputStream.toOutputStream();
    assertEquals(3, backingStore.size());
    final String reRewritten = writtenToBuffer + ". This is much larger than the last one";
    outputStream.write(reRewritten.getBytes());
    inputStream = outputStream.toInputStream();

    val = convertStreamToString(inputStream);
    assertEquals(reRewritten, val);

    assertEquals(7, backingStore.size());
  }

  @Test
  public void testEmpty() throws Exception
  {
    assertTrue(convertStreamToString(outputStream.toInputStream()).isEmpty());
  }

  @Test
  public void testRecycle() throws Exception
  {
    outputStream.write("Test bytes".getBytes());
    outputStream.recycle();

    final String writenToBuffer = "More Test Bytes";
    outputStream.write(writenToBuffer.getBytes());

    final InputStream inputStream = outputStream.toInputStream();
    final String val = convertStreamToString(inputStream);

    assertEquals(writenToBuffer, val);
  }

  public void testClosed() throws Exception
  {
    outputStream.write("Test bytes".getBytes());
    outputStream.close();

    try
    {
      outputStream.write("Test bytes".getBytes());
      fail();
    }
    catch (final IOException e)
    {
      // Expected
    }

    try
    {
      outputStream.recycle();
      fail();
    }
    catch (final IllegalStateException e)
    {
      // Expected
    }

    final InputStream inputStream = outputStream.toInputStream();
    inputStream.close();

    try
    {
      inputStream.read();
      fail();
    }
    catch (final IOException e)
    {
      // Expected
    }
  }

  @Test
  public void testReset() throws Exception
  {
    final String writtenToBuffer =
        "12345678901234567890123456789012345678901234567890123456789012345678901234567890";
    outputStream.write(writtenToBuffer.getBytes());

    final DynamicallyResizedByteInputStream inputStream =
        outputStream.toInputStream();

    assertEquals(writtenToBuffer, convertStreamToString(inputStream));

    // Full stream reset
    inputStream.reset();
    assertEquals(writtenToBuffer, convertStreamToString(inputStream));

    // Mid stream mark and reset
    inputStream.reset();

    inputStream.read(); // Trim one from the beginning
    inputStream.mark(500); // Mark the trimmed stream
    assertEquals(writtenToBuffer.substring(1), convertStreamToString(inputStream));

    inputStream.reset();
    assertEquals(writtenToBuffer.substring(1), convertStreamToString(inputStream));

    // Mid stream mark and reset 2
    inputStream.reset();

    inputStream.read(); // Trim one from the beginning
    inputStream.mark(500); // Mark the trimmed stream
    inputStream.reset(); // Reset to the position where we already are
    assertEquals(writtenToBuffer.substring(2), convertStreamToString(inputStream));

    inputStream.reset();
    assertEquals(writtenToBuffer.substring(2), convertStreamToString(inputStream));

    // End of stream mark
    inputStream.mark(500);
    inputStream.reset();

    assertTrue(convertStreamToString(inputStream).isEmpty());
  }

  @SuppressWarnings("resource")
  private static String convertStreamToString(final InputStream is)
  {
    final Scanner s = new Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }
}