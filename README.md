# Myco Commons

Stuff that does things on different projects.

* [**BOM**](./bom/README.md)
* [**Callbacks - DEPRECATED**](./callbacks/README.md)
* [**Concurrent**](./concurrent/README.md)
* [**Function**](./function/README.md)
* [**HawtDispatch**](./hawtdispatch/README.md)
* [**IO**](./io)
* [**Lang**](./lang)
* [**Lifecycle**](./lifecycle)
* [**Lifecycle Guava**](./lifecycle-guava)
* [**Listenable**](./listenable)
* [**Metrics**](./metrics/README.md)
* [**Retry**](./retry/README.md)
* [**Testing**](./testing)
* [**Versions**](./versions)
* [**Change Log**](#changes)

## <a name="changes"></a>Change Log

### 0.2.6
* Upgrading from 0.2.5
  * No changes required
* Change Log
  * US7443 - Changed unchanged metric reporting filter to support counters and gauges
  * US7821 - Added additional methods to Pnky / PnkyPromise 

### 0.2.5
* Upgrading from 0.2.4
  * No changes required
* Change Log
  * NS - Added a ScheduledExecutorService implementation backed by HawtDispatch
  * US7443 - Changed unchanged metric reporting to report once a minute regardless of change

### 0.2.4
* Upgrading from 0.2.3
  * No changes required
* Change Log
  * NS - Deprecated callbacks module
  * US7425 - Added Lang module

### ???
* Some crazy stuff happened here...

### 0.1.2
* Upgrading from 0.1.1
  * No changes required
* Change Log
  * NS - Updated metrics logging to be less frequent during Graphite reporting issues

### 0.1.1
* Upgrading from 0.1.0
  * No changes required
* Change Log
  * US5129 - Added wrappers to convert `Lifecycled` instances to/from a Guava `Service`

### 0.1.0
* Upgrading from 0.0.10
  * `Lifecycled` no longer supports the methods taking a `Callback`.  Implementations must move to 
    support the PnkyPromise API.  Use of `AbstractLifecycled` is strongly encouraged. 
* Change Log
  * NS - Removed deprecated methods in `Lifecycled`

### 0.0.10
* Upgrading from 0.0.9
  * No changes required.
* Change Log
  * NS - Upgraded to Myco Parent 27
  * NS - Changed some Graphite logging levels to trace

### 0.0.9
* Upgrading from 0.0.8
  * Projects using the Graphite reporter in the Metrics Module must take into account that metric
    values are not reported for counters with unchanged values.  Graphite graphs may use a function
    to report the last known value for the counter.
* Change Log
  * NS - Upgraded to Myco Parent 26
  * NS - Fix exceptional functions to allow throwing exceptions
  * NS - Add ability to listen to lifecycle state changes
  * NS - Add custom singleton executor for executing on the current thread
  * US4842 - Changed to filter unchanged counters by default.

### 0.0.8
* Upgrading from 0.0.7
  * No changes required.
* Change Log
  * NS - Upgraded to Myco Parent 23
  * NS - Reformatted README.md files
  * US4536 - Added custom Graphite reporter client, ability to use Pickle encoding, long lived 
    Carbon sockets for the Graphite reporter, optional filtering of unchanged metrics in Graphite 
    reports, and batch transmissions of Graphite reports.
  * NS - Add ability to create a `PnkyPromise` from a `ListenableFuture`

### 0.0.7
* Upgrading from 0.0.6
  * `Pnky.all()` now uses a custom `CombinedException` class that will provide access to all 
    exceptions that occurred during the operation rather than rejecting with just the first 
    exception to occur.
* Change Log
  * NS - Modify behavior of `Pnky.all()` to wait for all promises to be complete before resolving 
    the future and use a custom exception to gather all errors that occurred.
  * NS - Add ability for `ListenableContainer` to perform an action as soon as the listener is added
  * NS - Add helper function on `ExceptionalFunction` for transforming to a typed `null` value

### 0.0.6
* Upgrading from 0.0.5
  * No changes required.
  * `MetricsManager.segment()` may now be invoked before the metrics manager has been initialized 
    in order to facilitate deriving contexts during construction.
* Change Log
  * NS - Updated `DefaultMetricsManager` to support segmenting before initialization

### 0.0.5
* Upgrading from 0.0.4
  * The deprecated `CallbackListenableFuture` class has been removed. Usagers of this class can use 
    the drop in replacement, `CallbackFuture`, or can transition away from callbacks to the 
    `PnkyPromise` future/promise API.
  * `Lifecycled.init(Callback)` and `Lifecycled.destroy(Callback)` have been deprecated in favor of 
    a new future/promises model. Users are encouraged to transition to using `AbstractLifecycled` 
    and implementing the `PnkyPromise init/destroyInternal` lifecycle methods as opposed to the 
    callback versions. They will be removed in a future release.
* Change Log
  * NS - Created new promises/futures async framework, including a module with exceptional function 
    types analogous to `java.util.function` classes and removed deprecated async classes.
  * NS - Updated the Default Metrics Manager to use `AbstractLifecycled`

### 0.0.4
* Upgrading from 0.0.3
  * Optionally replace individual dependency management entries with the new BOM POM.
  * Users of interfaces and classes from the *com.jive.myco.commons.listenable* package will observe 
    breaking API changes as this module was updated to align more closely with Java 8 features.
* Change Log
  * US3323 - Added BOM module
  * US3223 - Generic type changes to listenable API
  * US3223 - Move atomic integer additions to new commons module
  * US3223 - Added HawtDispatch module which includes a queue builder utility
  * US3223 - Added [AbstractLifecycled](./lifecycle/src/main/java/com/jive/myco/commons/lifecycle/AbstractLifecycled.java)
             utility class to enforce desired behavior for lifecycled services as well as making it
             much easier to create new lifecycled services.
