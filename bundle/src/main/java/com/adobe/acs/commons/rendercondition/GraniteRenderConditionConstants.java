package com.adobe.acs.commons.rendercondition;

public class GraniteRenderConditionConstants {

  private GraniteRenderConditionConstants() {
    throw new IllegalStateException("Tried to instantiate a constants class");
  }

  /**
   * An OSGI service property name used to find a particular GraniteRenderCondition service.
   */
  public static final String CONDITION_NAME = "condition.name";
}
