package com.jive.myco.commons.function;

/**
 *
 * @author David Valeri
 */
@FunctionalInterface
public interface ExceptionalRunnable
{
  void run() throws Exception;
}
