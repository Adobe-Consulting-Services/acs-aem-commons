package com.adobe.acs.commons.adobeio.core.config;

import org.apache.commons.lang.StringUtils;

@SuppressWarnings("WeakerAccess")
public class Entry {

    private final String method;
    private final String endpoint;

    public Entry(String method, String endpoint) {
        if (StringUtils.isNotBlank(method)) {
            this.method = method;
        } else {
            this.method = StringUtils.EMPTY;
        }
        if (StringUtils.isNotBlank(endpoint)) {
            this.endpoint = endpoint;
        } else {
            this.endpoint = StringUtils.EMPTY;
        }
    }

    public String getMethod() {
        return method;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
