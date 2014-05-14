package com.jive.myco.commons.concurrent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides some additional utility methods for use with an {@link AtomicInteger}.
 *
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public class AtomicIntegerEx extends AtomicInteger
{
  private static final long serialVersionUID = -199941484526666599L;

  public AtomicIntegerEx()
  {
    super();
  }

  public AtomicIntegerEx(int initialValue)
  {
    super(initialValue);
  }

  /**
   * Set this guys value iff the provided value is greater than the current value.
   *
   * @param value
   *     the new value to set
   *
   * @return true if the provided value was greater than the current value and was set as the new
   *         value
   */
  public boolean setIfGreater(int value)
  {
    int current;
    while (true)
    {
      current = get();

      if (value > current)
      {
        if (compareAndSet(current, value))
        {
          return true;
        }
      }
      else
      {
        return false;
      }
    }
  }
}
