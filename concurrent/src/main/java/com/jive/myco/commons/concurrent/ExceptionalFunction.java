package com.jive.myco.commons.concurrent;

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
}
