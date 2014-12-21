package com.jive.myco.commons.callbacks;

import com.jive.myco.commons.concurrent.Pnky;
import com.jive.myco.commons.concurrent.PnkyPromise;

/**
 * Temporary utility class to make transitioning from callbacks to {@link PnkyPromise promises} more
 * accessible.
 *
 * @author Brandon Pedersen &lt;bpedersen@getjive.com&gt;
 */
@Deprecated
public final class PnkyCallback<V> extends Pnky<V> implements Callback<V>
{

  @Override
  public void onSuccess(final V result)
  {
    resolve(result);
  }

  @Override
  public void onFailure(final Throwable cause)
  {
    reject(cause);
  }
}
