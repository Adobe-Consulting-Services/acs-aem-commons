package com.adobe.acs.commons.forms.helpers;

/**
 * Used to register the PostFormHelper as an OSGi Service
 */
public interface PostFormHelper extends FormHelper {
    public String getSuffix();
}