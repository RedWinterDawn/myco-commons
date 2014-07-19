package com.jive.myco.commons.function;

/**
 * Represents a function that accepts two arguments and produces a result.
 *
 * @param <T>
 *          the type of the first argument to the function
 * @param <U>
 *          the type of the second argument to the function
 * @param <R>
 *          the type of the result of the function
 *
 * @author David Valeri
 */
@FunctionalInterface
public interface ExceptionalBiFunction<T, U, R>
{
  /**
   * Applies this function to the given arguments.
   *
   * @param t
   *          the first function argument
   * @param u
   *          the second function argument
   *
   * @return the function result
   */
  R apply(final T t, final U u) throws Exception;
}
