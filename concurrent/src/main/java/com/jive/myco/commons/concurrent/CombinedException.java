package com.jive.myco.commons.concurrent;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;

/**
 * Exception that is the combination of multiple exceptions. Used with {@link Pnky} to represent the
 * set of exceptions that occurred from watching the results of multiple {@link PnkyPromise futures}
 * .
 *
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public class CombinedException extends Exception
{
  private static final long serialVersionUID = -1359656636853321262L;

  /**
   * The indexed list of causes in which the element at index i corresponds to the cause generated
   * by the future at position i in the original list of futures being observed. May contain
   * {@code null} at any given index.
   */
  @Getter
  private final List<? extends Throwable> causes;

  /**
   * Creates a new combined exception composed of the provided causes and the supplied message.
   *
   * @param message
   *          the detail message
   * @param causes
   *          the indexed list of causes in which the element at index i corresponds to the cause
   *          generated by the future at position i in the original list of futures being observed.
   *          That is, causes may be sparesly populated with {@code null} values in certain
   *          positions.
   */
  public CombinedException(final String message, final List<? extends Throwable> causes)
  {
    super(message);

    if (causes == null)
    {
      this.causes = Collections.emptyList();
    }
    else
    {
      this.causes = Collections.unmodifiableList(new ArrayList<>(causes));
    }
  }

  /**
   * Creates a new combined exception composed of the provided causes.
   *
   * @param causes
   *          the indexed list of causes in which the element at index i corresponds to the cause
   *          generated by the future at position i in the original list of futures being observed.
   *          That is, causes may be sparesly populated with {@code null} values in certain
   *          positions.
   */
  public CombinedException(final List<? extends Throwable> causes)
  {
    this(null, causes);
  }

  @Override
  public String getMessage()
  {
    final StringBuilder sb = new StringBuilder();
    int index = 0;

    sb.append(super.getMessage());

    sb.append("\nCauses:");

    for (final Throwable cause : causes)
    {
      if (cause != null)
      {
        try (StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw))
        {
          cause.printStackTrace(pw);
          pw.flush();

          sb.append("\n\tIndex: ").append(index++).append("\n").append(sw.toString());
        }
        catch (final IOException e)
        {
          throw new RuntimeException("Error generating message for combined exception.", e);
        }
      }
    }

    return sb.toString();
  }
}
