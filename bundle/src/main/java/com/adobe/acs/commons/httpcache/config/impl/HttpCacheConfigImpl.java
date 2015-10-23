package com.adobe.acs.commons.httpcache.config.impl;

import com.adobe.acs.commons.httpcache.config.HttpCacheConfig;
import org.apache.felix.scr.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Config class for http cache. Modelled as OSGi config factory. Every factory instance is tied to a request URI.
 * Parameters such as selection of cache store, user groups, invalidation details shall also be configured.
 */
@Component(label = "ACS AEM Commons - HTTP Cache - Cache config",
           description = "Config for request URI pattern that has to be cached. Each config is tied to a single " +
                   "request URI pattern. Allows multiple configurations.",
           configurationFactory = true,
           metatype = true
)
@Service
public class HttpCacheConfigImpl implements HttpCacheConfig {
    private static final Logger log = LoggerFactory.getLogger(HttpCacheConfigImpl.class);

    // TODO - Supply an example for this config.
    @Property(label = "Request URI pattern",
              description = "Request URI pattern (REGEX) to be cached. Example - ")
    private static final String PROP_REQUEST_URI_PATTERN = "httpcache.config.requesturi.pattern";
    private String requestUriPattern;

    @Property(label = "Response MIME type",
              description = "Example - application/json")
    private static final String PROP_RESPONSE_MIME_TYPE = "httpcache.config.response.mime";
    private String responseMimeType;

    @Property(label = "AEM user groups",
              description = "AEM user groups for which this config is applicable.",
              cardinality = Integer.MAX_VALUE)
    private static final String PROP_USER_GROUPS = "httpcache.config.users.group";
    private String[] userGroups;

    @Property(label = "Cache store",
              description = "Cache store which shall be used for caching the response for this request URI. Example -" +
                      " MEM",
              value = DEFAULT_CACHE_STORE)
    private static final String PROP_CACHE_STORE = "httpcache.config.cachestore";
    private static final String DEFAULT_CACHE_STORE = "MEM"; // Defaults to memory cache store
    private String cacheStore;

    @Property(label = "JCR path pattern (REGEX) for cache invalidation ",
              description = "Path in JCR (Oak) repository for which this cache has to be invalidated. This accepts " +
                      "REGEX.")
    private static final String PROP_CACHE_INVALIDATION_PATH = "httpcache.config.invalidation.oakpath";
    private String cacheInvalidationPath;

    @Activate
    @Modified
    protected void activate(Map<String, Object> configs) {
        log.info("HttpCacheConfigImpl activated /modified.");
        // TODO Read configs and populate the fields.
    }

    @Deactivate
    protected void deactivate(Map<String, Object> configs) {
        log.info("HttpCacheConfigImpl deactivated.");
    }

    @Override
    public String getRequestUri() {
        return requestUriPattern;
    }
}
