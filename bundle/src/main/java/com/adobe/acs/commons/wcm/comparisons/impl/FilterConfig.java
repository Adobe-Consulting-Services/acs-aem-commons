package com.adobe.acs.commons.wcm.comparisons.impl;


import org.apache.commons.lang3.ArrayUtils;

import java.util.regex.Pattern;

class FilterConfig {

    private final String[] ignoreProperties;
    private final String[] ignoreResources;

    public FilterConfig(String[] ignoreProperties, String[] ignoreResources) {
        this.ignoreProperties = ArrayUtils.clone(ignoreProperties);
        this.ignoreResources = ArrayUtils.clone(ignoreResources);
    }

    public boolean filterProperty(String name) {
        for (String entry : ignoreProperties) {
            if (Pattern.matches(entry, name)) {
                return true;
            }
        }
        return false;
    }

    public boolean filterResource(String name) {
        for (String entry : ignoreResources) {
            if (Pattern.matches(entry, name)) {
                return true;
            }
        }
        return false;
    }
}
