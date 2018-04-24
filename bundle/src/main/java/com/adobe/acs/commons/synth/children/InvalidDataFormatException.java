package com.adobe.acs.commons.synth.children;

import org.apache.sling.api.resource.Resource;

import javax.jcr.RepositoryException;

/**
 * Exception indicating the data representing the children is invalid.
 */
@SuppressWarnings("squid:S2166")
public final class InvalidDataFormatException extends RepositoryException {
    public InvalidDataFormatException(final Resource resource, final String propertyName, final String data) {
        super("Property Value in invalid format [ " + resource.getPath() + "/" + propertyName + " = "
                + data + " ]");
    }
}
