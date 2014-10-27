package com.jive.myco.commons.networking;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Predicate;

import com.google.common.base.Throwables;

/**
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public final class NetworkingUtil
{
  /**
   * Fetches an identifier for the machine itself. This doesn't use the standard Java mechanism as
   * it (a) doesn't work a unless the host is configured with correct DNS mapped to IP address, and
   * (b) can block while DNS lookups happen. Instead, we use OS specific mechanisms, falling back to
   * running "hostname". Will need to be ported to windows if we ever want to run there.
   *
   * @return the hostname of the current machine
   */
  public static String getHostname()
  {
    try
    {
      final Process p = Runtime.getRuntime().exec("hostname");
      try (final BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream())))
      {
        return in.readLine().trim();
      }
    }
    catch (final Exception ex)
    {
      throw new RuntimeException("Failed to get hostname");
    }
  }

  /**
   * Retrieves the first non-loopback {@link InetAddress address} with an IPv4 address if any are
   * present.
   *
   * @return an {@link Optional} IPv4 InetAddress
   */
  public static Optional<Inet4Address> getFirstNonLoopbackV4Address()
  {
    try
    {
      return Collections
          .list(NetworkInterface.getNetworkInterfaces())
          .stream()
          .sorted((a, b) -> Integer.compare(a.getIndex(), b.getIndex()))
          .filter(NetworkingUtil::isUpAndNonLoopback)
          .map((iface) -> getV4AddressForInterface(iface.getName()))
          .filter(Predicate.isEqual(null).negate())
          .findFirst();
    }
    catch (final SocketException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Checks whether the given interface is up and is not a loopback device.
   *
   * @param iface
   *          the network interface to check
   * @return true if the interface is not a loopback device
   */
  public static boolean isUpAndNonLoopback(final NetworkInterface iface)
  {
    try
    {
      return iface.isUp() && !iface.isLoopback();
    }
    catch (final Exception ex)
    {
      return false;
    }
  }

  /**
   * Retrieves the IPv4 network address from an interface with the given interface name.
   *
   * @param ifname
   *          the network interface name
   * @return the IPv4 address for the interface if one exists else {@code null}
   */
  public static Inet4Address getV4AddressForInterface(final String ifname)
  {
    try
    {
      final NetworkInterface iface = NetworkInterface.getByName(ifname);

      if (iface == null)
      {
        throw new RuntimeException(String.format("Can't find interface '%s'", ifname));
      }

      for (final InterfaceAddress ifaddr : iface.getInterfaceAddresses())
      {

        if (ifaddr.getAddress() instanceof Inet4Address)
        {
          return (Inet4Address) ifaddr.getAddress();
        }

      }

      return null;
    }
    catch (final SocketException ex)
    {
      throw Throwables.propagate(ex);
    }
  }
}
