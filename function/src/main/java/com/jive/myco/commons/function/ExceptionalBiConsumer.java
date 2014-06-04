package com.jive.myco.commons.function;

/**
 * Represents an operation that accepts two input arguments and returns no result.
 *
 * @param <T>
 *          the type of the first argument to the operation
 * @param <U>
 *          the type of the second argument to the operation
 *
 * @author David Valeri
 */
@FunctionalInterface
public interface ExceptionalBiConsumer<T, U>
{
  /**
   * Performs this operation on the given arguments.
   *
   * @param t
   *          the first input argument
   * @param u
   *          the second input argument
   */
  void accept(final T t, final U u);
}
