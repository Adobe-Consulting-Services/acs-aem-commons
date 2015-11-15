package com.adobe.acs.commons.httpcache.keys;

import com.adobe.acs.commons.httpcache.exception.HttpCacheKeyCreationException;
import org.apache.sling.api.SlingHttpServletRequest;

/**
 * CacheKeyFactory is a OSGi Service interface that allows for consumers to generate their own CacheKey's based on their
 * out use-cases.
 *
 * This project will provide a GroupBased CacheKey factory.
 */
public interface CacheKeyFactory {
    CacheKey build(SlingHttpServletRequest request) throws HttpCacheKeyCreationException;
}
