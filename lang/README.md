# Lang

This module contains extra enhancements for the core of Java, much like Apache Commons Lang.

## LoggingUncaughtExceptionHandler

This `Thread.UncaughExceptionHandler` implementation provides defensive capabilities for handling
uncaught exceptions.  The implementation attempts to log the exception information to an `Slf4j`
logger.  If there is an error logging to the logger, the handler attempts to log the error
information and falls back to the default behavior of logging to `System.err`.

To configure the handler...

* Globally - Set the default handler to an instance of `LoggingUncaughtExceptionHandler`.
  
  ```java
  Thread.setDefaultUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler());
  ```
* Per Thread - Set the handler explicitly on an instance of `Thread`.
  
  ```java
  final Thread thread = new Thread(() -> xyz.pdq());
  thread.setUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler());
  ```
* On a per-Executor Basis - Configure the `ThreadPoolExecutor` instance with a custom 
`ThreadFactory` that sets the handler on each new `Thread` created.  Typically, you should use
Guava's `ThreadFactoryBuilder` to simplify ThreadFactory creation.  The example below illustrates
a concrete implementation rather than one built using the Guava builder.
  
  ```java
  private final Thread.UncaughtExceptionHandler handler  = new LoggingUncaughtExceptionHandler(); 
  
  (r) ->
  {
    final Thread t = new Thread(r, ...);
    t.setUncaughtExceptionHandler(handler);
    return t;
  }
  ```
        
