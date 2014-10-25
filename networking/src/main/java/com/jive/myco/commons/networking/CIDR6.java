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

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import lombok.extern.slf4j.Slf4j;

/**
 */
@Slf4j
public class CIDR6 extends CIDR
{
  /**
   * The big integer for the base address
   */
  private BigInteger addressBigInt;

  /**
   * The big integer for the end address
   */
  private final BigInteger addressEndBigInt;

  protected CIDR6(final Inet6Address newaddress, final int newmask)
  {
    cidrMask = newmask;
    addressBigInt = ipv6AddressToBigInteger(newaddress);
    final BigInteger mask = ipv6CidrMaskToMask(newmask);
    try
    {
      addressBigInt = addressBigInt.and(mask);
      baseAddress = bigIntToIPv6Address(addressBigInt);
    }
    catch (final UnknownHostException e)
    {
      // this should never happen.
    }
    addressEndBigInt =
        addressBigInt.add(ipv6CidrMaskToBaseAddress(cidrMask)).subtract(BigInteger.ONE);
  }

  @Override
  public InetAddress getEndAddress()
  {
    try
    {
      return bigIntToIPv6Address(addressEndBigInt);
    }
    catch (final UnknownHostException e)
    {
      log.error("invalid ip address calculated as an end address");
      return null;
    }
  }

  public int compareTo(final CIDR arg)
  {
    if (arg instanceof CIDR4)
    {
      final BigInteger net = ipv6AddressToBigInteger(arg.baseAddress);
      final int res = net.compareTo(addressBigInt);
      if (res == 0)
      {
        if (arg.cidrMask == cidrMask)
        {
          return 0;
        }
        if (arg.cidrMask < cidrMask)
        {
          return -1;
        }
        return 1;
      }
      return res;
    }
    final CIDR6 o = (CIDR6) arg;
    if (o.addressBigInt.equals(addressBigInt) && o.cidrMask == cidrMask)
    {
      return 0;
    }
    final int res = o.addressBigInt.compareTo(addressBigInt);
    if (res == 0)
    {
      if (o.cidrMask < cidrMask)
      {
        // greater Mask means less IpAddresses so -1
        return -1;
      }
      return 1;
    }
    return res;
  }

  @Override
  public boolean contains(final InetAddress inetAddress)
  {
    final BigInteger search = ipv6AddressToBigInteger(inetAddress);
    return search.compareTo(addressBigInt) >= 0 && search.compareTo(addressEndBigInt) <= 0;
  }

  /**
   * Given an IPv6 baseAddress length, return the block length. I.e., a baseAddress length of 96
   * will return 2**32.
   */
  private static BigInteger ipv6CidrMaskToBaseAddress(final int cidrMask)
  {
    return BigInteger.ONE.shiftLeft(128 - cidrMask);
  }

  private static BigInteger ipv6CidrMaskToMask(final int cidrMask)
  {
    return BigInteger.ONE.shiftLeft(128 - cidrMask).subtract(BigInteger.ONE).not();
  }

  /**
   * Given an IPv6 address, convert it into a BigInteger.
   *
   * @return the integer representation of the InetAddress
   *
   * @throws IllegalArgumentException
   *           if the address is not an IPv6 address.
   */
  private static BigInteger ipv6AddressToBigInteger(final InetAddress addr)
  {
    final byte[] ipv6;
    if (addr instanceof Inet4Address)
    {
      ipv6 = getIpV6FromIpV4((Inet4Address) addr);
    }
    else
    {
      ipv6 = addr.getAddress();
    }
    if (ipv6[0] == -1)
    {
      return new BigInteger(1, ipv6);
    }
    return new BigInteger(ipv6);
  }

  /**
   * Convert a big integer into an IPv6 address.
   *
   * @return the inetAddress from the integer
   *
   * @throws UnknownHostException
   *           if the big integer is too large, and thus an invalid IPv6 address.
   */
  private static InetAddress bigIntToIPv6Address(final BigInteger addr) throws UnknownHostException
  {
    final byte[] a = new byte[16];
    final byte[] b = addr.toByteArray();
    if (b.length > 16 && !(b.length == 17 && b[0] == 0))
    {
      throw new UnknownHostException("invalid IPv6 address (too big)");
    }
    if (b.length == 16)
    {
      return InetAddress.getByAddress(b);
    }
    // handle the case where the IPv6 address starts with "FF".
    if (b.length == 17)
    {
      System.arraycopy(b, 1, a, 0, 16);
    }
    else
    {
      // copy the address into a 16 byte array, zero-filled.
      final int p = 16 - b.length;
      System.arraycopy(b, 0, a, p, b.length);
    }
    return InetAddress.getByAddress(a);
  }
}
