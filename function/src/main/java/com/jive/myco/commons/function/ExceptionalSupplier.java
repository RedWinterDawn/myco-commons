package com.jive.myco.commons.function;

/**
 *
 * @param <T>
 *          the type of results supplied by this supplier
 *
 * @author David Valeri
 */
@FunctionalInterface
public interface ExceptionalSupplier<T>
{
  T get() throws Exception;
}
