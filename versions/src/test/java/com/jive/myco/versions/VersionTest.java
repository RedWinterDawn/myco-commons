package com.jive.myco.versions;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public class VersionTest
{
  @Test
  public void testSimple() throws Exception
  {
    Version version = Version.parseVersion("1.0.0");

    assertEquals(1, version.getMajor());
    assertEquals(0, version.getMinor());
    assertEquals(0, version.getPatch());
    assertNull(version.getQualifier());
  }

  @Test
  public void testWithQualifier() throws Exception
  {
    Version version = Version.parseVersion("1.0.0-SNAPSHOT");

    assertEquals(1, version.getMajor());
    assertEquals(0, version.getMinor());
    assertEquals(0, version.getPatch());
    assertEquals("SNAPSHOT", version.getQualifier());
  }

  @Test
  public void testWithoutAllParts() throws Exception
  {
    Version version = Version.parseVersion("5.5-FOO");

    assertEquals(5, version.getMajor());
    assertEquals(5, version.getMinor());
    assertEquals(0, version.getPatch());
    assertEquals("FOO", version.getQualifier());
  }

  @Test
  public void testMajorOnly() throws Exception
  {
    Version version = Version.parseVersion("17");

    assertEquals(17, version.getMajor());
    assertEquals(0, version.getMinor());
    assertEquals(0, version.getPatch());
    assertNull(version.getQualifier());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidChars() throws Exception
  {
    Version.parseVersion("1.0.0a-skdj");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmpty() throws Exception
  {
    Version.parseVersion("");
  }

  @Test(expected = NullPointerException.class)
  public void testNull() throws Exception
  {
    Version.parseVersion(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeVersion() throws Exception
  {
    Version.parseVersion("-1.0.0");
  }

}
