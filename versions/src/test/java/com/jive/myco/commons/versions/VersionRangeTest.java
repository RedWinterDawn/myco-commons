package com.jive.myco.commons.versions;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public class VersionRangeTest
{
  @Test
  public void testExactVersion() throws Exception
  {
    final VersionRange range = new VersionRange("1.0.0");
    assertTrue(range.isInRange("1.0.0"));
    assertFalse(range.isInRange("1.0.1"));
    assertFalse(range.isInRange("0.9.9"));
  }

  @Test
  public void testSinglePatchRange() throws Exception
  {
    VersionRange range = new VersionRange("[1.0.0,1.0.1]");

    assertTrue(range.isInRange("1.0.0"));
    assertTrue(range.isInRange("1.0.1"));
    assertFalse(range.isInRange("1.0.2"));

    range = new VersionRange("[1.0.0,1.0.2)");

    assertTrue(range.isInRange("1.0.0"));
    assertTrue(range.isInRange("1.0.1"));
    assertFalse(range.isInRange("1.0.2"));
  }

  @Test
  public void testNoMin() throws Exception
  {
    final VersionRange range = new VersionRange("[,1.20.100]");

    assertTrue(range.isInRange("1.0.0"));
    assertTrue(range.isInRange("1.20.100"));
    assertTrue(range.isInRange("1.10.1"));
    assertTrue(range.isInRange("1.5.100000"));
    assertTrue(range.isInRange("0.0.0"));
    assertTrue(range.isInRange("0.10.10"));
    assertTrue(range.isInRange("1"));
    assertTrue(range.isInRange("0"));
    assertTrue(range.isInRange("0.50.10000"));

    assertFalse(range.isInRange("2"));
    assertFalse(range.isInRange("1.21.1"));
    assertFalse(range.isInRange("1.20.101"));
  }

  @Test
  public void testNoMax() throws Exception
  {
    final VersionRange range = new VersionRange("(25,)");

    assertFalse(range.isInRange("10"));
    assertFalse(range.isInRange("0"));
    assertFalse(range.isInRange("24.99.99"));
    assertFalse(range.isInRange("25"));
    assertFalse(range.isInRange("25.0.0"));

    assertTrue(range.isInRange("26.0.0"));
    assertTrue(range.isInRange("25.1.1"));
    assertTrue(range.isInRange("1000"));
    assertTrue(range.isInRange("25.0.1"));
  }

  @Test
  public void testMissingStuff() throws Exception
  {
    try
    {
      new VersionRange("(10,100");
      fail();
    }
    catch (final IllegalArgumentException e)
    {
      // Expected
    }

    try
    {
      new VersionRange("10,100)");
      fail();
    }
    catch (final IllegalArgumentException e)
    {
      // Expected
    }

    try
    {
      new VersionRange("[10,100");
      fail();
    }
    catch (final IllegalArgumentException e)
    {
      // Expected
    }

    try
    {
      new VersionRange("10,100]");
      fail();
    }
    catch (final IllegalArgumentException e)
    {
      // Expected
    }
  }

  @Test
  public void testAnyVersion() throws Exception
  {
    VersionRange range = new VersionRange("[,]");

    assertTrue(range.isInRange("1"));
    assertTrue(range.isInRange("10.0.20"));
    assertTrue(range.isInRange("0.0.0"));
    assertTrue(range.isInRange("0.0.1"));
    assertTrue(range.isInRange("100000.0.2039"));

    range = new VersionRange("(,)");

    assertTrue(range.isInRange("1"));
    assertTrue(range.isInRange("10.0.20"));
    assertTrue(range.isInRange("0.0.0"));
    assertTrue(range.isInRange("0.0.1"));
    assertTrue(range.isInRange("100000.0.2039"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmpty() throws Exception
  {
    new VersionRange("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNull() throws Exception
  {
    new VersionRange(null);
  }

  @Test
  public void testInvalidChars() throws Exception
  {
    try
    {
      new VersionRange("(a,b)");
      fail();
    }
    catch (final IllegalArgumentException e)
    {
      // Expected
    }

    try
    {
      new VersionRange("ssd");
      fail();
    }
    catch (final IllegalArgumentException e)
    {
      // Expected
    }
  }
}
