package com.adobe.acs.commons.synth.children;

import org.apache.sling.api.resource.Resource;

/**
 * Exception indicating the data representing the children is invalid.
 */
public final class InvalidDataFormatException extends Exception {
    public InvalidDataFormatException(final Resource resource, final String propertyName, final String data) {
        super("Property Value in invalid format [ " + resource.getPath() + "/" + propertyName + " = "
                + data + " ]" );
    }
}
