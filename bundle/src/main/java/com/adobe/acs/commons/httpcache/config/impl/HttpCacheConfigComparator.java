package com.adobe.acs.commons.httpcache.config.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;

import java.util.Comparator;

/**
 * Used to sort HttpCacheConfig @Components by their order (ascending)
 */
public class HttpCacheConfigComparator implements Comparator<HttpCacheConfig> {

    @Override
    public int compare(final HttpCacheConfig cacheConfig1, final HttpCacheConfig cacheConfig2) {

        Integer order1 = cacheConfig1.getOrder();
        Integer order2 = cacheConfig2.getOrder();

        return order1.compareTo(order2);
    }
}