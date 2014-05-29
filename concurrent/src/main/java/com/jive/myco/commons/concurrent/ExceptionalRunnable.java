package com.jive.myco.commons.concurrent;

/**
 *
 * @author David Valeri
 */
@FunctionalInterface
public interface ExceptionalRunnable
{
  void run() throws Exception;
}
