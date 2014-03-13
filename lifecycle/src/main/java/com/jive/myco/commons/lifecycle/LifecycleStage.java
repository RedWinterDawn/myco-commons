package com.jive.myco.commons.lifecycle;

/**
 * Enumeration of basic lifecycle phases.
 * 
 * @author David Valeri
 */
public enum LifecycleStage
{
  UNINITIALIZED(0),
  INITIALIZING(50),
  INITIALIZATION_FAILED(75),
  INITIALIZED(100),
  DESTROYING(150),
  DESTROYED(200);

  private final int stageValue;

  private LifecycleStage(final int stage)
  {
    this.stageValue = stage;
  }
  
  /**
   * Returns true if {@code this} is equivalent to or further along the lifecycle timeline than
   * {@code stage}.
   * 
   * @param stage
   *          the stage to compare to
   */
  public boolean hasAchieved(final LifecycleStage stage)
  {
    return stageValue >= stage.stageValue;
  }
}
