package com.jive.myco.commons.networking;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.function.Predicate;

/**
 * Common {@link Predicate filters} that can be used to filter out certain network addresses.
 * Multiple filters may be chained together using {@link Predicate#and(Predicate)} or
 * {@link Predicate#or(Predicate)}.
 *
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public final class AddressFilters
{
  public static final CIDR CLASS_A_PRIVATE = CIDR.newCIDR("10.0.0.0/8");
  public static final CIDR CLASS_B_PRIVATE = CIDR.newCIDR("172.16.0.0/12");
  public static final CIDR CLASS_C_PRIVATE = CIDR.newCIDR("192.168.0.0/16");
  public static final CIDR CLASS_D_MULTICAST = CIDR.newCIDR("224.0.0.0/4");
  public static final CIDR LOOPBACK = CIDR.newCIDR("127.0.0.0/8");
  public static final CIDR LINK_LOCAL = CIDR.newCIDR("169.254.0/16");

  public static Predicate<InetAddress> inRange(final CIDR cidr)
  {
    return (addr) ->
    {
      try
      {
        return cidr.contains(addr);
      }
      catch (final IllegalArgumentException e)
      {
        return false;
      }
    };
  }

  public static Predicate<InetAddress> inRange(final String cidr)
  {
    return inRange(CIDR.newCIDR(cidr));
  }

  public static Predicate<InetAddress> ip4()
  {
    return (addr) -> addr instanceof Inet4Address;
  }

  public static Predicate<InetAddress> ip6()
  {
    return (addr) -> addr instanceof Inet6Address;
  }

  public static Predicate<InetAddress> localOnly()
  {
    return inRange(LOOPBACK);
  }

  public static Predicate<InetAddress> privateOnly()
  {
    return inRange(CLASS_A_PRIVATE).or(inRange(CLASS_B_PRIVATE)).or(inRange(CLASS_C_PRIVATE));
  }

  public static Predicate<InetAddress> publicOnly()
  {
    // TODO: probably some other ranges we should exclude...
    // TODO: for now I am only going to filter for ipv4 public addresses to avoid local IPv6 addrs
    return ip4().and((inRange(LOOPBACK)
        .or(inRange(LINK_LOCAL))
        .or(inRange(CLASS_A_PRIVATE))
        .or(inRange(CLASS_B_PRIVATE))
        .or(inRange(CLASS_C_PRIVATE)))
        .negate());
  }
}
