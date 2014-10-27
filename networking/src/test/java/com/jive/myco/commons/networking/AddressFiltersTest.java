package com.jive.myco.commons.networking;

import static com.jive.myco.commons.networking.AddressFilters.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.net.InetAddress;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;

/**
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public class AddressFiltersTest
{
  private static final List<InetAddress> TEST_ADDRESSES =
      Lists.newArrayList(InetAddresses.forString("127.0.0.1"),
          InetAddresses.forString("10.102.53.1"),
          InetAddresses.forString("192.168.2.1"),
          InetAddresses.forString("8.8.8.8"),
          InetAddresses.forString("::1"),
          InetAddresses.forString("fe80::5626:96ff:fed7:94ed"));

  @Test
  public void testLocalOnly() throws Exception
  {
    final List<InetAddress> all = TEST_ADDRESSES.stream()
        .filter(localOnly())
        .collect(Collectors.toList());

    // TODO: should include ::1 as well...just need to add checks for ipv6
    assertThat(all, hasSize(1));
    assertThat(all, hasItem(InetAddresses.forString("127.0.0.1")));
  }

  @Test
  public void testIp4() throws Exception
  {
    final List<InetAddress> all = TEST_ADDRESSES.stream()
        .filter(ip4())
        .collect(Collectors.toList());

    assertThat(all, hasSize(4));
    assertThat(all, hasItems(
        InetAddresses.forString("127.0.0.1"),
        InetAddresses.forString("10.102.53.1"),
        InetAddresses.forString("192.168.2.1"),
        InetAddresses.forString("8.8.8.8")));
  }

  @Test
  public void testIp6() throws Exception
  {
    final List<InetAddress> all = TEST_ADDRESSES.stream()
        .filter(ip6())
        .collect(Collectors.toList());

    assertThat(all, hasSize(2));
    assertThat(all, hasItems(
        InetAddresses.forString("::1"),
        InetAddresses.forString("fe80::5626:96ff:fed7:94ed")));
  }

  @Test
  public void testInRange() throws Exception
  {
    List<InetAddress> all = TEST_ADDRESSES.stream()
        .filter(inRange("10.102.0.0/16"))
        .collect(Collectors.toList());

    assertThat(all, hasSize(1));
    assertThat(all, hasItem(InetAddresses.forString("10.102.53.1")));

    all = TEST_ADDRESSES.stream()
        .filter(inRange("::1/128"))
        .collect(Collectors.toList());

    assertThat(all, hasSize(1));
    assertThat(all, hasItem(InetAddresses.forString("::1")));
  }

  @Test
  public void testNotInRange() throws Exception
  {
    final List<InetAddress> all = TEST_ADDRESSES.stream()
        .filter(inRange("10.102.0.0/16").negate())
        .filter(inRange("::1/128").negate())
        .collect(Collectors.toList());

    assertThat(all, hasSize(4));
    assertThat(all, not(hasItem(InetAddresses.forString("::20"))));
  }

  @Test
  public void testPrivateOnly() throws Exception
  {
    final List<InetAddress> all = TEST_ADDRESSES.stream()
        .filter(privateOnly())
        .collect(Collectors.toList());

    assertThat(all, hasSize(2));
    assertThat(all, hasItems(
        InetAddresses.forString("192.168.2.1"),
        InetAddresses.forString("10.102.53.1")));
  }

  @Test
  public void testPublicOnly() throws Exception
  {
    final List<InetAddress> all = TEST_ADDRESSES.stream()
        .filter(publicOnly())
        .collect(Collectors.toList());

    assertThat(all, hasSize(1));
    assertThat(all, hasItems(InetAddresses.forString("8.8.8.8")));
  }
}
