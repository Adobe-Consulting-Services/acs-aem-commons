package com.adobe.acs.commons.forms.helpers;

import org.apache.sling.api.SlingHttpServletRequest;

/**
 * Used to register the PostFormHelper as an OSGi Service
 */
public interface PostFormHelper extends FormHelper {
    public String getSuffix();
    public boolean hasValidSuffix(SlingHttpServletRequest request);
}