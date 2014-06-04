package com.jive.myco.commons.concurrent;

import java.util.concurrent.Executor;

import com.google.common.util.concurrent.ListenableFuture;
import com.jive.myco.commons.function.ExceptionalBiConsumer;
import com.jive.myco.commons.function.ExceptionalBiFunction;
import com.jive.myco.commons.function.ExceptionalConsumer;
import com.jive.myco.commons.function.ExceptionalFunction;

/**
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
public interface PnkyPromise<V> extends ListenableFuture<V>
{
  // always -- propagate
  PnkyPromise<V> thenHandle(ExceptionalConsumer<V> onSuccess,
      ExceptionalConsumer<Throwable> onFailure);

  PnkyPromise<V> thenHandle(ExceptionalConsumer<V> onSuccess,
      ExceptionalConsumer<Throwable> onFailure, Executor executor);

  // always -- transform
  <O> PnkyPromise<O> thenTransform(ExceptionalFunction<V, O> onSuccess,
      ExceptionalFunction<Throwable, O> onFailure);

  <O> PnkyPromise<O> thenTransform(ExceptionalFunction<V, O> onSuccess,
      ExceptionalFunction<Throwable, O> onFailure, Executor executor);

  // always -- compose
  <O> PnkyPromise<O> thenCompose(ExceptionalFunction<V, PnkyPromise<O>> onSuccess,
      ExceptionalFunction<Throwable, PnkyPromise<O>> onFailure);

  <O> PnkyPromise<O> thenCompose(ExceptionalFunction<V, PnkyPromise<O>> onSuccess,
      ExceptionalFunction<Throwable, PnkyPromise<O>> onFailure, Executor executor);

  // always -- propagate
  PnkyPromise<V> thenHandle(ExceptionalBiConsumer<V, Throwable> handler);

  PnkyPromise<V> thenHandle(ExceptionalBiConsumer<V, Throwable> handler, Executor executor);

  // always -- transform
  <O> PnkyPromise<O> thenTransform(ExceptionalBiFunction<V, Throwable, O> handler);

  <O> PnkyPromise<O> thenTransform(ExceptionalBiFunction<V, Throwable, O> handler,
      Executor executor);

  // always -- compose
  <O> PnkyPromise<O> thenCompose(ExceptionalBiFunction<V, Throwable, PnkyPromise<O>> handler);

  <O> PnkyPromise<O> thenCompose(ExceptionalBiFunction<V, Throwable, PnkyPromise<O>> handler,
      Executor executor);

  // onSuccess -- propagate
  PnkyPromise<V> thenHandle(ExceptionalConsumer<V> onSuccess);

  PnkyPromise<V> thenHandle(ExceptionalConsumer<V> onSuccess, Executor executor);

  // onSuccess -- transform
  <O> PnkyPromise<O> thenTransform(ExceptionalFunction<V, O> onSuccess);

  <O> PnkyPromise<O> thenTransform(ExceptionalFunction<V, O> onSuccess, Executor executor);

  // onSuccess -- compose
  <O> PnkyPromise<O> thenCompose(ExceptionalFunction<V, PnkyPromise<O>> onSuccess);

  <O> PnkyPromise<O> thenCompose(ExceptionalFunction<V, PnkyPromise<O>> onSuccess,
      Executor executor);

  // onFailure -- propagate
  PnkyPromise<V> onFailure(ExceptionalConsumer<Throwable> onFailure);

  PnkyPromise<V> onFailure(ExceptionalConsumer<Throwable> onFailure, Executor executor);

  // onFailure -- transform
  PnkyPromise<V> withFallback(ExceptionalFunction<Throwable, V> onFailure);

  PnkyPromise<V> withFallback(ExceptionalFunction<Throwable, V> onFailure, Executor executor);

  // onFailure -- compose
  PnkyPromise<V> composeFallback(ExceptionalFunction<Throwable, PnkyPromise<V>> onFailure);

  PnkyPromise<V> composeFallback(ExceptionalFunction<Throwable, PnkyPromise<V>> onFailure,
      Executor executor);
}
