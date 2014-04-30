package com.jive.myco.commons.testing;

import static lombok.AccessLevel.*;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.NoArgsConstructor;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.base.Preconditions;
import com.jive.myco.commons.callbacks.Callback;

/**
 * Testing utilities for {@link Callback Callbacks}.
 *
 * @author Brandon Pedersen <bpedersen@getjive.com>
 */
@NoArgsConstructor(access = PRIVATE)
public final class Callbacks
{
  public static final Object DO_NOTHING = new Object();

  /**
   * Interface for invoking the supplied callback with dynamically generated content or performing
   * custom logic before or after the callback invocation.
   *
   * @author David Valeri
   */
  public static interface CallbackInvoker
  {
    void invokeCallback(final Callback<?> callback);
  }

  /**
   * Generate mocked responses to methods that require a callback for sending a response back. Each
   * invocation will trigger the next response in the list of responses provided to be provided to
   * the callback's response.
   * <p>
   * If the response is of type {@link Throwable} then the callback's {@link Callback#onFailure}
   * method will be invoked with the error. If the response is {@link #DO_NOTHING} then the callback
   * will not be invoked. If the response is of type {@link CallbackInvoker}, the invoker will be
   * invoked with the callback. Any other response will be forwarded on to the callback's
   * {@link Callback#onSuccess} method.
   *
   * @param responses
   *          set of responses to send to the callback
   * @return an answer to use to mock the method's response
   */
  public static Answer<Void> getAnswer(final Object... responses)
  {
    Preconditions.checkArgument(responses != null && responses.length > 0);

    final AtomicInteger counter = new AtomicInteger();
    return new Answer<Void>()
    {
      @Override
      public Void answer(final InvocationOnMock invocation) throws Throwable
      {
        final int callbackArg = getCallbackIndex(invocation);

        final Object response = getResponse();

        @SuppressWarnings("unchecked")
        final Callback<Object> callback = (Callback<Object>) invocation.getArguments()[callbackArg];

        if (response == DO_NOTHING)
        {
          // don't do anything
        }
        else if (response instanceof Throwable)
        {
          callback.onFailure((Throwable) response);
        }
        else if (response instanceof CallbackInvoker)
        {
          ((CallbackInvoker) response).invokeCallback(callback);
        }
        else
        {
          callback.onSuccess(response);
        }

        return null;
      }

      private Object getResponse()
      {
        final int invocationNumber = counter.getAndIncrement();
        Object response;
        if (invocationNumber >= responses.length)
        {
          response = responses[responses.length - 1];
        }
        else
        {
          response = responses[invocationNumber];
        }
        return response;
      }

      private int getCallbackIndex(final InvocationOnMock invocation) throws IllegalStateException
      {
        final Class<?>[] parameterTypes = invocation.getMethod().getParameterTypes();
        int callbackArg = -1;
        // reverse iteration since callback is usually at the end
        for (int i = parameterTypes.length - 1; i >= 0; i--)
        {
          final Class<?> parameterType = parameterTypes[i];
          if (Callback.class.isAssignableFrom(parameterType))
          {
            callbackArg = i;
            break;
          }
        }
        if (callbackArg == -1)
        {
          throw new IllegalStateException("No callback argument found!");
        }
        return callbackArg;
      }
    };
  }
}
