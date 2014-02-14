Infrastructure Team Commons Project
===================================

Intro
----------------------------------

Stuff we need to use across different projects.

### Core
#### Retry Manager and Policy and Strategy
This classes are responsible for, you guessed it, retrying things. We are currently using this within jotter to handle reconnects and failed operation retries. What could this be used for you say? Anything that you want to beat to death with retries. Like the Persians and those pesky Spartans. I guess we'll start with the [RetryPolicy](core/src/main/java/com/jive/myco/core/retry/RetryPolicy.java). The retry policy is responsible for controlling how many retries and the delay period between retries. The [RetryManager](core/src/main/java/com/jive/myco/core/retry/RetryManager.java) requires one when it is created.

```java
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

The [RetryStrategy](core/src/main/java/com/jive/myco/core/retry/RetryStrategy.java) is an interface that must be implemented by you. It's purpose is to define what you want to retry and how to handle failures sooooo

``` java
    private class PersianRetryStrat implements RetryStrategy{

        @Override
        void scheduleRetry(final long delay)
        {
            //Wait for delay millis
            Thread.sleep(delay);
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
        }

        void onFailure(final int retryCount, final Throwable cause)
        {
            log.error("Retry failed. Prepare to face Xerxes");
        }

        void onRetriesExhausted(final List<Throwable> causes)
        {
            //At this point you call it quits and deal with it.
            log.error("We should have brought more arrows");
        }
    }
```

And finally we have the [RetryManager](core/src/main/java/com/jive/myco/core/retry/RetryManager.java). From the javadoc "A manager for tracking retry attempts, scheduling delayed retries, and notifying a strategy of ultimate failure after exhausting all retries. The manager maintains internal state and should not be shared across multiple threads without external synchronization."

```java
RetryManager retryManager = RetryManager.builder().retryPolicy(policy).retryStrategy(strategy).build();

// This resets the retryManager so that is can be reused.
retryManager.onSucess();

// This will trigger a retry. You can pass an exception if you want to give context about the failure.
retryManager.onFailure(new ExceptionThatYouChoose())


```
