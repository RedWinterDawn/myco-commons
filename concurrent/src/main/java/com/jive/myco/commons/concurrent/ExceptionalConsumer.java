package com.jive.myco.commons.concurrent;

/**
 *
 * @param <T>
 *          the type of the input to the operation
 *
 * @author David Valeri
 */
@FunctionalInterface
public interface ExceptionalConsumer<T>
{
  void accept(final T input) throws Exception;
}
