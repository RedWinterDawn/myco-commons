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
    backingStore = outputStream.getDataQueue();
  }

  @Test
  public void testOutputResize() throws IOException
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
  public void testWriteSmallThenRead() throws IOException
  {
    assertEquals(1, backingStore.size());
    final String writenToBuffer = "Test bytes";
    outputStream.write(writenToBuffer.getBytes());

    final InputStream inputStream = outputStream.toInputStream();
    final String val = convertStreamToString(inputStream);

    assertEquals(writenToBuffer, val);
  }

  @Test
  public void testWriteFullThenRead() throws IOException
  {
    assertEquals(1, backingStore.size());
    outputStream.write(fiftyBytes.getBytes());

    final InputStream inputStream = outputStream.toInputStream();
    final String val = convertStreamToString(inputStream);

    assertEquals(fiftyBytes, val);
  }

  @Test
  public void testWriteOverThenRead() throws IOException
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
  public void testRecycledDataIntegrity() throws IOException
  {
    final String writtenToBuffer =
        "This is supposed to be 50 bytes long ... and it is unless it isn't";
    assertEquals(1, backingStore.size());
    outputStream.write(writtenToBuffer.getBytes());
    DynamicallyResizedByteInputStream inputStream =
        (DynamicallyResizedByteInputStream) outputStream.toInputStream();

    String val = convertStreamToString(inputStream);

    assertEquals(writtenToBuffer, val);

    outputStream = (DynamicallyResizedByteOutputStream) inputStream.toOutputStream();
    assertEquals(3, backingStore.size());
    final String rewritten = "This is much shorter than the last one";
    outputStream.write(rewritten.getBytes());
    inputStream = (DynamicallyResizedByteInputStream) outputStream.toInputStream();

    val = convertStreamToString(inputStream);
    assertEquals(rewritten, val);

    outputStream = (DynamicallyResizedByteOutputStream) inputStream.toOutputStream();
    assertEquals(3, backingStore.size());
    final String reRewritten = writtenToBuffer + ". This is much larger than the last one";
    outputStream.write(reRewritten.getBytes());
    inputStream = (DynamicallyResizedByteInputStream) outputStream.toInputStream();

    val = convertStreamToString(inputStream);
    assertEquals(reRewritten, val);

    assertEquals(7, backingStore.size());
  }

  @SuppressWarnings("resource")
  private static String convertStreamToString(final InputStream is)
  {
    final Scanner s = new Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }
}