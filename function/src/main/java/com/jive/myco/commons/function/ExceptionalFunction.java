package com.jive.myco.commons.function;

/**
 *
 * @param <T>
 *          the type of the input to the function
 * @param <R>
 *          the type of the result of the function
 *
 * @author David Valeri
 */
@FunctionalInterface
public interface ExceptionalFunction<T, R>
{
  R apply(final T input) throws Exception;

  static final ExceptionalFunction<?, ?> IDENTITY = (x) -> x;

  @SuppressWarnings("unchecked")
  static <T> ExceptionalFunction<T, T> identity()
  {
    return (ExceptionalFunction<T, T>) IDENTITY;
  }
}
