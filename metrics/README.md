# Metrics

This module provides interfaces and implementation classes used to interact with a metrics library, currently Coda Hale Metrics.  This API attempts to ensure uniform metric naming, provide easy lifecycle management of metrics as they are created and destroyed, and manage the boostrap of the underlying metrics library.

## Metrics Manager

The example code below demonstrates how to manage the lifecycle of the *MetricsManager* and to leverage *MetricsManagerContext*s to provide uniform naming of metrics within an application.


```java
// Create and configure the manager when your application starts up.
final DefaultMetricsManager metricsManager = new DefaultMetricsManager();
metricsManager.setId(id); // Optional
metricsManager.setDispatcher(dispatcher); // Optional
metricsManager.setMetricsManagerConfiguration(
    MetricsManagerConfiguration.builder().build());
    
CallbackFuture<Void> callback = new CallbackFuture<>();
manager.init(callback);
// Blocking style usage only for example.
callback.get(50, TimeUnit.MILLISECONDS);

// Create a context for some example component in the application.
// All metrics created under this context share a common name prefix.
final MetricsManagerContext exampleComponentMmc = metricsManager.segment(
    "example-application", "example-component");

// You can pass a MetricsManagerContext to a sub-component to derive another
// context and it will automatically inherit the parent contexts name prefix.
final MetricsManagerContext exampleSubComponentMmc = exampleComponentMmc.segment(
    "example-sub-component");

// Initialize new metrics to use
Meter exampleComponentRate = exampleComponentMmc.getMeter("rate");
Meter exampleSubComponentRate = exampleSubComponentMmc.getMeter("rate");

// Use them
exampleComponentRate.mark();
exampleSubComponentRate.mark();

// Optionally discard the metrics when they are no longer needed.  The
// metrics manager performs discard via an object reference to the
// metric rather than by metric name.  This approach is much easier
// to use as you don't have to reconstruct all the metric names
// again when you want to destroy the metric.
exampleComponentMmc.getMetricsManager().removeMetric(
    exampleComponentRate);

// Discards are performed against the manager, not a single context.
// You can use any context, or the manager directly to discard a metric.
exampleComponentMmc.getMetricsManager().removeMetric(
    exampleSubComponentRate);

// When you are shutting down your application, destroy the metrics manager.
// All internal resources are cleaned and all metrics are destroyed.
callback = new CallbackFuture<>();
manager.destroy(callback);
// Blocking style usage only for example.
callback.get(50, TimeUnit.MILLISECONDS);

```