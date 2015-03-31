package com.jive.myco.commons.metrics;

import java.net.InetSocketAddress;

import javax.net.SocketFactory;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Builder;

/**
 * Configuration for {@link MetricsManager}.
 *
 * @author David Valeri
 */
@Getter
public class MetricsManagerConfiguration
{
  /**
   * The flag indicating if SLF4J reports should be enabled. Defaults to false.
   */
  private final boolean slf4jReporterEnabled;

  /**
   * The period to output SLF4J reports, in milliseconds. Defaults to 1 minute.
   */
  private final long slf4jReporterPeriod;

  /**
   * The name of the logger used to output metrics for the SLF4J reports.
   */
  private final String slf4jReporterLoggerName;

  /**
   * The flag indicating if JMX reports should be enabled. Defaults to true.
   */
  private final boolean jmxReporterEnabled;

  /**
   * The domain housing metrics exposed via JMX. Defaults to "com.jive.metrics".
   */
  private final String jmxReporterDomain;

  /**
   * The flag indicating if Graphite reports should be enabled. Defaults to false.
   */
  private final boolean graphiteReporterEnabled;

  /**
   * The period to output Graphite reports, in milliseconds. Defaults to 5 seconds.
   */
  private final long graphiteReporterPeriod;

  /**
   * The prefix for metric names produced by the Graphite reporter. Defaults to "com.jive.metrics".
   */
  private final String graphiteReporterPrefix;

  /**
   * The address of the Graphite server used by the Graphite reporter. Required if
   * {@link #graphiteReporterEnabled} is {@code true}.
   */
  private final InetSocketAddress graphiteReporterAddress;

  /**
   * The size of the internal queue used to hold metric reports before they are transmitted.
   * Defaults to 2000.
   */
  private final int graphiteReporterQueueSize;

  /**
   * Flag indicating if counters with unchanged values are transmitted with each report or if they
   * are filtered out of the report until they change again. Defaults to {@code true}.
   */
  private final boolean graphiteReporterFilterUnchangedCounters;

  /**
   * Flag indicating if gauges with unchanged values are transmitted with each report or if they are
   * filtered out of the report until they change again. Defaults to {@code false}.
   */
  private final boolean graphiteReporterFilterUnchangedGauges;

  /**
   * The socket factory used to create connections within the Graphite reporter. Defaults to
   * {@link SocketFactory#getDefault()}.
   */
  private final SocketFactory graphiteReporterSocketFactory;

  /**
   * Flag indicating if the Graphite reporter should use Pickle encoding when reporting. Defaults to
   * {@code false}.
   */
  private final boolean graphiteReporterPickle;

  @Builder
  private MetricsManagerConfiguration(final boolean slf4jReporterEnabled,
      final long slf4jReporterPeriod,
      @NonNull final String slf4jReporterLoggerName,
      final boolean jmxReporterEnabled,
      @NonNull final String jmxReporterDomain,
      final boolean graphiteReporterEnabled,
      final long graphiteReporterPeriod,
      @NonNull final String graphiteReporterPrefix,
      final InetSocketAddress graphiteReporterAddress,
      final int graphiteReporterQueueSize,
      final boolean graphiteReporterFilterUnchangedCounters,
      final boolean graphiteReporterFilterUnchangedGauges,
      final SocketFactory graphiteReporterSocketFactory,
      final boolean graphiteReporterPickle)
  {
    if (graphiteReporterEnabled && graphiteReporterAddress == null)
    {
      throw new NullPointerException(
          "graphiteReporterAddress required when the Graphite reporter is enabled.");
    }

    this.slf4jReporterEnabled = slf4jReporterEnabled;
    this.slf4jReporterPeriod = slf4jReporterPeriod;
    this.slf4jReporterLoggerName = slf4jReporterLoggerName;
    this.jmxReporterEnabled = jmxReporterEnabled;
    this.jmxReporterDomain = jmxReporterDomain;
    this.graphiteReporterEnabled = graphiteReporterEnabled;
    this.graphiteReporterPeriod = graphiteReporterPeriod;
    this.graphiteReporterPrefix = graphiteReporterPrefix;
    this.graphiteReporterAddress = graphiteReporterAddress;
    this.graphiteReporterQueueSize = graphiteReporterQueueSize;
    this.graphiteReporterFilterUnchangedCounters = graphiteReporterFilterUnchangedCounters;
    this.graphiteReporterFilterUnchangedGauges = graphiteReporterFilterUnchangedGauges;
    this.graphiteReporterSocketFactory = graphiteReporterSocketFactory;
    this.graphiteReporterPickle = graphiteReporterPickle;
  }

  public static final class MetricsManagerConfigurationBuilder
  {
    /**
     * The flag indicating if SLF4J reports should be enabled. Defaults to false.
     */
    private boolean slf4jReporterEnabled = false;

    /**
     * The period to output SLF4J reports, in milliseconds. Defaults to 1 minute.
     */
    private long slf4jReporterPeriod = 60000;

    /**
     * The name of the logger used to output metrics for the SLF4J reports.
     */
    private String slf4jReporterLoggerName = "com.jive.metrics";

    /**
     * The flag indicating if JMX reports should be enabled. Defaults to true.
     */
    private boolean jmxReporterEnabled = true;

    /**
     * The domain housing metrics exposed via JMX. Defaults to "com.jive.metrics".
     */
    private String jmxReporterDomain = "com.jive.metrics";

    /**
     * The flag indicating if Graphite reports should be enabled. Defaults to false.
     */
    private boolean graphiteReporterEnabled = false;

    /**
     * The period to output Graphite reports, in milliseconds. Defaults to 5 seconds.
     */
    private long graphiteReporterPeriod = 5000;

    /**
     * The prefix for metric names produced by the Graphite reporter. Defaults to
     * "com.jive.metrics".
     */
    private String graphiteReporterPrefix = "com.jive.metrics";

    /**
     * The size of the internal queue used to hold metric reports before they are transmitted.
     * Defaults to 2000.
     */
    private int graphiteReporterQueueSize = 2000;

    /**
     * Flag indicating if counters with unchanged values are transmitted with each report or if they
     * are filtered out of the report until they change again. Defaults to {@code true}.
     */
    private boolean graphiteReporterFilterUnchangedCounters = true;

    /**
     * Flag indicating if gauges with unchanged values are transmitted with each report or if they
     * are filtered out of the report until they change again. Defaults to {@code false}.
     */
    private boolean graphiteReporterFilterUnchangedGauges = false;

    /**
     * The socket factory used to create connections within the Graphite reporter. Defaults to
     * {@link SocketFactory#getDefault()}.
     */
    private SocketFactory graphiteReporterSocketFactory = SocketFactory.getDefault();

    /**
     * Flag indicating if the Graphite reporter should use Pickle encoding when reporting. Defaults
     * to {@code false}.
     */
    private boolean graphiteReporterPickle;
  }
}
