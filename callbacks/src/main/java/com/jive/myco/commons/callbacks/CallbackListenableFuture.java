package com.jive.myco.commons.callbacks;

/**
 * Listenable future type callback...exists for previous users of the library
 *
 * @deprecated Just use {@link CallbackFuture} instead
 *
 * @param <T>
 *          the result type returned from the future / provided to the callback
 */
@Deprecated
public class CallbackListenableFuture<T> extends CallbackFuture<T>
{
}
