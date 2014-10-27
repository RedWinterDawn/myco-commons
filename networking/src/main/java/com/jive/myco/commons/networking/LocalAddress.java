package com.jive.myco.commons.networking;

import static com.jive.myco.commons.networking.AddressFilters.*;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.Synchronized;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.net.InetAddresses;

/**
 * Utilities for working with the inet addresses of the local machine
 *
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public final class LocalAddress
{
  public static final InetAddress DEFAULT_LOCAL_ADDRESS = InetAddresses.forString("127.0.0.1");

  private static List<InetAddress> allAddresses = null;

  public static Optional<InetAddress> getLoopback()
  {
    return getLoopback(true);
  }

  public static Optional<InetAddress> getLoopback(final boolean ip4)
  {
    // if they wanted 127.0.0.1 they should just grab the variable themselves from above
    return fromType(ip4).filter(localOnly()).findFirst();
  }

  public static Optional<InetAddress> getDefaultPrivateAddress()
  {
    return getDefaultPrivateAddress(true);
  }

  public static Optional<InetAddress> getDefaultPrivateAddress(final boolean ip4)
  {
    return fromType(ip4).filter(privateOnly()).findFirst();
  }

  public static Optional<InetAddress> getDefaultPublicAddress()
  {
    return getDefaultPublicAddress(true);
  }

  public static Optional<InetAddress> getDefaultPublicAddress(final boolean ip4)
  {
    return fromType(ip4).filter(publicOnly()).findFirst();
  }

  private static Stream<InetAddress> fromType(final boolean ip4)
  {
    final Stream<InetAddress> addressStream = getAllLocalAddresses().stream();

    if (ip4)
    {
      return addressStream.filter(ip4());
    }
    else
    {
      return addressStream.filter(ip6());
    }
  }

  @Synchronized
  public static void clearCache()
  {
    allAddresses = null;
  }

  @Synchronized
  public static List<InetAddress> getAllLocalAddresses()
  {
    if (allAddresses == null)
    {
      final Builder<InetAddress> addressBuilder = ImmutableList.builder();
      final Enumeration<NetworkInterface> interfaces;
      try
      {
        interfaces = NetworkInterface.getNetworkInterfaces();
      }
      catch (final SocketException e)
      {
        throw new RuntimeException(e);
      }

      while (interfaces.hasMoreElements())
      {
        final NetworkInterface networkInterface = interfaces.nextElement();
        final Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
        while (inetAddresses.hasMoreElements())
        {
          addressBuilder.add(inetAddresses.nextElement());
        }
      }
      allAddresses = addressBuilder.build();
    }
    return allAddresses;
  }
}
