package com.jive.myco.commons.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.jive.myco.commons.function.ExceptionalBiConsumer;
import com.jive.myco.commons.function.ExceptionalBiFunction;
import com.jive.myco.commons.function.ExceptionalConsumer;
import com.jive.myco.commons.function.ExceptionalFunction;

//@formatter:off
/**
 * {@code PnkyPromise} is a custom implementation of a {@link ListenableFuture} that provides
 * methods to handle most all scenarios that would be needed in an asynchronous environment. While
 * much more verbose, and a different API, a {@code PnkyPromise} will work similarly to a JavaScript
 * <a href="http://people.mozilla.org/~jorendorff/es6-draft.html#sec-promise-objects">promises</a>.
 *
 * <p>
 *   While experimenting with Guava's {@link ListenableFuture} and Java 8's {@link CompletableFuture},
 *   we ran into shortcomings of either approach using the built in components. We identified 3 cross-
 *   cutting concerns that we wanted to be addressed through different APIs.
 *   <ol>
 *     <li>When to perform an action (always, on success only, or on failure only).</li>
 *     <li>Whether or not to propagate the successful result or error on completion or transform or
 *     recover the result</li>
 *     <li>Handle a transformed result that returns the result of another asynchronous action</li>
 *   </ol>
 * </p>
 *
 * <p>
 * This set of operations led us to the following results:
 *   <table>
 *     <tr>
 *       <th colspan="4">Listenable Future</th>
 *     </tr>
 *     <tr>
 *       <td></td>
 *       <td>Always invoke</td>
 *       <td>Invoke on success only</td>
 *       <td>Invoke on failure only</td>
 *     </tr>
 *     <tr>
 *       <td>Propagate result</td>
 *       <td>No</td>
 *       <td>No</td>
 *       <td>No</td>
 *     </tr>
 *     <tr>
 *       <td>Transform/recover result</td>
 *       <td>No</td>
 *       <td>Yes - transform</td>
 *       <td>Yes - withFallback</td>
 *     </tr>
 *     <tr>
 *       <td>Transform/recover with new future</td>
 *       <td>No</td>
 *       <td>No</td>
 *       <td>No</td>
 *     </tr>
 *   </table>
 *
 *   <table>
 *     <tr>
 *       <th colspan="4">Completable Future</th>
 *     </tr>
 *     <tr>
 *       <td></td>
 *       <td>Always invoke</td>
 *       <td>Invoke on success only</td>
 *       <td>Invoke on failure only</td>
 *     </tr>
 *     <tr>
 *       <td>Propagate result</td>
 *       <td>Yes - whenComplete</td>
 *       <td>Yes - thenAccept</td>
 *       <td>No</td>
 *     </tr>
 *     <tr>
 *       <td>Transform/recover result</td>
 *       <td>Yes - handle</td>
 *       <td>Yes - thenApply</td>
 *       <td>Yes - exceptionally</td>
 *     </tr>
 *     <tr>
 *       <td>Transform/recover with new future</td>
 *       <td>No</td>
 *       <td>Yes - thenCompose</td>
 *       <td>No</td>
 *     </tr>
 *   </table>
 * </p>
 *
 * <p>
 *   {@link ListenableFuture} is missing a lot of operations and also does not actually provide the
 *   ability to chain handling one after the other, you must go use the extension class
 *   {@link Futures}.
 * </p>
 *
 * <p>
 *   {@link CompletableFuture} was close but still did not provide all the operations we want to
 *   handle. There are also issues with the functional interfaces in Java 8 that do not allow you
 *   to throw checked exceptions from the method. While it is generally not the best idea to be
 *   throwing checked exceptions within an asynchronous chain, it is easy to acknowledge that these
 *   types of errors can happen and we should just be prepared to handle the exceptional result.
 * </p>
 *
 * <p>
 *   Since neither of these tools gave us all of what we wanted, we decided to come up with our own.
 *   While {@link ListenableFuture} is not nearly as close to what we want with regard to all these
 *   operations, it was the easiest and cleanest to get started with and add the exact functionality
 *   that we needed.
 * </p>
 *
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
//@formatter:on
public interface PnkyPromise<V> extends ListenableFuture<V>
{
  /**
   * Provide operations to perform with a successful or failed condition. Since these operations do
   * not return a result, the existing result will be propagated unless one of the operations throws
   * an exception in which case a failed promise will be propagated with that error.
   *
   * @param onSuccess
   *          operation to perform with a successful result
   * @param onFailure
   *          operation to perform when there is a failure
   * @return a new promise
   */
  PnkyPromise<V> alwaysAccept(ExceptionalConsumer<? super V> onSuccess,
      ExceptionalConsumer<Throwable> onFailure);

  /**
   * Provide operations to perform with a successful or failed condition, executing the operations
   * on the provided executor. Since these operations do not return a result, the existing result
   * will be propagated unless one of the operations throws an exception in which case a failed
   * promise will be propagated with that error.
   *
   * @param onSuccess
   *          operation to perform with a successful result
   * @param onFailure
   *          operation to perform when there is a failure
   * @param executor
   *          the executor to use for asynchronous processing of the operations
   * @return a new promise
   */
  PnkyPromise<V> alwaysAccept(ExceptionalConsumer<? super V> onSuccess,
      ExceptionalConsumer<Throwable> onFailure, Executor executor);

  /**
   * Provide functions to transform the given result on success or recover with a valid result on
   * failure. A failed promise will be propagated if either of the functions throw an exception.
   *
   * @param <O>
   *          type of transformed result
   * @param onSuccess
   *          function used to transform a successful result on completion
   * @param onFailure
   *          function used to potentially recover from a failure
   * @return a new promise
   */
  <O> PnkyPromise<O> alwaysTransform(ExceptionalFunction<? super V, O> onSuccess,
      ExceptionalFunction<Throwable, O> onFailure);

  /**
   * Provide functions to transform the given result on success or recover with a valid result on
   * failure, executing the function on the provided executor. A failed promise will be propagated
   * if either of the functions throw an exception.
   *
   * @param <O>
   *          type of transformed result
   * @param onSuccess
   *          function used to transform a successful result on completion
   * @param onFailure
   *          function used to potentially recover from a failure
   * @param executor
   *          the executor to use for asynchronous processing of the function
   * @return a new promise
   */
  <O> PnkyPromise<O> alwaysTransform(ExceptionalFunction<? super V, O> onSuccess,
      ExceptionalFunction<Throwable, O> onFailure, Executor executor);

  /**
   * Provide functions that will return a new promise on success or recover with a new promise on
   * failure. The returned promise will be completed only when the promise returned from the
   * function is complete. A failed promise will be propagated if either of the functions throw an
   * exception.
   *
   * @param <O>
   *          type of transformed result from the returned promise
   * @param onSuccess
   *          function to use to transform a successful result on completion
   * @param onFailure
   *          function to use to potentially recover from a failure
   * @return a new promise that is completed when the promise from the function completes
   */
  <O> PnkyPromise<O> alwaysCompose(ExceptionalFunction<? super V, PnkyPromise<O>> onSuccess,
      ExceptionalFunction<Throwable, PnkyPromise<O>> onFailure);

  /**
   * Provide functions that will return a new promise on success or recover with a new promise on
   * failure, executing the function on the provided executor. The returned promise will be
   * completed only when the promise returned from the function is complete. A failed promise will
   * be propagated if either of the functions throw an exception.
   *
   * @param <O>
   *          type of transformed result from the returned promise
   * @param onSuccess
   *          function to use to transform a successful result on completion
   * @param onFailure
   *          function to use to potentially recover from a failure
   * @param executor
   *          the executor to use for asynchronous processing of the function
   * @return a new promise that is completed when the promise from the function completes
   */
  <O> PnkyPromise<O> alwaysCompose(ExceptionalFunction<? super V, PnkyPromise<O>> onSuccess,
      ExceptionalFunction<Throwable, PnkyPromise<O>> onFailure, Executor executor);

  /**
   * Provide an operation to perform with a successful or failed condition. The function will
   * receive arguments for both values. If the promise has failed, the {@code Throwable} will be a
   * non-null value. Since this operation does not return a result, the existing result will be
   * propagated unless the operation throws an exception in which case a failed promise will be
   * propagated with that error.
   *
   * @param handler
   *          operation to perform with the successful result or error on completion
   * @return a new promise
   */
  PnkyPromise<V> alwaysAccept(ExceptionalBiConsumer<? super V, Throwable> handler);

  /**
   * Provide an operation to perform with a successful or failed condition, executing the operation
   * on the provided executor. The function will receive arguments for both values. If the promise
   * has failed, the {@code Throwable} will be a non-null value. Since this operation does not
   * return a result, the existing result will be propagated unless the operation throws an
   * exception in which case a failed promise will be propagated with that error.
   *
   * @param handler
   *          operation to perform with the successful result or error on completion
   * @param executor
   *          the executor to use for asynchronous processing of the operation
   * @return a new promise
   */
  PnkyPromise<V> alwaysAccept(ExceptionalBiConsumer<? super V, Throwable> handler, Executor executor);

  /**
   * Provide a function to transform the given result on success or recover with a valid result on
   * failure. The function will receive arguments for both values. If the promise has failed, the
   * {@code Throwable} will be a non-null value. A failed promise will be propagated if the function
   * throws an exception.
   *
   * @param <O>
   *          type of transformed result
   * @param handler
   *          function used to transform the successful or failed result on completion
   * @return a new promise
   */
  <O> PnkyPromise<O> alwaysTransform(ExceptionalBiFunction<? super V, Throwable, O> handler);

  /**
   * Provide a function to transform the given result on success or recover with a valid result on
   * failure, executing the function on the provided executor. The function will receive arguments
   * for both values. If the promise has failed, the {@code Throwable} will be a non-null value. A
   * failed promise will be propagated if the function throws an exception.
   *
   * @param <O>
   *          type of transformed result
   * @param handler
   *          function used to transform the successful or failed result on completion
   * @param executor
   *          the executor to use for asynchronous processing of the function
   * @return a new promise
   */
  <O> PnkyPromise<O> alwaysTransform(ExceptionalBiFunction<? super V, Throwable, O> handler,
      Executor executor);

  /**
   * Provide a function that will return a new promise on success or recover with a new promise on
   * failure. The function will receive arguments for both values. If the promise has failed, the
   * {@code Throwable} will be a non-null value. The returned promise will be completed only when
   * the promise returned from the function is complete. A failed promise will be propagated if the
   * function throws an exception.
   *
   * @param <O>
   *          type of transformed result from the returned promise
   * @param handler
   *          function to use to transform a successful or failed result on completion
   * @return a new promise that is completed when the promise from the function completes
   */
  <O> PnkyPromise<O> alwaysCompose(
      ExceptionalBiFunction<? super V, Throwable, PnkyPromise<O>> handler);

  /**
   * Provide a function that will return a new promise on success or recover with a new promise on
   * failure, executing the function on the provided executor. The function will receive arguments
   * for both values. If the promise has failed, the {@code Throwable} will be a non-null value. The
   * returned promise will be completed only when the promise returned from the function is
   * complete. A failed promise will be propagated if the function throws an exception.
   *
   * @param <O>
   *          type of transformed result from the returned promise
   * @param handler
   *          function to use to transform a successful or failed result on completion
   * @param executor
   *          the executor to use for asynchronous processing of the function
   * @return a new promise that is completed when the promise from the function completes
   */
  <O> PnkyPromise<O> alwaysCompose(
      ExceptionalBiFunction<? super V, Throwable, PnkyPromise<O>> handler,
      Executor executor);

  /**
   * Provide an operation to perform with a successful result. If this promise has failed, that
   * failure will be propagated and the given operation will not be invoked. Since this operation
   * does not return a result, the existing result will be propagated (either success or failure)
   * unless the operation throws an exception in which case a failed promise will be propagated with
   * that error.
   *
   * @param onSuccess
   *          operation to perform with the successful result
   * @return a new promise
   */
  PnkyPromise<V> thenAccept(ExceptionalConsumer<? super V> onSuccess);

  /**
   * Provide an operation to perform with a successful result, executing the opertation on the
   * provided executor. If this promise has failed, that failure will be propagated and the given
   * operation will not be invoked. Since this operation does not return a result, the existing
   * result will be propagated unless the operation throws an exception in which case a failed
   * promise will be propagated with that error.
   *
   * @param onSuccess
   *          operation to perform with the successful result
   * @param executor
   *          the executor to use for asynchronous processing of the operation
   * @return a new promise
   */
  PnkyPromise<V> thenAccept(ExceptionalConsumer<? super V> onSuccess, Executor executor);

  /**
   * Provide a function to transform the given result on success. If this promise has failed, that
   * failure will be propagated and the given function will not be invoked. A failed promise will be
   * propagated if the function throws an exception.
   *
   * @param <O>
   *          type of transformed result
   * @param onSuccess
   *          function used to transform the successful result on completion
   * @return a new promise
   */
  <O> PnkyPromise<O> thenTransform(ExceptionalFunction<? super V, O> onSuccess);

  /**
   * Provide a function to transform the given result on success, executing the function on the
   * provided executor. If this promise has failed, that failure will be propagated and the given
   * function will not be invoked. A failed promise will be propagated if the function throws an
   * exception.
   *
   * @param <O>
   *          type of transformed result
   * @param onSuccess
   *          function used to transform the successful result on completion
   * @param executor
   *          the executor to use for asynchronous processing of the function
   * @return a new promise
   */
  <O> PnkyPromise<O> thenTransform(ExceptionalFunction<? super V, O> onSuccess, Executor executor);

  /**
   * Provide a function that will return a new promise on success. If this promise has failed, that
   * failure will be propagated and the given function will not be invoked. The returned promise
   * will be completed only when the promise returned from the function is complete or if it is
   * propagating the failure. A failed promise will be propagated if the function throws an
   * exception.
   *
   * @param <O>
   *          type of transformed result from the returned promise
   * @param onSuccess
   *          function to use to transform the successful result on completion
   * @return a new promise that is completed when the promise from the function completes
   */
  <O> PnkyPromise<O> thenCompose(ExceptionalFunction<? super V, PnkyPromise<O>> onSuccess);

  /**
   * Provide a function that will return a new promise on success, executing the function on the
   * provided executor. If this promise has failed, that failure will be propagated and the given
   * function will not be invoked. The returned promise will be completed only when the promise
   * returned from the function is complete or if it is propagating the failure. A failed promise
   * will be propagated if the function throws an exception.
   *
   * @param <O>
   *          type of transformed result from the returned promise
   * @param onSuccess
   *          function to use to transform the successful result on completion
   * @param executor
   *          the executor to use for asynchronous processing of the function
   * @return a new promise that is completed when the promise from the function completes
   */
  <O> PnkyPromise<O> thenCompose(ExceptionalFunction<? super V, PnkyPromise<O>> onSuccess,
      Executor executor);

  /**
   * Provide an operation to perform on a failed result. If this promise has completed successfully,
   * that result will be propagated and the given operation will not be invoked. Since this
   * operation does not return a result, the existing result will be propagated (either success or
   * failure) unless the operation throws an exception in which case a failed promise will be
   * propagated with that error.
   *
   * @param onFailure
   *          operation to perform with the failed result
   * @return a new promise
   */
  PnkyPromise<V> onFailure(ExceptionalConsumer<Throwable> onFailure);

  /**
   * Provide an operation to perform on a failed result, executing the operation on the provided
   * executor. If this promise has completed successfully, that result will be propagated and the
   * given operation will not be invoked. Since this operation does not return a result, the
   * existing result will be propagated (either success or failure) unless the operation throws an
   * exception in which case a failed promise will be propagated with that error.
   *
   * @param onFailure
   *          operation to perform with the failed result
   * @param executor
   *          the executor to use for asynchronous processing of the operation
   * @return a new promise
   */
  PnkyPromise<V> onFailure(ExceptionalConsumer<Throwable> onFailure, Executor executor);

  /**
   * Provide a function to recover from a failure with a valid result. If this promise has completed
   * successfully, that result will be propagated and the given operation will not be invoked. A
   * failed promise will be propagated if the function throws an exception.
   *
   * @param onFailure
   *          function used to recover from a failed condition with a valid result
   * @return a new promise
   */
  PnkyPromise<V> withFallback(ExceptionalFunction<Throwable, V> onFailure);

  /**
   * Provide a function to recover from a failure with a valid result, executing the function on the
   * provided executor. If this promise has completed successfully, that result will be propagated
   * and the given operation will not be invoked. A failed promise will be propagated if the
   * function throws an exception.
   *
   * @param onFailure
   *          function used to recover from a failed condition with a valid result
   * @param executor
   *          the executor to use for asynchronous processing of the function
   * @return a new promise
   */
  PnkyPromise<V> withFallback(ExceptionalFunction<Throwable, V> onFailure,
      Executor executor);

  /**
   * Provide a function to recover from a failure with a new promise that will return a valid
   * result. If this promise has completed successfully, that result will be propagated and the
   * given operation will not be invoked. The returned promise will be completed only when the
   * promise returned from the function is complete or if it is propagating the successful result. A
   * failed promise will be propagated if the function throws an exception.
   *
   * @param onFailure
   *          function to use to recover from a failed condition with a new promise
   * @return a new promise that is completed when the promise from the function completes
   */
  PnkyPromise<V> composeFallback(ExceptionalFunction<Throwable, PnkyPromise<V>> onFailure);

  /**
   * Provide a function to recover from a failure with a new promise that will return a valid
   * result, executing the function on the provided executor. If this promise has completed
   * successfully, that result will be propagated and the given operation will not be invoked. The
   * returned promise will be completed only when the promise returned from the function is complete
   * or if it is propagating the successful result. A failed promise will be propagated if the
   * function throws an exception.
   *
   * @param onFailure
   *          function to use to recover from a failed condition with a new promise
   * @param executor
   *          the executor to use for asynchronous processing of the function
   * @return a new promise that is completed when the promise from the function completes
   */
  PnkyPromise<V> composeFallback(
      ExceptionalFunction<Throwable, PnkyPromise<V>> onFailure,
      Executor executor);
}
