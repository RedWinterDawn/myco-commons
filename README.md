# Myco Commons

Stuff that does things on different projects.

* [**BOM**](./bom/README.md)
* [**Callbacks**](./callbacks/README.md)
* [**Concurrent**](./concurrent/README.md)
* [**Function**](./function/README.md)
* [**HawtDispatch**](./hawtdispatch/README.md)
* [**IO**](./io)
* [**Lifecycle**](./lifecycle)
* [**Listenable**](./listenable)
* [**Metrics**](./metrics/README.md)
* [**Retry**](./retry/README.md)
* [**Testing**](./testing)
* [**Versions**](./versions)
* [**Change Log**](#changes)

## <a name="changes"></a>Change Log

##### 0.0.5
* Upgrading from 0.0.4
  * The deprecated `CallbackListenableFuture` class has been removed. You can do a quick change and just replace `CallbackListenableFuture` with `CallbackFuture`, or you can transition away from callbacks and to the `PnkyPromise` method of asynchronous tasks.
  * `Lifecycled.init(Callback)` and `Lifecycled.destroy(Callback)` have been deprecated in favor of a new future/promises model. You are encouraged to transition to using `AbstractLifecycled` and implementing the `PnkyPromise init/destroyInternal` lifecycle methods as opposed to the callback versions. They will be removed as soon as the next release.
* Change Log
  * NS - Create new promises/futures async framework, including a module with exceptional function types analogous to `java.util.function` classes.
  * NS - Updated the Default Metrics Manager to use `AbstractLifecycled`

#### 0.0.4
* Upgrading from 0.0.3
  * Optionally replace individual dependency management entries with the new BOM POM.
  * Users of interfaces and classes from the *com.jive.myco.commons.listenable* package will observe breaking API changes as this module was updated to align more closely with Java 8 features.
* Change Log
  * US3323 - Added BOM module
  * US3223 - Generic type changes to listenable API
  * US3223 - Move atomic integer additions to new commons module
  * US3223 - Added HawtDispatch module which includes a queue builder utility
  * US3223 - Added [AbstractLifecycled](./lifecycle/src/main/java/com/jive/myco/commons/lifecycle/AbstractLifecycled.java)
             utility class to enforce desired behavior for lifecycled services as well as making it
             much easier to create new lifecycled services.
