package com.adobe.acs.commons.adobeio.core.types;

/**
 * This interface is used to specify the action in the operation
 */
public interface Action {

    void setValue(String actionType);

    String getValue();
}
