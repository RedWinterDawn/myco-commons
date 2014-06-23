package com.jive.myco.commons.concurrent;

import java.util.List;

import lombok.Getter;

/**
 * Exception that is the combination of multiple exceptions. Used with {@link Pnky} to represent the
 * set of exceptions that occurred from watching the results of multiple {@link PnkyPromise futures}
 *
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public class CombinedException extends Exception
{
  @Getter
  private final List<? extends Throwable> causes;

  public CombinedException(final List<? extends Throwable> causes)
  {
    this.causes = causes;
  }
}
