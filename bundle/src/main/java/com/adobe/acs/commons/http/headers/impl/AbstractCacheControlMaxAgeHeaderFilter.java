package com.adobe.acs.commons.http.headers.impl;

import javax.servlet.http.HttpServletRequest;

import org.osgi.framework.BundleContext;

public abstract class AbstractCacheControlMaxAgeHeaderFilter extends AbstractCacheHeaderFilter {

    protected static final String CACHE_CONTROL_NAME = "Cache-Control";
    protected static final String HEADER_PREFIX = "max-age=";
    protected final long maxAge;

    protected AbstractCacheControlMaxAgeHeaderFilter(boolean isSlingFilter, long maxAge, ServletRequestPredicates requestPredicates, int serviceRanking,
            BundleContext bundleContext) {
        super(isSlingFilter, requestPredicates, serviceRanking, bundleContext);
        if (maxAge <= 0) {
            throw new IllegalArgumentException("Max Age must be specified and greater than 0 but is " + maxAge);
        }
        this.maxAge = maxAge;
    }

    @Override
    protected String getHeaderName() {
        return CACHE_CONTROL_NAME;
    }

    @Override
    protected String getHeaderValue(HttpServletRequest request) {
        return HEADER_PREFIX + maxAge;
    }


}