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

/**
 */
public class CIDR4 extends CIDR
{
  /**
   * The integer for the base address
   */
  private int addressInt;

  /**
   * The integer for the end address
   */
  private final int addressEndInt;

  protected CIDR4(final Inet4Address newaddr, final int mask)
  {
    cidrMask = mask;
    addressInt = ipv4AddressToInt(newaddr);
    final int newmask = ipv4PrefixLengthToMask(mask);
    addressInt &= newmask;
    try
    {
      baseAddress = intToIPv4Address(addressInt);
    }
    catch (final UnknownHostException e)
    {
      // this should never happen
    }
    addressEndInt = addressInt + ipv4PrefixLengthToLength(cidrMask) - 1;
  }

  @Override
  public InetAddress getEndAddress()
  {
    try
    {
      return intToIPv4Address(addressEndInt);
    }
    catch (final UnknownHostException e)
    {
      // this should never happen
      return null;
    }
  }

  public int compareTo(final CIDR arg)
  {
    if (arg instanceof CIDR6)
    {
      final byte[] address = getIpV4FromIpV6((Inet6Address) arg.baseAddress);
      final int net = ipv4AddressToInt(address);
      if (net == addressInt && arg.cidrMask == cidrMask)
      {
        return 0;
      }
      if (net < addressInt)
      {
        return 1;
      }
      if (net > addressInt)
      {
        return -1;
      }
      if (arg.cidrMask < cidrMask)
      {
        return -1;
      }
      return 1;
    }
    final CIDR4 o = (CIDR4) arg;
    if (o.addressInt == addressInt && o.cidrMask == cidrMask)
    {
      return 0;
    }
    if (o.addressInt < addressInt)
    {
      return 1;
    }
    if (o.addressInt > addressInt)
    {
      return -1;
    }
    if (o.cidrMask < cidrMask)
    {
      // greater Mask means less IpAddresses so -1
      return -1;
    }
    return 1;
  }

  @Override
  public boolean contains(final InetAddress inetAddress)
  {
    final int search = ipv4AddressToInt(inetAddress);
    return search >= addressInt && search <= addressEndInt;
  }

  /**
   * Given an IPv4 baseAddress length, return the block length. I.e., a baseAddress length of 24
   * will return 256.
   */
  private static int ipv4PrefixLengthToLength(final int prefixLength)
  {
    return 1 << 32 - prefixLength;
  }

  /**
   * Given a baseAddress length, return a netmask. I.e, a baseAddress length of 24 will return
   * 0xFFFFFF00.
   */
  private static int ipv4PrefixLengthToMask(final int prefixLength)
  {
    return ~((1 << 32 - prefixLength) - 1);
  }

  /**
   * Convert an integer into an (IPv4) InetAddress.
   *
   * @return the created InetAddress
   */
  private static InetAddress intToIPv4Address(final int addr) throws UnknownHostException
  {
    final byte[] a = new byte[4];
    a[0] = (byte) (addr >> 24 & 0xFF);
    a[1] = (byte) (addr >> 16 & 0xFF);
    a[2] = (byte) (addr >> 8 & 0xFF);
    a[3] = (byte) (addr & 0xFF);
    return InetAddress.getByAddress(a);
  }

  /**
   * Given an IPv4 address, convert it into an integer.
   *
   * @return the integer representation of the InetAddress
   *
   * @throws IllegalArgumentException
   *           if the address is really an IPv6 address.
   */
  private static int ipv4AddressToInt(final InetAddress addr)
  {
    final byte[] address;
    if (addr instanceof Inet6Address)
    {
      address = getIpV4FromIpV6((Inet6Address) addr);
    }
    else
    {
      address = addr.getAddress();
    }
    return ipv4AddressToInt(address);
  }

  /**
   * Given an IPv4 address as array of bytes, convert it into an integer.
   *
   * @return the integer representation of the InetAddress
   *
   * @throws IllegalArgumentException
   *           if the address is really an IPv6 address.
   */
  private static int ipv4AddressToInt(final byte[] address)
  {
    int net = 0;
    for (final byte addres : address)
    {
      net <<= 8;
      net |= addres & 0xFF;
    }
    return net;
  }
}
