package com.jive.myco.commons.io;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.nio.ByteBuffer;

import org.junit.Test;

public class ByteBufferInputStreamTest
{

  @Test
  public void test() throws Exception
  {
    final String string = "This is my test string and it should come out like it went in...";
    final byte[] bytes = string.getBytes();
    final ByteBuffer buffer = ByteBuffer.wrap(bytes);

    try (final InputStream inputStream = new ByteBufferInputStream(buffer))
    {
      assertEquals(bytes.length, inputStream.available());

      byte[] readBytes = new byte[5];

      assertEquals(1, inputStream.read(readBytes, 0, 1));
      assertEquals(bytes[0], readBytes[0]);

      assertEquals(5, inputStream.read(readBytes, 0, 5));
      assertEquals(bytes[1], readBytes[0]);
      assertEquals(bytes[2], readBytes[1]);
      assertEquals(bytes[3], readBytes[2]);
      assertEquals(bytes[4], readBytes[3]);
      assertEquals(bytes[5], readBytes[4]);

      readBytes = new byte[1024];

      assertEquals(bytes.length - 6, inputStream.read(readBytes, 0, 1024));
      for (int i = 0; i < bytes.length - 6; i++)
      {
        assertEquals(bytes[i + 6], readBytes[i]);
      }
    }
  }
}
