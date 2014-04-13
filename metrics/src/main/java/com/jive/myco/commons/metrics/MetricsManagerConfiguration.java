package com.jive.myco.commons.metrics;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Builder;

/**
 * Configuration for {@link MetricsManager}.
 * 
 * @author David Valeri
 */
@Getter
@Builder
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
   * The flag indicating if JMX reports should be enabled. Defaults to true.
   */
  private final boolean jmxReporterEnabled;

  /**
   * The domain housing metrics exposed via JMX. Defaults to "com.jive.metrics".
   */
  @NonNull
  private final String jmxReporterDomain;

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
     * The flag indicating if JMX reports should be enabled. Defaults to true.
     */
    private boolean jmxReporterEnabled = true;

    /**
     * The domain housing metrics exposed via JMX. Defaults to "com.jive.metrics".
     */
    private String jmxReporterDomain = "com.jive.metrics";
  }
}
