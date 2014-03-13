Myco Commons Retry
===================================

## Retry Manager, Policy, and Strategy
These classes are responsible for, you guessed it, retrying things. The retry manager can be used to sechedule delayed retries and handle failures of a process.  We'll start with the [RetryPolicy](core/src/main/java/com/jive/myco/core/retry/RetryPolicy.java). The retry policy is responsible for controlling how retreies are attempted and the delay period between retries. The [RetryManager](core/src/main/java/com/jive/myco/core/retry/RetryManager.java) requires one when it is created.

``` java
RetryPolicy.builder()
    // Maximum amount of retries before quitting. A value less than 0 will retry indefinitely
    .maximumRetries(5)
    // Amount of time in milliseconds between retries. The value must be greater than 0.
    .initialRetryDelay(5)
    // Handles the increasing amount of time between retries. Will multiply the last retry delay
    // everytime. In this case it will go 5, 10, 20, ... By default this is not used.
    .backoffMultiplier(2)
    // Maximum amount of time in milliseconds between retries. This puts a ceiling on the backoff
    // multiplier value to 32 seconds.
    .maximumRetryDelay(32000)
    // Builds the thing, like what were you expecting?
    .build()
```

The [RetryStrategy](core/src/main/java/com/jive/myco/core/retry/RetryStrategy.java) is an interface that must be implemented by the user. Its purpose is to define what you want to retry and how to handle failures.  Typically the retry will schedule a retry task on an executor.  It is important to note that the *scheduleRetry*, *onFailure*, and *onRetriesExhausted* methods of the user defined strategy are invoked by the same thread that invokes *onFailure* on the manager.  For this reason, calling *Thread.sleep()* or *RetryManager.onFailure(...)* from the same thread that executes your stretegy can block the thread and/or lead to infinite recursion.

``` java
    private class PersianRetryStrat implements RetryStrategy{

        @Override
        void scheduleRetry(final long delay)
        {
            //Wait for delay millis
            executor.schedule(
                    new Runnable()
                    {
                        try
                        {
                            spartan.killEmAll();
                            // VICTORY!!!!!!
                            retryManager.onSuccess();
                        }
                        catch(LeonidasTheFirstException e)
                        {
                            //We'll talk about this later
                            retryManager.onFailure(e);
                        }
                    },
                    delay, TimeUnit.MILLISECONDS);
            }
        }

        void onFailure(final int retryCount, final Throwable cause)
        {
            log.warn("Retry failed. Prepare to face Xerces.", cause);
        }

        void onRetriesExhausted(final List<Throwable> causes)
        {
            //At this point you call it quits and deal with it.
            log.error("We should have brought more arrows.", ...);
        }
    }
```

And finally we have the [RetryManager](core/src/main/java/com/jive/myco/core/retry/RetryManager.java). The manager is used to track retry attempts, schedule delayed retries, and notify a strategy of ultimate failure after exhausting all retries. The manager maintains internal state and should not be shared across multiple threads without external synchronization.

```java
RetryManager retryManager = RetryManager.builder().retryPolicy(policy).retryStrategy(strategy).build();

// This resets the retryManager so that is can be reused.
retryManager.onSucess();

// This will trigger a retry. You can pass an exception if you want to give context about the failure.
retryManager.onFailure(new ExceptionThatYouChoose())


```
