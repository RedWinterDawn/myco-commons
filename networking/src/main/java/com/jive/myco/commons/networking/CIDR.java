/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jive.myco.commons.networking;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

/**
 */
public abstract class CIDR implements Comparable<CIDR>
{
  /**
   * The base address of the CIDR notation
   */
  protected InetAddress baseAddress;

  /**
   * The mask used in the CIDR notation
   */
  protected int cidrMask;

  /**
   * Create CIDR using the CIDR Notation
   *
   * @return the generated CIDR
   */
  public static CIDR newCIDR(final InetAddress baseAddress, final int cidrMask)
  {
    if (cidrMask < 0)
    {
      throw new IllegalArgumentException("Invalid mask length used: " + cidrMask);
    }
    if (baseAddress instanceof Inet4Address)
    {
      if (cidrMask > 32)
      {
        throw new IllegalArgumentException("Invalid mask length used: " + cidrMask);
      }
      return new CIDR4((Inet4Address) baseAddress, cidrMask);
    }
    // IPv6.
    if (cidrMask > 128)
    {
      throw new IllegalArgumentException("Invalid mask length used: " + cidrMask);
    }
    return new CIDR6((Inet6Address) baseAddress, cidrMask);
  }

  /**
   * Create CIDR using the normal Notation
   *
   * @return the generated CIDR
   */
  public static CIDR newCIDR(final InetAddress baseAddress, final String scidrMask)
      throws UnknownHostException
  {
    int cidrMask = getNetMask(scidrMask);
    if (cidrMask < 0)
    {
      throw new UnknownHostException("Invalid mask length used: " + cidrMask);
    }
    if (baseAddress instanceof Inet4Address)
    {
      if (cidrMask > 32)
      {
        throw new UnknownHostException("Invalid mask length used: " + cidrMask);
      }
      return new CIDR4((Inet4Address) baseAddress, cidrMask);
    }
    cidrMask += 96;
    // IPv6.
    if (cidrMask > 128)
    {
      throw new UnknownHostException("Invalid mask length used: " + cidrMask);
    }
    return new CIDR6((Inet6Address) baseAddress, cidrMask);
  }

  /**
   * Create CIDR using the CIDR or normal Notation<BR>
   * i.e.: CIDR subnet = newCIDR ("10.10.10.0/24"); or CIDR subnet = newCIDR
   * ("1fff:0:0a88:85a3:0:0:ac1f:8001/24"); or CIDR subnet = newCIDR ("10.10.10.0/255.255.255.0");
   *
   * @return the generated CIDR
   * @throws IllegalArgumentException
   *           if the provided CIDR string is not valid
   */
  public static CIDR newCIDR(final String cidr)
  {
    final int p = cidr.indexOf('/');
    if (p < 0)
    {
      throw new IllegalArgumentException("Invalid CIDR notation used: " + cidr);
    }
    final String addrString = cidr.substring(0, p);
    final String maskString = cidr.substring(p + 1);
    final InetAddress addr = addressStringToInet(addrString);
    int mask;
    if (maskString.indexOf('.') < 0)
    {
      mask = parseInt(maskString, -1);
    }
    else
    {
      mask = getNetMask(maskString);
      if (addr instanceof Inet6Address)
      {
        mask += 96;
      }
    }
    if (mask < 0)
    {
      throw new IllegalArgumentException("Invalid mask length used: " + maskString);
    }
    return newCIDR(addr, mask);
  }

  /**
   * @return the baseAddress of the CIDR block.
   */
  public InetAddress getBaseAddress()
  {
    return baseAddress;
  }

  /**
   * @return the Mask length.
   */
  public int getMask()
  {
    return cidrMask;
  }

  /**
   * @return the textual CIDR notation.
   */
  @Override
  public String toString()
  {
    return baseAddress.getHostAddress() + '/' + cidrMask;
  }

  /**
   * @return the end address of this block.
   */
  public abstract InetAddress getEndAddress();

  /**
   * Compares the given InetAddress against the CIDR and returns true if the ip is in the
   * subnet-ip-range and false if not.
   *
   * @return returns true if the given IP address is inside the currently set network.
   */
  public abstract boolean contains(InetAddress inetAddress);

  @Override
  public boolean equals(final Object o)
  {
    if (!(o instanceof CIDR))
    {
      return false;
    }
    return compareTo((CIDR) o) == 0;
  }

  @Override
  public int hashCode()
  {
    return baseAddress.hashCode();
  }

  /**
   * Convert an IPv4 or IPv6 textual representation into an InetAddress.
   *
   * @return the created InetAddress
   */
  private static InetAddress addressStringToInet(final String addr)
  {
    try
    {
      return InetAddress.getByName(addr);
    }
    catch (UnknownHostException e)
    {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * Get the Subnet's Netmask in Decimal format.<BR>
   * i.e.: getNetMask("255.255.255.0") returns the integer CIDR mask
   *
   * @param netMask
   *          a network mask
   *
   * @return the integer CIDR mask
   */
  private static int getNetMask(final String netMask)
  {
    final StringTokenizer nm = new StringTokenizer(netMask, ".");
    int i = 0;
    final int[] netmask = new int[4];
    while (nm.hasMoreTokens())
    {
      netmask[i] = Integer.parseInt(nm.nextToken());
      i++;
    }
    int mask1 = 0;
    for (i = 0; i < 4; i++)
    {
      mask1 += Integer.bitCount(netmask[i]);
    }
    return mask1;
  }

  /**
   * @param intstr
   *          a string containing an integer.
   * @param def
   *          the default if the string does not contain a valid integer.
   *
   * @return the inetAddress from the integer
   */
  private static int parseInt(final String intstr, final int def)
  {
    Integer res;
    if (intstr == null)
    {
      return def;
    }
    try
    {
      res = Integer.decode(intstr);
    }
    catch (final Exception e)
    {
      res = def;
    }
    return res.intValue();
  }

  /**
   * Compute a byte representation of IpV4 from a IpV6
   *
   * @return the byte representation
   *
   * @throws IllegalArgumentException
   *           if the IpV6 cannot be mapped to IpV4
   */
  public static byte[] getIpV4FromIpV6(final Inet6Address address)
  {
    final byte[] baddr = address.getAddress();
    for (int i = 0; i < 9; i++)
    {
      if (baddr[i] != 0)
      {
        throw new IllegalArgumentException("This IPv6 address cannot be used in IPv4 context");
      }
    }
    if (baddr[10] != 0 && baddr[10] != 0xFF || baddr[11] != 0 && baddr[11] != 0xFF)
    {
      throw new IllegalArgumentException("This IPv6 address cannot be used in IPv4 context");
    }
    return new byte[]
    { baddr[12], baddr[13], baddr[14], baddr[15] };
  }

  /**
   * Compute a byte representation of IpV6 from a IpV4
   *
   * @return the byte representation
   *
   * @throws IllegalArgumentException
   *           if the IpV6 cannot be mapped to IpV4
   */
  public static byte[] getIpV6FromIpV4(final Inet4Address address)
  {
    final byte[] baddr = address.getAddress();
    return new byte[]
    { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, baddr[0], baddr[1], baddr[2], baddr[3] };
  }
}
